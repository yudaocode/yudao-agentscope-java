package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Checks the installed Java 8 transport SPI and cancellation with an idle SSE peer.
 *
 * @author 芋道源码
 */
class Java8A2AHttpClientTest {

    @Test
    void interruptingGetClosesConnectionWhilePeerIsSendingBody() throws Exception {
        CountDownLatch bodyStarted = new CountDownLatch(1);
        CompletableFuture<Void> connectionClosed = new CompletableFuture<>();
        CompletableFuture<Throwable> getResult = new CompletableFuture<>();
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            Thread peer = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    InputStream request = socket.getInputStream();
                    String terminator = "\r\n\r\n";
                    int matched = 0;
                    while (matched < terminator.length()) {
                        int next = request.read();
                        if (next == -1) {
                            throw new IOException("Request ended before the headers");
                        }
                        matched = next == terminator.charAt(matched) ? matched + 1 : 0;
                    }
                    OutputStream output = socket.getOutputStream();
                    output.write(("HTTP/1.1 200 OK\r\nContent-Length: 1024\r\n\r\n"
                            + "{").getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    bodyStarted.countDown();
                    try {
                        assertEquals(-1, request.read());
                    } catch (java.net.SocketException reset) {
                        // A TCP reset also confirms that the peer closed the connection.
                    }
                    connectionClosed.complete(null);
                } catch (Throwable failure) {
                    connectionClosed.completeExceptionally(failure);
                }
            });
            peer.setDaemon(true);
            peer.start();

            A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT);
            Thread reading = new Thread(() -> {
                try {
                    client.createGet().url("http://127.0.0.1:" + server.getLocalPort()).get();
                    getResult.completeExceptionally(new AssertionError("GET returned an incomplete body"));
                } catch (Exception failure) {
                    getResult.complete(failure);
                }
            });
            reading.setDaemon(true);
            reading.start();
            try {
                assertTrue(bodyStarted.await(5, TimeUnit.SECONDS));
                reading.interrupt();
                assertInstanceOf(InterruptedException.class, getResult.get(5, TimeUnit.SECONDS));
                connectionClosed.get(5, TimeUnit.SECONDS);
            } finally {
                reading.interrupt();
            }
        }
    }

    @Test
    void cancelsStreamWhilePeerIsIdle() throws Exception {
        CountDownLatch releaseServer = new CountDownLatch(1);
        CompletableFuture<String> firstMessage = new CompletableFuture<>();
        AtomicReference<Throwable> unexpectedError = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/events",
                exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                    exchange.sendResponseHeaders(200, 0);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write("data: first\n\n".getBytes(StandardCharsets.UTF_8));
                        output.flush();
                        releaseServer.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try {
            A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT);
            assertInstanceOf(Java8A2AHttpClient.class, client);
            CompletableFuture<Void> stream =
                    client.createPost()
                            .url("http://127.0.0.1:" + server.getAddress().getPort() + "/events")
                            .body("{}")
                            .postAsyncSSE(event -> firstMessage.complete(event.data()),
                                    unexpectedError::set,
                                    () -> {});
            assertEquals("first", firstMessage.get(5, TimeUnit.SECONDS));
            assertTrue(CompletableFuture.supplyAsync(() -> stream.cancel(true)).get(3, TimeUnit.SECONDS));
            assertTrue(stream.isCancelled());
            assertNull(unexpectedError.get());
        } finally {
            releaseServer.countDown();
            server.stop(0);
        }
    }
}
