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
package io.agentscope.extensions.sandbox.daytona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Exercises the Daytona control plane and toolbox lifecycle via a local HTTP fixture.
 *
 * @author 芋道源码
 */
@Timeout(20)
class DaytonaLifecycleTest {

    @Test
    void createsStartsAndReleasesAnOwnedSandbox() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(json("{\"id\":\"fixture-sandbox\"}"));
            server.enqueue(json("{}"));
            server.enqueue(json("{\"state\":\"started\"}"));
            server.enqueue(json("{\"exitCode\":0,\"result\":\"\"}"));
            server.enqueue(new MockResponse().setResponseCode(204));

            String base = server.url("/").toString();
            base = base.substring(0, base.length() - 1);
            DaytonaSandboxClientOptions options = new DaytonaSandboxClientOptions();
            options.setApiKey("fixture-key");
            options.setControlPlaneBaseUrl(base);
            options.setToolboxBaseUrl(base);
            options.setSnapshotId("fixture-snapshot");
            options.setWorkspaceRoot("/workspace/兼容");
            DaytonaSandboxClient client = new DaytonaSandboxClient(options, null);

            Sandbox sandbox = client.create(new WorkspaceSpec(), null, null);
            sandbox.start();
            DaytonaSandboxState state = (DaytonaSandboxState) sandbox.getState();
            assertEquals("fixture-sandbox", state.getSandboxId());
            assertTrue(state.isWorkspaceRootReady());
            Sandbox resumed = client.resume(client.deserializeState(client.serializeState(state)));
            resumed.shutdown();

            ObjectMapper mapper = new ObjectMapper();
            RecordedRequest create = expect(server, "POST", "/sandbox");
            assertEquals(
                    "fixture-snapshot",
                    mapper.readTree(create.getBody().readUtf8()).path("snapshot").asText());
            expect(server, "POST", "/sandbox/fixture-sandbox/start");
            expect(server, "GET", "/sandbox/fixture-sandbox");
            RecordedRequest setup =
                    expect(server, "POST", "/toolbox/fixture-sandbox/process/execute");
            assertTrue(
                    mapper.readTree(setup.getBody().readUtf8())
                            .path("command")
                            .asText()
                            .contains("/workspace/兼容"));
            expect(server, "DELETE", "/sandbox/fixture-sandbox");
        }
    }

    private static RecordedRequest expect(MockWebServer server, String method, String path)
            throws InterruptedException {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
        assertEquals("Bearer fixture-key", request.getHeader("Authorization"));
        return request;
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}
