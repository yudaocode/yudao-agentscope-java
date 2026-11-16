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
package io.agentscope.core.model.transport.websocket;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Real network coverage for the Java 8 WebSocket backend and client ownership.
 * @author 芋道源码
 */
@Timeout(15)
class JdkWebSocketLoopbackTest {
    private MockWebServer server;
    private OkHttpClient borrowed;

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
        borrowed = new OkHttpClient();
    }

    @AfterEach
    void stop() throws Exception {
        borrowed.dispatcher().cancelAll();
        borrowed.connectionPool().evictAll();
        borrowed.dispatcher().executorService().shutdownNow();
        server.shutdown();
    }

    private void enqueueEcho() {
        server.enqueue(
                new MockResponse()
                        .withWebSocketUpgrade(
                                new WebSocketListener() {
                                    @Override
                                    public void onMessage(WebSocket socket, String text) {
                                        socket.send("echo:" + text);
                                    }

                                    @Override
                                    public void onMessage(WebSocket socket, ByteString bytes) {
                                        socket.send(bytes);
                                    }

                                    @Override
                                    public void onClosing(
                                            WebSocket socket, int code, String reason) {
                                        socket.close(code, reason);
                                    }
                                }));
    }

    private WebSocketRequest request() {
        return WebSocketRequest.builder(server.url("/socket").toString())
                .header("X-Probe", "java8")
                .build();
    }

    @Test
    void exchangesTextAndHeadersUsingOwnedClient() throws Exception {
        enqueueEcho();
        JdkWebSocketTransport transport = JdkWebSocketTransport.create();
        try {
            WebSocketConnection<String> connection =
                    transport.connect(request(), String.class).block(Duration.ofSeconds(3));
            assertNotNull(connection);
            CompletableFuture<String> response = connection.receive().next().toFuture();
            connection.send("hello").block(Duration.ofSeconds(2));
            assertEquals("echo:hello", response.get(3, TimeUnit.SECONDS));
            assertEquals("java8", server.takeRequest(2, TimeUnit.SECONDS).getHeader("X-Probe"));
            connection.close().block(Duration.ofSeconds(2));
            assertFalse(connection.isOpen());
        } finally {
            transport.shutdown();
        }
    }

    @Test
    void shutdownClosesOwnedConnectionsAndLeavesBorrowedClientUsable() throws Exception {
        enqueueEcho();
        JdkWebSocketTransport first = JdkWebSocketTransport.create(borrowed);
        JdkWebSocketTransport second = JdkWebSocketTransport.create(borrowed);
        try {
            WebSocketConnection<byte[]> binary =
                    first.connect(request(), byte[].class).block(Duration.ofSeconds(3));
            CompletableFuture<byte[]> echoed = binary.receive().next().toFuture();
            byte[] payload = new byte[] {0, 1, -1, 42};
            binary.send(payload).block(Duration.ofSeconds(2));
            assertArrayEquals(payload, echoed.get(3, TimeUnit.SECONDS));
            first.shutdown();
            assertFalse(binary.isOpen());
            assertFalse(borrowed.dispatcher().executorService().isShutdown());
            enqueueEcho();
            WebSocketConnection<String> connection =
                    second.connect(request(), String.class).block(Duration.ofSeconds(3));
            CompletableFuture<String> response = connection.receive().next().toFuture();
            connection.send("after-shutdown").block(Duration.ofSeconds(2));
            assertEquals("echo:after-shutdown", response.get(3, TimeUnit.SECONDS));
            connection.close().block(Duration.ofSeconds(2));
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    void cancellingPendingHandshakeReleasesTheCall() throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
        Disposable pending = transport.connect(request(), String.class).subscribe();
        try {
            assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
            assertTrue(borrowed.dispatcher().runningCallsCount() > 0);
            pending.dispose();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (borrowed.dispatcher().runningCallsCount() != 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(0, borrowed.dispatcher().runningCallsCount());
            assertFalse(borrowed.dispatcher().executorService().isShutdown());
        } finally {
            pending.dispose();
            transport.shutdown();
        }
    }

    @Test
    void nullRequestTimeoutRetainsClientDefaults() throws Exception {
        enqueueEcho();
        JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
        try {
            Mono<WebSocketConnection<String>> handshake =
                    transport.connect(
                            WebSocketRequest.builder(server.url("/").toString())
                                    .connectTimeout(null)
                                    .build(),
                            String.class);
            WebSocketConnection<String> connection = handshake.block(Duration.ofSeconds(3));
            assertNotNull(connection);
            CompletableFuture<String> response = connection.receive().next().toFuture();
            connection.send("nullable").block(Duration.ofSeconds(2));
            assertEquals("echo:nullable", response.get(3, TimeUnit.SECONDS));
            connection.close().block(Duration.ofSeconds(2));
        } finally {
            transport.shutdown();
        }
    }

    @Test
    void appliesHandshakeDeadlineAndReleasesBorrowedCall() throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
        try {
            StepVerifier.create(
                            transport.connect(
                                    WebSocketRequest.builder(server.url("/").toString())
                                            .connectTimeout(Duration.ofMillis(150))
                                            .build(),
                                    String.class))
                    .expectError(WebSocketTransportException.class)
                    .verify(Duration.ofSeconds(3));
            awaitNoRunningCalls();
            assertFalse(borrowed.dispatcher().executorService().isShutdown());
        } finally {
            transport.shutdown();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void shutdownTerminatesPendingHandshake(boolean ownClient) throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        JdkWebSocketTransport transport =
                ownClient ? JdkWebSocketTransport.create() : JdkWebSocketTransport.create(borrowed);
        CompletableFuture<?> pending = transport.connect(request(), String.class).toFuture();
        try {
            assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
            transport.shutdown();
            java.util.concurrent.ExecutionException failure =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            java.util.concurrent.ExecutionException.class,
                            () -> pending.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof IllegalStateException);
            if (!ownClient) awaitNoRunningCalls();
            assertFalse(borrowed.dispatcher().executorService().isShutdown());
        } finally {
            pending.cancel(true);
            transport.shutdown();
        }
    }

    @Test
    void localCloseCompletesReceiveEvenWhenPeerNeverAcknowledges() throws Exception {
        try (java.net.ServerSocket listener =
                new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            java.util.concurrent.CountDownLatch closeReceived =
                    new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch releasePeer =
                    new java.util.concurrent.CountDownLatch(1);
            CompletableFuture<Void> peer =
                    CompletableFuture.runAsync(
                            () -> {
                                try (java.net.Socket socket = listener.accept()) {
                                    socket.setSoTimeout(3000);
                                    java.io.DataInputStream input = upgrade(socket);
                                    assertEquals(0x88, input.readUnsignedByte());
                                    int length = input.readUnsignedByte() & 0x7f;
                                    byte[] frame = new byte[4 + length];
                                    input.readFully(frame);
                                    closeReceived.countDown();
                                    // Deliberately do not acknowledge; only fixture cleanup
                                    // releases the socket.
                                    assertTrue(releasePeer.await(3, TimeUnit.SECONDS));
                                } catch (Exception error) {
                                    throw new java.util.concurrent.CompletionException(error);
                                }
                            });
            JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
            try {
                WebSocketConnection<String> connection =
                        transport
                                .connect(
                                        WebSocketRequest.builder(
                                                        "ws://127.0.0.1:"
                                                                + listener.getLocalPort()
                                                                + "/")
                                                .build(),
                                        String.class)
                                .block(Duration.ofSeconds(3));
                CompletableFuture<Void> received = connection.receive().then().toFuture();
                connection.close().block(Duration.ofSeconds(2));
                received.get(2, TimeUnit.SECONDS);
                assertTrue(closeReceived.await(2, TimeUnit.SECONDS));
                assertEquals(CloseInfo.NORMAL_CLOSURE, connection.getCloseInfo().code());
            } finally {
                releasePeer.countDown();
                try {
                    peer.get(3, TimeUnit.SECONDS);
                } finally {
                    transport.shutdown();
                }
            }
        }
    }

    @Test
    void acknowledgesEmptyPeerCloseWithoutSendingReservedCode() throws Exception {
        try (java.net.ServerSocket listener =
                new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            CompletableFuture<Void> peer =
                    CompletableFuture.runAsync(
                            () -> {
                                try (java.net.Socket socket = listener.accept()) {
                                    socket.setSoTimeout(3000);
                                    java.io.DataInputStream input = upgrade(socket);
                                    socket.getOutputStream().write(new byte[] {(byte) 0x88, 0});
                                    socket.getOutputStream().flush();
                                    assertEquals(0x88, input.readUnsignedByte());
                                    assertEquals(0x82, input.readUnsignedByte());
                                    byte[] mask = new byte[4];
                                    input.readFully(mask);
                                    int code =
                                            ((input.readUnsignedByte() ^ (mask[0] & 255)) << 8)
                                                    | (input.readUnsignedByte() ^ (mask[1] & 255));
                                    assertEquals(CloseInfo.NORMAL_CLOSURE, code);
                                } catch (Exception error) {
                                    throw new java.util.concurrent.CompletionException(error);
                                }
                            });
            JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
            try {
                WebSocketConnection<String> connection =
                        transport
                                .connect(
                                        WebSocketRequest.builder(
                                                        "ws://127.0.0.1:"
                                                                + listener.getLocalPort()
                                                                + "/")
                                                .build(),
                                        String.class)
                                .block(Duration.ofSeconds(3));
                connection.receive().then().block(Duration.ofSeconds(3));
                assertEquals(1005, connection.getCloseInfo().code());
                peer.get(3, TimeUnit.SECONDS);
            } finally {
                transport.shutdown();
            }
        }
    }

    private static java.io.DataInputStream upgrade(java.net.Socket socket) throws Exception {
        java.io.BufferedReader reader =
                new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                socket.getInputStream(),
                                java.nio.charset.StandardCharsets.US_ASCII));
        String key = null;
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase(java.util.Locale.ROOT).startsWith("sec-websocket-key:"))
                key = line.substring(line.indexOf(':') + 1).trim();
        }
        assertNotNull(key);
        String accept =
                java.util.Base64.getEncoder()
                        .encodeToString(
                                java.security.MessageDigest.getInstance("SHA-1")
                                        .digest(
                                                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .US_ASCII)));
        socket.getOutputStream()
                .write(
                        ("HTTP/1.1 101 Switching Protocols\r\n"
                                        + "Upgrade: websocket\r\n"
                                        + "Connection: Upgrade\r\n"
                                        + "Sec-WebSocket-Accept: "
                                        + accept
                                        + "\r\n\r\n")
                                .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
        return new java.io.DataInputStream(socket.getInputStream());
    }

    private void awaitNoRunningCalls() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (borrowed.dispatcher().runningCallsCount() != 0 && System.nanoTime() < deadline)
            Thread.sleep(10);
        assertEquals(0, borrowed.dispatcher().runningCallsCount());
    }

    @Test
    void monoCreatedBeforeShutdownIsRejectedWhenSubscribed() {
        JdkWebSocketTransport transport = JdkWebSocketTransport.create(borrowed);
        Mono<WebSocketConnection<String>> pending = transport.connect(request(), String.class);
        transport.shutdown();
        StepVerifier.create(pending)
                .expectError(IllegalStateException.class)
                .verify(Duration.ofSeconds(2));
        assertEquals(0, server.getRequestCount());
        assertFalse(borrowed.dispatcher().executorService().isShutdown());
    }
}
