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
package io.agentscope.extensions.sandbox.agentrun;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Verifies the installed AgentRun extension with Boot 2 dependency management on Java 8.
 *
 * @author 芋道源码
 */
@Timeout(20)
class Native8AgentRunConsumerTest {

    @Test
    void restoresOwnedStateAndReleasesItThroughTheDataPlane() throws Exception {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(204));
            AgentRunSandboxClientOptions options =
                    new AgentRunSandboxClientOptions()
                            .setApiKey("fixture-key")
                            .setAccountId("fixture-account")
                            .setTemplateName("fixture-template")
                            .setMcpServerUrl(server.url("/mcp").toString())
                            .setDataPlaneBaseUrl(server.url("/").toString());
            AgentRunSandboxClient client = new AgentRunSandboxClient(options, null);
            WorkspaceSpec workspace = new WorkspaceSpec();
            workspace.setRoot("/home/agentscope/workspace/兼容");
            Sandbox original = client.create(workspace, null, null);
            AgentRunSandboxState state = (AgentRunSandboxState) original.getState();
            Sandbox resumed = client.resume(client.deserializeState(client.serializeState(state)));
            AgentRunSandboxState restored = (AgentRunSandboxState) resumed.getState();
            assertThat(restored.getSandboxId()).isEqualTo(state.getSandboxId());
            assertThat(restored.getWorkspaceRoot()).isEqualTo(workspace.getRoot());
            assertThat(restored.isSandboxOwned()).isTrue();

            resumed.shutdown();
            RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("DELETE");
            assertThat(request.getPath()).isEqualTo("/sandboxes/" + state.getSandboxId());
            assertThat(request.getHeader("X-API-Key")).isEqualTo("fixture-key");
            assertThat(request.getHeader("X-Acs-Parent-Id")).isEqualTo("fixture-account");
        }
    }
}
