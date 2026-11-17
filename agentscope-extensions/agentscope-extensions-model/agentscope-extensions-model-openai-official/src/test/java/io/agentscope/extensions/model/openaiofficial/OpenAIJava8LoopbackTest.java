/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.extensions.model.openaiofficial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Exercises the upstream SDK's actual HTTP and JSON path against a local endpoint.
 *
 * @author 芋道源码
 */
class OpenAIJava8LoopbackTest {

    @Test
    @Timeout(20)
    void responsesRoundTripUsesTheConfiguredClientAndPreservesUnicode() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    path.set(exchange.getRequestURI().getPath());
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    try (InputStream input = exchange.getRequestBody();
                            ByteArrayOutputStream captured = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) captured.write(buffer, 0, count);
                        body.set(new String(captured.toByteArray(), StandardCharsets.UTF_8));
                    }
                    byte[] response =
                            ("{\"id\":\"resp-local\",\"object\":\"response\",\"created_at\":1,"
                                 + "\"status\":\"completed\",\"error\":null,\"incomplete_details\":null,"
                                 + "\"model\":\"gpt-4o\",\"parallel_tool_calls\":false,\"tools\":[],"
                                 + "\"tool_choice\":\"auto\",\"temperature\":1,\"top_p\":1,\"metadata\":{},"
                                 + "\"output\":[{\"type\":\"message\",\"id\":\"msg-local\",\"status\":\"completed\","
                                 + "\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"你好"
                                 + " Java8\",\"annotations\":[]}]}]}")
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders()
                            .set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, response.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(response);
                    }
                });
        server.start();
        OpenAIClient client = null;
        try {
            client =
                    OpenAISdkClientFactory.createClient(
                            "test-key",
                            "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                            null,
                            Duration.ofSeconds(10),
                            null);
            Response response =
                    client.responses()
                            .create(
                                    ResponseCreateParams.builder()
                                            .model("gpt-4o")
                                            .input("你好")
                                            .build());
            assertEquals(
                    "你好 Java8",
                    response.output().get(0).asMessage().content().get(0).asOutputText().text());
            assertEquals("/v1/responses", path.get());
            assertEquals("Bearer test-key", authorization.get());
            JsonNode request = new ObjectMapper().readTree(body.get());
            assertEquals("gpt-4o", request.path("model").asText());
            assertEquals("你好", request.path("input").asText());
        } finally {
            if (client != null) client.close();
            server.stop(0);
        }
    }

    @Test
    @Timeout(20)
    void streamingResponsesDecodeUnicodeDeltasThroughTheRealSdk() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/responses",
                exchange -> {
                    try (InputStream input = exchange.getRequestBody()) {
                        byte[] buffer = new byte[1024];
                        while (input.read(buffer) != -1) {}
                    }
                    String event =
                            "event: response.output_text.delta\n"
                                + "data:"
                                + " {\"type\":\"response.output_text.delta\",\"sequence_number\":0,"
                                + "\"item_id\":\"msg-local\",\"output_index\":0,\"content_index\":0,\"delta\":\"你好"
                                + " Java8\",\"logprobs\":[]}\n\n"
                                + "data: [DONE]\n\n";
                    byte[] bytes = event.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders()
                            .set("Content-Type", "text/event-stream; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                });
        server.start();
        OpenAIClient client = null;
        try {
            client =
                    OpenAISdkClientFactory.createClient(
                            "test-key",
                            "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                            null,
                            Duration.ofSeconds(10),
                            null);
            try (com.openai.core.http.StreamResponse<
                            com.openai.models.responses.ResponseStreamEvent>
                    response =
                            client.responses()
                                    .createStreaming(
                                            ResponseCreateParams.builder()
                                                    .model("gpt-4o")
                                                    .input("你好")
                                                    .build())) {
                String text =
                        response.stream()
                                .filter(
                                        com.openai.models.responses.ResponseStreamEvent
                                                ::isOutputTextDelta)
                                .map(value -> value.asOutputTextDelta().delta())
                                .collect(java.util.stream.Collectors.joining());
                assertEquals("你好 Java8", text);
            }
        } finally {
            if (client != null) client.close();
            server.stop(0);
        }
    }
}
