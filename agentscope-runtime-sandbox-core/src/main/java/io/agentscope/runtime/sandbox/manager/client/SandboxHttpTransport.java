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
package io.agentscope.runtime.sandbox.manager.client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tracks sandbox requests and bounds the complete response body by the request timeout.
 * Closing cancels this transport's requests and closes an owned client when its JDK
 * implementation supports close. Borrowed clients remain available to other callers.
 *
 * @author 芋道源码
 */
public final class SandboxHttpTransport implements AutoCloseable {
    private HttpClient client;
    private final boolean ownsClient;
    private final Set<CompletableFuture<?>> active = new HashSet<>();
    private boolean closed;

    public SandboxHttpTransport(HttpClient client, boolean ownsClient) {
        this.client = Objects.requireNonNull(client, "client");
        this.ownsClient = ownsClient;
    }

    public static SandboxHttpTransport create(Duration connectTimeout) {
        return new SandboxHttpTransport(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build(), true);
    }

    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        Duration timeout = request.timeout().orElse(null);
        long started = System.nanoTime();
        CompletableFuture<HttpResponse<String>> pending;
        synchronized (active) {
            if (closed) throw new IllegalStateException("Sandbox HTTP client is closed");
            pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            active.add(pending);
        }
        try {
            if (timeout == null) return pending.get();
            long budget;
            try {
                budget = timeout.toNanos();
            } catch (ArithmeticException tooLarge) {
                budget = Long.MAX_VALUE;
            }
            long remaining = Math.max(0, budget - (System.nanoTime() - started));
            return pending.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException error) {
            pending.cancel(true);
            HttpTimeoutException failure =
                    new HttpTimeoutException("Sandbox response deadline exceeded");
            failure.initCause(error);
            throw failure;
        } catch (InterruptedException error) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw error;
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IOException("Sandbox HTTP request failed", cause);
        } finally {
            synchronized (active) {
                active.remove(pending);
            }
        }
    }

    @Override
    public void close() {
        HttpClient owned;
        List<CompletableFuture<?>> calls;
        synchronized (active) {
            if (closed) return;
            closed = true;
            calls = new ArrayList<>(active);
            active.clear();
            owned = ownsClient ? client : null;
            client = null;
        }
        calls.forEach(call -> call.cancel(true));
        // Native JDK17 has no close API. Releasing our reference permits GC cleanup.
        if (owned instanceof AutoCloseable) {
            try {
                ((AutoCloseable) owned).close();
            } catch (Exception error) {
                throw new IllegalStateException("Failed to close sandbox HTTP client", error);
            }
        }
    }
}
