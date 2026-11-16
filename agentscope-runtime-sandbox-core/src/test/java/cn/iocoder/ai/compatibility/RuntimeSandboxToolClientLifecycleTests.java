/*
 * Copyright 2025-2026 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.agentscope.runtime.sandbox.manager.client.sandbox.SandboxHttpClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.TrainingSandboxClient;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.remote.RemoteHttpClient;
import io.agentscope.runtime.sandbox.manager.remote.RequestMethod;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies full-body deadlines and owned tool-client termination on local HTTP peers.
 *
 * @author 芋道源码
 */
@Timeout(8)
class RuntimeSandboxToolClientLifecycleTests {
    private HttpServer server;
    private ExecutorService peer;
    private final CountDownLatch headers = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer = Executors.newCachedThreadPool();
        server.setExecutor(peer);
        server.createContext("/healthz", exchange -> reply(exchange, "{\"healthy\":true}"));
        server.createContext(
                "/tools/run_shell_command",
                exchange -> {
                    assertEquals("POST", exchange.getRequestMethod());
                    assertEquals(
                            "Bearer fixture-token",
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    reply(
                            exchange,
                            "{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"tool-中文\"}]}");
                });
        server.createContext(
                "/partial",
                exchange -> {
                    try {
                        requests.incrementAndGet();
                        exchange.sendResponseHeaders(200, 64);
                        exchange.getResponseBody()
                                .write("{\"data\":\"".getBytes(StandardCharsets.UTF_8));
                        exchange.getResponseBody().flush();
                        headers.countDown();
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
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
        peer.shutdownNow();
    }

    private void reply(HttpExchange exchange, String body) throws IOException {
        requests.incrementAndGet();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void responseHeadersDoNotDisableTheBodyDeadline() throws Exception {
        try (RemoteHttpClient client =
                new RemoteHttpClient(
                        baseUrl(), null, Duration.ofSeconds(1), Duration.ofMillis(200))) {
            // Establish a healthy connection before measuring the partial-body case.
            // A cold connect timeout does not satisfy this test's headers-arrived precondition.
            boolean healthy = false;
            for (int attempt = 0; attempt < 3 && !healthy; attempt++) {
                Object warmup = client.makeRequest(RequestMethod.GET, "/healthz", null, null);
                healthy =
                        warmup instanceof Map
                                && Boolean.TRUE.equals(((Map<?, ?>) warmup).get("healthy"));
            }
            assertTrue(
                    healthy, "Local connection must be established before testing a stalled body");
            long started = System.nanoTime();
            Object result = client.makeRequest(RequestMethod.GET, "/partial", null, null);
            assertTrue(headers.await(1, TimeUnit.SECONDS));
            assertTrue(result instanceof Map);
            assertTrue(((Map<?, ?>) result).containsKey("error"));
            assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(2));
        }
    }

    @Test
    void sandboxToolClientClosesAndCannotSendMoreRequests() throws Exception {
        ContainerModel model =
                ContainerModel.builder()
                        .baseUrl(baseUrl())
                        .authToken("fixture-token")
                        .sessionId("fixture")
                        .build();
        SandboxHttpClient client = new SandboxHttpClient(model, 2);
        try {
            assertTrue(
                    client.callTool(
                                    "run_shell_command",
                                    Collections.singletonMap("command", "printf local"))
                            .contains("tool-中文"));
            client.close();
            client.close();
            int before = requests.get();
            assertFalse(client.checkHealth());
            assertTrue(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(client.callTool("run_shell_command", Collections.emptyMap()))
                            .path("isError")
                            .asBoolean());
            assertEquals(before, requests.get());
        } finally {
            client.close();
        }
    }

    @Test
    void trainingClientClosesAndCannotSendMoreRequests() throws Exception {
        TrainingSandboxClient client = new TrainingSandboxClient(baseUrl() + "/fixture", 2);
        try {
            assertTrue(client.checkHealth());
            client.close();
            client.close();
            int before = requests.get();
            assertFalse(client.checkHealth());
            assertEquals(before, requests.get());
        } finally {
            client.close();
        }
    }
}
