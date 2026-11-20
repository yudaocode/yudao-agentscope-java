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
package io.agentscope.harness.agent.sandbox.impl.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Verifies that a real non-root Docker sandbox can cold-restore POSIX workspace modes.
 *
 * @author 芋道源码
 */
@EnabledIfSystemProperty(named = "docker.it", matches = "true")
class DockerSandboxWorkspaceModeIntegrationTest {

    @Test
    void coldRestorePreservesWorkspaceModesAsNonRoot() throws Exception {
        try (Sandbox source = newSandbox();
                Sandbox restored = newSandbox()) {
            source.start();
            assertEquals("65534", run(source, "id -u"));
            run(source, "mkdir shared && chmod 0775 shared"
                    + " && printf sample > shared/data.txt && chmod 0666 shared/data.txt"
                    + " && printf '#!/bin/sh\nprintf restored\n' > shared/check.sh"
                    + " && chmod 0751 shared/check.sh");

            try (InputStream archive = source.persistWorkspace()) {
                restored.start();
                assertEquals("65534", run(restored, "id -u"));
                restored.hydrateWorkspace(archive);
            }

            assertEquals("775\n666\n751", run(restored,
                    "stat -c '%a' shared shared/data.txt shared/check.sh").replace("\r", ""));
            assertEquals("sample", run(restored, "cat shared/data.txt"));
            assertEquals("restored", run(restored, "./shared/check.sh"));
        }
    }

    private Sandbox newSandbox() {
        DockerSandboxClientOptions options = new DockerSandboxClientOptions();
        options.setImage(System.getProperty("docker.it.image", "alpine:3.22"));
        options.setWorkspaceRoot("/tmp/agentscope-workspace");
        options.setNetwork("none");
        options.setAdditionalRunArgs(Arrays.asList("--user", "65534:65534"));
        return new DockerSandboxClient().create(new WorkspaceSpec(), null, options);
    }

    private String run(Sandbox sandbox, String command) throws Exception {
        ExecResult result = sandbox.exec(RuntimeContext.empty(), command, 10);
        assertEquals(0, result.exitCode(), result.stderr());
        return result.stdout().trim();
    }
}
