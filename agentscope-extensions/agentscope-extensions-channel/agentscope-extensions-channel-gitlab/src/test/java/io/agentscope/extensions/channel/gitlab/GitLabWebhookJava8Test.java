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
package io.agentscope.extensions.channel.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentscope.extensions.channel.common.BotLoopGuard;
import io.agentscope.extensions.channel.common.IdempotencyStore;
import io.agentscope.harness.agent.gateway.channel.InboundMessage;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

/**
 * Verifies authentication and local event dispatch without sending platform messages.
 *
 * @author 芋道源码
 */
class GitLabWebhookJava8Test {
    private static final String BODY =
            "{\"object_attributes\":{\"id\":11,\"note\":\"你好\",\"noteable_type\":\"Issue\",\"system\":false},\"project\":{\"path_with_namespace\":\"owner/repo\",\"namespace\":\"owner\"},\"issue\":{\"iid\":7},\"user\":{\"id\":5,\"username\":\"alice\"}}";
    private final String id = "local-" + UUID.randomUUID();
    private final GitLabChannel channel = mock(GitLabChannel.class);
    private final GitLabChannelRegistry registry = GitLabChannelRegistry.instance();
    private final GitLabWebhookController controller = new GitLabWebhookController(registry);

    @BeforeEach
    void register() {
        when(channel.channelId()).thenReturn(id);
        when(channel.properties())
                .thenReturn(
                        new GitLabChannelProperties(
                                "local-token", "hook-secret", "http://127.0.0.1", "/local"));
        when(channel.idempotency()).thenReturn(new IdempotencyStore());
        when(channel.botLoopGuard()).thenReturn(new BotLoopGuard());
        when(channel.mapper()).thenReturn(new GitLabInboundMapper(id));
        when(channel.botIdentity()).thenReturn(mock(GitLabBotIdentityResolver.class));
        when(channel.dispatch(any(InboundMessage.class))).thenReturn(Mono.empty());
        registry.register(channel);
    }

    @AfterEach
    void unregister() {
        registry.unregister(id);
    }

    private int call(String event, String credential, String body) {
        return controller.webhook(id, credential, event, body).block().getStatusCodeValue();
    }

    @Test
    void missingChannelReturns404() throws Exception {
        registry.unregister(id);
        assertEquals(404, call("Note Hook", "hook-secret", BODY));
    }

    @Test
    void rejectsMissingOrInvalidAuthenticationBeforeDispatch() throws Exception {
        assertEquals(401, call("Note Hook", null, BODY));
        assertEquals(401, call("Note Hook", "bad-secret", BODY));
        verify(channel, never()).dispatch(any(InboundMessage.class));
    }

    @Test
    void acknowledgesUnsupportedEventsAndRejectsMalformedJson() throws Exception {
        assertEquals(204, call("Push Hook", "hook-secret", BODY));
        assertEquals(400, call("Note Hook", "hook-secret", "{"));
        verify(channel, never()).dispatch(any(InboundMessage.class));
    }

    @Test
    void authenticatesUnicodePayloadAndDeduplicatesDelivery() throws Exception {
        assertEquals(200, call("Note Hook", "hook-secret", BODY));
        assertEquals(200, call("Note Hook", "hook-secret", BODY));
        ArgumentCaptor<InboundMessage> captured = ArgumentCaptor.forClass(InboundMessage.class);
        verify(channel, times(1)).dispatch(captured.capture());
        assertEquals("alice", captured.getValue().senderId());
        assertEquals("你好", captured.getValue().messages().get(0).getTextContent());
    }
}
