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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serializes non-throwing sink emissions from network callbacks, close and shutdown.
 * A close racing with onNext is queued without blocking on the receive subscriber.
 *
 * @author 芋道源码
 */
final class WebSocketSignalQueue {
    private final Queue<Runnable> signals = new ConcurrentLinkedQueue<>();
    private final AtomicInteger work = new AtomicInteger();

    void emit(Runnable signal) {
        signals.add(signal);
        if (work.getAndIncrement() != 0) return;
        int missed = 1;
        for (; ; ) {
            Runnable next;
            while ((next = signals.poll()) != null) next.run();
            missed = work.addAndGet(-missed);
            if (missed == 0) return;
        }
    }
}
