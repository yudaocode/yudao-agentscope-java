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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClient;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Uses the Runtime SDK's public Docker overloads with one labelled fixture container.
 * The fixture image must already exist locally; it is never pulled by this test.
 * @author 芋道源码
 */
@Timeout(30)
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
        named = "sandbox.docker.tests",
        matches = "true")
class RuntimeSandboxDockerLifecycleTests {
    @Test
    void createsExecutesStopsAndRemovesOnlyTheTaskContainer() throws Exception {
        String image = System.getenv("SANDBOX_PROBE_IMAGE");
        assertNotNull(image);
        assertTrue(image.startsWith("sha256:"));
        String task = UUID.randomUUID().toString();
        String label = "cn.iocoder.agentscope.compatibility-probe";
        DefaultDockerClientConfig config =
                DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ZerodepDockerHttpClient transport =
                new ZerodepDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();
        try (com.github.dockerjava.api.DockerClient raw =
                        DockerClientImpl.getInstance(config, transport);
                DockerClient sdk = new DockerClient()) {
            raw.inspectImageCmd(image).exec();
            // Add only the fixture process and ownership label; SDK production code still
            // applies container name, port/volume/environment and host runtime configuration.
            com.github.dockerjava.api.DockerClient fixture =
                    (com.github.dockerjava.api.DockerClient)
                            Proxy.newProxyInstance(
                                    getClass().getClassLoader(),
                                    new Class<?>[] {com.github.dockerjava.api.DockerClient.class},
                                    (proxy, method, args) -> {
                                        try {
                                            Object value = method.invoke(raw, args);
                                            if (method.getName().equals("createContainerCmd")) {
                                                ((CreateContainerCmd) value)
                                                        .withCmd("sh", "-c", "sleep 25")
                                                        .withLabels(
                                                                Collections.singletonMap(
                                                                        label, task));
                                            }
                                            return value;
                                        } catch (InvocationTargetException error) {
                                            throw error.getCause();
                                        }
                                    });
            String id = null;
            try {
                CreateContainerResponse created =
                        sdk.createContainers(
                                fixture,
                                "codex-agentscope-" + task,
                                image,
                                Collections.emptyList(),
                                Collections.emptyMap(),
                                Collections.emptyList(),
                                Collections.singletonMap("SANDBOX_COMPAT_FIXTURE", "true"),
                                Map.of("mem_limit", "64m", "nano_cpus", 100000000L));
                id = created.getId();
                assertEquals(
                        task,
                        raw.inspectContainerCmd(id).exec().getConfig().getLabels().get(label));
                sdk.startContainer(raw, id);
                assertEquals("running", sdk.getContainerStatus(raw, id));
                ExecCreateCmdResponse command =
                        raw.execCreateCmd(id)
                                .withAttachStdout(true)
                                .withAttachStderr(true)
                                .withCmd("sh", "-c", "printf 'sandbox-java8-中文'")
                                .exec();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ByteArrayOutputStream errors = new ByteArrayOutputStream();
                try (ExecStartResultCallback callback =
                        new ExecStartResultCallback(output, errors)) {
                    assertTrue(
                            raw.execStartCmd(command.getId())
                                    .exec(callback)
                                    .awaitCompletion(5, TimeUnit.SECONDS));
                }
                assertEquals(
                        Long.valueOf(0),
                        raw.inspectExecCmd(command.getId()).exec().getExitCodeLong());
                assertEquals(
                        "sandbox-java8-中文",
                        new String(output.toByteArray(), StandardCharsets.UTF_8));
                assertEquals(0, errors.size());
                sdk.stopContainer(raw, id);
                assertEquals("exited", sdk.getContainerStatus(raw, id));
                sdk.removeContainer(raw, id);
                String removed = id;
                assertThrows(
                        NotFoundException.class, () -> raw.inspectContainerCmd(removed).exec());
                id = null;
            } finally {
                if (id != null) {
                    try {
                        if (task.equals(
                                raw.inspectContainerCmd(id)
                                        .exec()
                                        .getConfig()
                                        .getLabels()
                                        .get(label))) {
                            raw.removeContainerCmd(id).withForce(true).exec();
                        }
                    } catch (NotFoundException alreadyRemoved) {
                    }
                }
            }
        }
    }
}
