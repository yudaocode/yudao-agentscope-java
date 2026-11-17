/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.agentscope.durable.DurableJson;
import io.agentscope.durable.DurableRuntime;
import io.agentscope.durable.DurableTypes.SessionKey;
import io.agentscope.durable.DurableTypes.SessionSpec;
import io.agentscope.durable.DurableTypes.ThreadBinding;
import io.agentscope.durable.SessionBackend;
import io.agentscope.durable.store.ObjectStore.Reference;
import io.agentscope.extensions.durable.http.HttpSessionBackend;
import io.agentscope.extensions.durable.http.SessionBackendHandler;
import io.agentscope.extensions.durable.jdbc.LocalBackend;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Exercises installed Durable artifacts under the native Boot2 parent on Java8.
 *
 * @author 芋道源码
 */
@Timeout(30)
class DurableBoot2Test {
    @TempDir Path directory;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(App.class)
                .withPropertyValues("durable.test.dir=" + directory.toString());
    }

    @Test
    void localSessionsAndImmutableObjectsSurviveBoot2ContextRestart() throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        assertEquals("2.21.4", com.fasterxml.jackson.core.json.PackageVersion.VERSION.toString());
        List<URL> origins = Collections.list(DurableRuntime.class.getClassLoader()
                .getResources("io/agentscope/durable/DurableRuntime.class"));
        assertEquals(1, origins.size(), origins.toString());
        assertTrue(origins.get(0).toString().contains("agentscope-durable-runtime"));
        SessionKey key = new SessionKey("tenant-native8", "session");
        byte[] content = "预算你好".getBytes(StandardCharsets.UTF_8);
        AtomicReference<Reference> reference = new AtomicReference<>();
        runner().run(context -> {
            SessionBackend backend = context.getBean(SessionBackend.class);
            backend.create(new SessionSpec(key, new ThreadBinding("fixture", "v1", "1"),
                    DurableJson.value(Collections.singletonMap("message", "预算你好"))));
            reference.set(backend.localStorage().get().objects().put(DurableJson.hash(key), content));
            assertTrue(backend.capabilities().durable());
        });
        runner().run(context -> {
            SessionBackend backend = context.getBean(SessionBackend.class);
            assertEquals(key, backend.session(key).key());
            assertEquals("预算你好", backend.session(key).metadata().path("message").asText());
            assertArrayEquals(content, backend.localStorage().get().objects().read(reference.get()));
        });
    }

    @Test
    void installedHttpBridgeCallsTheBoot2LocalBackend() {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        runner().run(context -> {
            SessionBackend backend = context.getBean(SessionBackend.class);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService workers = Executors.newCachedThreadPool();
            server.setExecutor(workers);
            server.createContext("/backend", new SessionBackendHandler(backend,
                    (exchange, operation, session) -> {
                        if (session != null && !"tenant-native8".equals(session.namespace())) {
                            throw new IllegalArgumentException("Fixture namespace required");
                        }
                    }));
            server.start();
            try (SessionBackend remote = new HttpSessionBackend(URI.create(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/backend"))) {
                SessionKey key = new SessionKey("tenant-native8", "http-session");
                remote.create(new SessionSpec(key, new ThreadBinding("fixture", "v1", "1"),
                        DurableJson.value(Collections.singletonMap("message", "HTTP你好"))));
                assertEquals(key, remote.session(key).key());
                assertEquals("HTTP你好", backend.session(key).metadata().path("message").asText());
            } finally {
                server.stop(0);
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            }
        });
    }

    /** @author 芋道源码 */
    @Configuration(proxyBeanMethods = false)
    static class App {
        @Bean(destroyMethod = "close")
        SessionBackend backend(Environment environment) {
            return LocalBackend.open(Paths.get(environment.getRequiredProperty("durable.test.dir")));
        }
    }
}
