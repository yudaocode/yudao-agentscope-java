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
package io.agentscope.extensions.channel.github;

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
import java.nio.charset.StandardCharsets;
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
class GitHubWebhookJava8Test {
    private static final String BODY =
            "{\"action\":\"created\",\"repository\":{\"full_name\":\"owner/repo\",\"owner\":{\"login\":\"owner\"}},\"issue\":{\"number\":7},\"comment\":{\"id\":11,\"body\":\"你好\",\"user\":{\"id\":5,\"login\":\"alice\"}}}";
    private final String id = "local-" + UUID.randomUUID();
    private final GitHubChannel channel = mock(GitHubChannel.class);
    private final GitHubChannelRegistry registry = GitHubChannelRegistry.instance();
    private final GitHubWebhookController controller = new GitHubWebhookController(registry);

    @BeforeEach
    void register() {
        when(channel.channelId()).thenReturn(id);
        when(channel.signatureVerifier()).thenReturn(new GitHubSignatureVerifier("hook-secret"));
        when(channel.idempotency()).thenReturn(new IdempotencyStore());
        when(channel.botLoopGuard()).thenReturn(new BotLoopGuard());
        when(channel.mapper()).thenReturn(new GitHubInboundMapper(id));
        when(channel.botIdentity()).thenReturn(mock(GitHubBotIdentityResolver.class));
        when(channel.dispatch(any(InboundMessage.class))).thenReturn(Mono.empty());
        registry.register(channel);
    }

    @AfterEach
    void unregister() {
        registry.unregister(id);
    }

    private int call(String event, String credential, String body) {
        return controller
                .webhook(
                        id,
                        event,
                        "delivery-local",
                        credential,
                        body.getBytes(StandardCharsets.UTF_8))
                .block()
                .getStatusCodeValue();
    }

    private String signature(String body) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(
                new javax.crypto.spec.SecretKeySpec(
                        "hook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder hex = new StringBuilder("sha256=");
        for (byte b : mac.doFinal(body.getBytes(StandardCharsets.UTF_8)))
            hex.append(String.format("%02x", b & 255));
        return hex.toString();
    }

    @Test
    void missingChannelReturns404() throws Exception {
        registry.unregister(id);
        assertEquals(404, call("issue_comment", signature(BODY), BODY));
    }

    @Test
    void rejectsMissingOrInvalidAuthenticationBeforeDispatch() throws Exception {
        assertEquals(401, call("issue_comment", null, BODY));
        assertEquals(401, call("issue_comment", signature(BODY + " "), BODY));
        verify(channel, never()).dispatch(any(InboundMessage.class));
    }

    @Test
    void acknowledgesUnsupportedEventsAndRejectsMalformedJson() throws Exception {
        assertEquals(204, call("Push Hook", signature(BODY), BODY));
        assertEquals(400, call("issue_comment", signature("{"), "{"));
        verify(channel, never()).dispatch(any(InboundMessage.class));
    }

    @Test
    void authenticatesUnicodePayloadAndDeduplicatesDelivery() throws Exception {
        assertEquals(200, call("issue_comment", signature(BODY), BODY));
        assertEquals(200, call("issue_comment", signature(BODY), BODY));
        ArgumentCaptor<InboundMessage> captured = ArgumentCaptor.forClass(InboundMessage.class);
        verify(channel, times(1)).dispatch(captured.capture());
        assertEquals("alice", captured.getValue().senderId());
        assertEquals("你好", captured.getValue().messages().get(0).getTextContent());
    }
}
