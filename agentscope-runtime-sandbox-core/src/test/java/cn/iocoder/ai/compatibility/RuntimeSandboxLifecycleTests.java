/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.remote.RemoteHttpClient;
import io.agentscope.runtime.sandbox.manager.remote.RequestMethod;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies remote sandbox lifecycle and request ownership against actual local HTTP peers.
 * @author 芋道源码
 */
@Timeout(12)
class RuntimeSandboxLifecycleTests {
    private HttpServer server;
    private ExecutorService executor;
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor =
                Executors.newCachedThreadPool(
                        task -> {
                            Thread thread = new Thread(task, "sandbox-test-http");
                            thread.setDaemon(true);
                            return thread;
                        });
        server.setExecutor(executor);
        server.createContext(
                "/blocked",
                exchange -> {
                    requests.incrementAndGet();
                    entered.countDown();
                    try {
                        release.await(8, TimeUnit.SECONDS);
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.createContext(
                "/healthy",
                exchange -> {
                    requests.incrementAndGet();
                    byte[] bytes = "{\"data\":\"healthy\"}".getBytes(StandardCharsets.UTF_8);
                    try {
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.createContext(
                "/sandbox/getInfo",
                exchange -> {
                    requests.incrementAndGet();
                    assertEquals(
                            "Bearer fixture-token",
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] bytes =
                            "{\"data\":{\"containerId\":\"fixture-id\",\"version\":\"local-fixture\"}}"
                                    .getBytes(StandardCharsets.UTF_8);
                    try {
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
    }

    @AfterEach
    void stop() {
        release.countDown();
        server.stop(0);
        executor.shutdownNow();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void remoteModeNeverStartsLocalContainerClient() {
        BaseClientStarter forbidden =
                new BaseClientStarter(ContainerClientType.DOCKER) {
                    @Override
                    public BaseClient startClient(PortManager ports) {
                        throw new AssertionError("Remote mode started Docker");
                    }
                };
        SandboxService service =
                new SandboxService(
                        ManagerConfig.builder()
                                .baseUrl(baseUrl())
                                .bearerToken("fixture-token")
                                .clientStarter(forbidden)
                                .remoteRequestTimeout(Duration.ofSeconds(2))
                                .build());
        try {
            service.start();
            service.start();
            assertEquals("fixture-id", service.getInfo("fixture-id").getContainerId());
            assertEquals(1, requests.get());
        } finally {
            service.close();
            service.close();
        }
        assertThrows(IllegalStateException.class, service::start);
        assertNull(service.getInfo("after-close"));
        assertEquals(1, requests.get());
    }

    @Test
    void requestDeadlineBoundsAStalledServer() throws Exception {
        try (RemoteHttpClient client =
                new RemoteHttpClient(
                        baseUrl(), null, Duration.ofSeconds(1), Duration.ofMillis(150))) {
            long started = System.nanoTime();
            Object result = client.makeRequest(RequestMethod.GET, "/blocked", null, null);
            assertTrue(result instanceof Map);
            assertTrue(((Map<?, ?>) result).containsKey("error"));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(2));
        }
    }

    @Test
    void closingBorrowedClientCancelsOwnRequestAndPreservesSharedClient() throws Exception {
        HttpClient shared = HttpClient.newHttpClient();
        RemoteHttpClient client =
                new RemoteHttpClient(shared, baseUrl(), null, Duration.ofSeconds(8));
        CompletableFuture<Object> pending =
                CompletableFuture.supplyAsync(
                        () -> client.makeRequest(RequestMethod.GET, "/blocked", null, null));
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            client.close();
            assertTrue(pending.get(2, TimeUnit.SECONDS) instanceof Map);
            assertEquals(
                    200,
                    shared.sendAsync(
                                    HttpRequest.newBuilder(URI.create(baseUrl() + "/healthy"))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString())
                            .get(2, TimeUnit.SECONDS)
                            .statusCode());
        } finally {
            pending.cancel(true);
            client.close();
            if (shared instanceof AutoCloseable) ((AutoCloseable) shared).close();
        }
    }

    @Test
    void closedOwnedClientRejectsNewWorkWithoutSendingARequest() {
        RemoteHttpClient client = new RemoteHttpClient(baseUrl(), null);
        client.close();
        client.close();
        Object result =
                client.makeRequest(RequestMethod.GET, "/healthy", Collections.emptyMap(), null);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).containsKey("error"));
        assertEquals(0, requests.get());
    }

    @Test
    void interruptionRestoresFlagAndReleasesTheWaitingCall() throws Exception {
        AtomicBoolean restored = new AtomicBoolean();
        try (RemoteHttpClient client = new RemoteHttpClient(baseUrl(), null)) {
            Thread caller =
                    new Thread(
                            () -> {
                                client.makeRequest(RequestMethod.GET, "/blocked", null, null);
                                restored.set(Thread.currentThread().isInterrupted());
                            },
                            "sandbox-interruption-probe");
            caller.start();
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                caller.interrupt();
                caller.join(2000);
                assertFalse(caller.isAlive());
                assertTrue(restored.get());
            } finally {
                caller.interrupt();
                release.countDown();
                caller.join(2000);
            }
        }
    }
}
