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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.remote.RemoteHttpClient;
import io.agentscope.runtime.sandbox.manager.remote.RequestMethod;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Exercises the isolated Runtime SDK source port on the actual Java 8 VM.
 *
 * @author 芋道源码
 */
class RuntimeSandboxJava8Tests {

    @Test
    void sandboxKeyRetainsValueAndJsonSemantics() throws Exception {
        SandboxKey first = new SandboxKey("user", "session", "base");
        SandboxKey second = new SandboxKey("user", "session", "base");
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, mapper.readValue(mapper.writeValueAsBytes(first), SandboxKey.class));
        assertEquals("session", first.sessionID());
        assertNotEquals(first, new SandboxKey("user", "other", "base"));
    }

    @Test
    void remotePostPreservesBearerAndUtf8Payload() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        HttpServer server =
                server(
                        "/sandbox/echo",
                        200,
                        "{\"data\":{\"value\":\"中文\"}}",
                        authorization,
                        payload,
                        null);
        RemoteHttpClient client = new RemoteHttpClient(url(server), "local-fixture-token");
        try {
            Object result =
                    client.makeRequest(
                            RequestMethod.POST,
                            "/sandbox/echo",
                            Collections.<String, Object>singletonMap("value", "中文"),
                            "data");
            assertTrue(result instanceof Map);
            assertEquals("中文", ((Map<?, ?>) result).get("value"));
            assertEquals("Bearer local-fixture-token", authorization.get());
            assertEquals("中文", new ObjectMapper().readTree(payload.get()).get("value").asText());
        } finally {
            closePrototypeClient(client);
            server.stop(0);
        }
    }

    @Test
    void remoteQueryPreservesUtf8AndReservedCharacters() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = server("/sandbox/query", 200, "{\"data\":\"ok\"}", null, null, query);
        RemoteHttpClient client = new RemoteHttpClient(url(server), null);
        try {
            assertEquals(
                    "ok",
                    client.makeRequest(
                            RequestMethod.GET,
                            "/sandbox/query",
                            Collections.<String, Object>singletonMap("q", "空 格+&"),
                            "data"));
            assertEquals("q=%E7%A9%BA+%E6%A0%BC%2B%26", query.get());
        } finally {
            closePrototypeClient(client);
            server.stop(0);
        }
    }

    @Test
    void remoteHttpErrorsPreserveExistingResultContract() throws Exception {
        HttpServer server =
                server(
                        "/sandbox/error",
                        503,
                        "{\"detail\":\"sandbox unavailable\"}",
                        null,
                        null,
                        null);
        RemoteHttpClient client = new RemoteHttpClient(url(server), null);
        try {
            Object result = client.makeRequest(RequestMethod.GET, "/sandbox/error", null, null);
            assertTrue(result instanceof Map);
            assertTrue(
                    String.valueOf(((Map<?, ?>) result).get("error"))
                            .contains("sandbox unavailable"));
            assertNull(client.makeRequest(RequestMethod.GET, "/sandbox/error", null, "data"));
        } finally {
            closePrototypeClient(client);
            server.stop(0);
        }
    }

    private static HttpServer server(
            String path,
            int status,
            String response,
            AtomicReference<String> authorization,
            AtomicReference<String> payload,
            AtomicReference<String> query)
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                path,
                exchange -> {
                    try {
                        if (authorization != null)
                            authorization.set(
                                    exchange.getRequestHeaders().getFirst("Authorization"));
                        if (query != null) query.set(exchange.getRequestURI().getRawQuery());
                        try (InputStream input = exchange.getRequestBody()) {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int count;
                            while ((count = input.read(buffer)) != -1)
                                bytes.write(buffer, 0, count);
                            if (payload != null)
                                payload.set(
                                        new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                        }
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders()
                                .set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(status, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        return server;
    }

    private static String url(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void closePrototypeClient(RemoteHttpClient client) throws Exception {
        client.close();
    }
}
