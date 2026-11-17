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
package io.agentscope.extensions.channel.dingtalk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Checks the actual registration, socket callback and ACK path without a DingTalk account.
 *
 * @author 芋道源码
 */
@Timeout(20)
class DingTalkStreamJava8Test {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void registersReceivesUnicodeAndAcknowledgesThroughTheSharedTransport() throws Exception {
        BlockingQueue<String> acks = new LinkedBlockingQueue<>();
        BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String gateway = server.url("/gateway").toString().replaceFirst("^http", "ws");
            server.setDispatcher(
                    new Dispatcher() {
                        @Override
                        public MockResponse dispatch(RecordedRequest request) {
                            if ("/register".equals(request.getPath())) {
                                return new MockResponse()
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                                "{\"endpoint\":\""
                                                        + gateway
                                                        + "\",\"ticket\":\"local-ticket\"}");
                            }
                            if ("/gateway?ticket=local-ticket".equals(request.getPath())) {
                                return new MockResponse()
                                        .withWebSocketUpgrade(
                                                new WebSocketListener() {
                                                    @Override
                                                    public void onOpen(
                                                            WebSocket socket, Response response) {
                                                        socket.send(
                                                                "{\"type\":\"CALLBACK\",\"headers\":{\"messageId\":\"m-local\"},"
                                                                    + "\"data\":\"{\\\"text\\\":\\\"你好\\\"}\"}");
                                                    }

                                                    @Override
                                                    public void onMessage(
                                                            WebSocket socket, String message) {
                                                        acks.add(message);
                                                    }

                                                    @Override
                                                    public void onClosing(
                                                            WebSocket socket,
                                                            int code,
                                                            String reason) {
                                                        socket.close(code, reason);
                                                    }
                                                });
                            }
                            return new MockResponse().setResponseCode(404);
                        }
                    });
            DingTalkChannelProperties props =
                    new DingTalkChannelProperties(
                            "key",
                            "secret",
                            "robot",
                            "stream",
                            null,
                            null,
                            null,
                            server.url("/register").toString());
            DingTalkStreamClient client = new DingTalkStreamClient(props, messages::add);
            try {
                client.start();
                client.start();
                JsonNode payload = messages.poll(10, TimeUnit.SECONDS);
                assertNotNull(payload);
                assertEquals("你好", payload.path("text").asText());
                String ack = acks.poll(5, TimeUnit.SECONDS);
                assertNotNull(ack);
                JsonNode json = JSON.readTree(ack);
                assertEquals(200, json.path("code").asInt());
                assertEquals("m-local", json.path("headers").path("messageId").asText());
                RecordedRequest registration = server.takeRequest(5, TimeUnit.SECONDS);
                assertNotNull(registration);
                JsonNode request = JSON.readTree(registration.getBody().readUtf8());
                assertEquals("key", request.path("clientId").asText());
                assertEquals("secret", request.path("clientSecret").asText());
                assertEquals(2, server.getRequestCount());
            } finally {
                client.stop();
                client.stop();
            }
            assertThrows(IllegalStateException.class, client::start);
        }
    }

    @Test
    void stopBeforeStartIsIdempotentAndReleasesOwnedClients() {
        DingTalkStreamClient client =
                new DingTalkStreamClient(
                        new DingTalkChannelProperties(
                                "key", "secret", "robot", "stream", null, null, null, null),
                        ignored -> fail("Unstarted client must not dispatch"));
        client.stop();
        client.stop();
        assertThrows(IllegalStateException.class, client::start);
    }
}
