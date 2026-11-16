/*
 * Copyright 2025 the original author or authors.
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
package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClient;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import org.junit.jupiter.api.Test;

/**
 * Reads daemon information through the source-ported SDK on the actual Java 8 VM.
 *
 * @author 芋道源码
 */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
        named = "sandbox.docker.tests",
        matches = "true")
class RuntimeSandboxDockerJava8Tests {
    @Test
    void connectsThroughTheConfiguredDockerSocketWithoutCreatingContainers() throws Exception {
        assertNotNull(
                System.getenv("DOCKER_HOST"), "Explicit local socket required for this probe");
        DockerClient client = new DockerClient(null, new PortManager(new PortRange(58500, 58502)));
        try {
            assertTrue(client.connect(), "SDK infoCmd must reach the configured Docker daemon");
            assertTrue(client.isConnected());
        } finally {
            client.close();
            client.close();
            assertFalse(client.isConnected());
            assertThrows(IllegalStateException.class, client::connect);
        }
    }
}
