/*
 * Copyright 2024-2026 the original author or authors.
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
package io.agentscope.core.model.transport.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import okhttp3.WebSocket;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.Disposable;

/**
 * Holds reception in onNext while a different thread requests a terminal signal.
 *
 * @author 芋道源码
 */
@Timeout(10)
class WebSocketCloseConcurrencyTest {
    static Stream<Arguments> scenarios() {
        return Stream.of(true, false)
                .flatMap(
                        jdk ->
                                Stream.of("local", "remote", "failure")
                                        .map(terminal -> Arguments.of(jdk, terminal)));
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void terminalSignalWaitsForInFlightMessagesWithoutBlockingClose(boolean jdk, String terminal)
            throws Exception {
        JdkWebSocketConnection<String> legacy =
                new JdkWebSocketConnection<>("ws://fixture/", String.class);
        OkHttpWebSocketConnection<String> okhttp =
                new OkHttpWebSocketConnection<>("ws://fixture/", String.class);
        WebSocket socket = mock(WebSocket.class);
        when(socket.close(anyInt(), anyString())).thenReturn(true);
        legacy.setWebSocket(socket);
        okhttp.setWebSocket(socket);
        WebSocketConnection<String> connection = jdk ? legacy : okhttp;
        Consumer<byte[]> incoming = jdk ? legacy::onMessage : okhttp::onMessage;
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<String> received = new CopyOnWriteArrayList<>();
        Disposable subscription =
                connection
                        .receive()
                        .subscribe(
                                value -> {
                                    received.add(value);
                                    if (value.equals("first")) {
                                        inside.countDown();
                                        try {
                                            assertTrue(release.await(3, TimeUnit.SECONDS));
                                        } catch (InterruptedException error) {
                                            throw new RuntimeException(error);
                                        }
                                    }
                                },
                                error -> {
                                    failure.set(error);
                                    done.countDown();
                                },
                                done::countDown);
        Thread producer =
                new Thread(
                        () -> incoming.accept("first".getBytes(StandardCharsets.UTF_8)),
                        "websocket-blocked-receiver");
        producer.start();
        try {
            assertTrue(inside.await(2, TimeUnit.SECONDS));
            incoming.accept("second".getBytes(StandardCharsets.UTF_8));
            if (terminal.equals("local")) {
                connection.close().block(Duration.ofSeconds(1));
            } else if (terminal.equals("remote")) {
                if (jdk) legacy.onClosed(1000, "normal");
                else okhttp.onClosed(1000, "normal");
            } else {
                if (jdk) legacy.onError(new IllegalStateException("fixture-failure"));
                else okhttp.onError(new IllegalStateException("fixture-failure"));
            }
            assertEquals(1, done.getCount(), "A terminal signal must not overlap onNext");
            release.countDown();
            producer.join(2000);
            assertFalse(producer.isAlive());
            assertTrue(done.await(2, TimeUnit.SECONDS), "Queued terminal signal must be delivered");
            assertEquals(List.of("first", "second"), received);
            if (terminal.equals("failure"))
                assertTrue(failure.get() instanceof WebSocketTransportException);
            else assertNull(failure.get());
        } finally {
            release.countDown();
            producer.join(2000);
            subscription.dispose();
        }
    }
}
