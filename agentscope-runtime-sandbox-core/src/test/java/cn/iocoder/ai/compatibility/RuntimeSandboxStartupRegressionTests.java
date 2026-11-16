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
package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.command.CreateContainerResponse;
import io.agentscope.runtime.sandbox.box.AgentBaySandbox;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.SandboxHttpTransport;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.ContainerCreateResult;
import io.agentscope.runtime.sandbox.manager.client.container.agentbay.AgentBayClient;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.SandboxHttpClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.TrainingSandboxClient;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Exercises startup retry and cleanup failures without external Docker or cloud services.
 *
 * @author 芋道源码
 */
@Timeout(10)
class RuntimeSandboxStartupRegressionTests {
    @Test
    void disconnectedDockerStartupClosesCandidateAndCanRetry() {
        BaseClientStarter starter = mock(BaseClientStarter.class);
        when(starter.getContainerClientType()).thenReturn(ContainerClientType.DOCKER);
        DockerClient failed = mock(DockerClient.class);
        DockerClient ready = mock(DockerClient.class);
        when(ready.isConnected()).thenReturn(true);
        when(starter.startClient(any())).thenReturn(failed, ready);
        try (SandboxService service =
                new SandboxService(ManagerConfig.builder().clientStarter(starter).build())) {
            assertThrows(IllegalStateException.class, service::start);
            verify(failed).close();
            service.start();
            service.start();
            verify(starter, times(2)).startClient(any());
        }
        verify(ready).close();
    }

    @Test
    void failedStartupRetainsItsFailureWhenCandidateCloseAlsoFails() {
        BaseClientStarter starter = mock(BaseClientStarter.class);
        when(starter.getContainerClientType()).thenReturn(ContainerClientType.DOCKER);
        DockerClient failed = mock(DockerClient.class);
        RuntimeException cleanup = new IllegalStateException("fixture cleanup");
        doThrow(cleanup).when(failed).close();
        when(starter.startClient(any())).thenReturn(failed);
        try (SandboxService service =
                new SandboxService(ManagerConfig.builder().clientStarter(starter).build())) {
            IllegalStateException primary =
                    assertThrows(IllegalStateException.class, service::start);
            assertEquals("Container client initialization failed", primary.getMessage());
            assertArrayEquals(new Throwable[] {cleanup}, primary.getSuppressed());
        }
    }

    @Test
    void remoteModeInitializesExplicitAgentBayWithoutStartingDocker() {
        BaseClientStarter forbidden = mock(BaseClientStarter.class);
        doThrow(new AssertionError("Unexpected local container startup"))
                .when(forbidden)
                .startClient(any());
        try (MockedConstruction<AgentBayClient> construction =
                        mockConstruction(
                                AgentBayClient.class,
                                (client, context) -> {
                                    assertEquals(
                                            Collections.singletonList("fixture-key"),
                                            context.arguments());
                                    when(client.createContainer(
                                                    "fixture-image", Collections.emptyMap()))
                                            .thenReturn(
                                                    new ContainerCreateResult("fixture-container"));
                                });
                SandboxService service =
                        new SandboxService(
                                ManagerConfig.builder()
                                        .baseUrl("http://127.0.0.1:1")
                                        .agentBayApiKey("fixture-key")
                                        .clientStarter(forbidden)
                                        .build())) {
            service.start();
            service.start();
            assertEquals(1, construction.constructed().size());
            assertSame(construction.constructed().get(0), service.getAgentBayClient());
            AgentBaySandbox sandbox = mock(AgentBaySandbox.class);
            when(sandbox.getImageId()).thenReturn("fixture-image");
            when(sandbox.getLabels()).thenReturn(Collections.emptyMap());
            when(sandbox.getUserId()).thenReturn("fixture-user");
            when(sandbox.getSessionId()).thenReturn("fixture-session");
            assertEquals("fixture-container", service.createAgentBayContainer(sandbox));
            verify(forbidden, never()).startClient(any());
        }
    }

    @Test
    void agentBayCanStartWithoutDockerAndLocalDockerRemainsRetryable() {
        BaseClientStarter starter = mock(BaseClientStarter.class);
        when(starter.getContainerClientType()).thenReturn(ContainerClientType.DOCKER);
        DockerClient unavailable = mock(DockerClient.class);
        DockerClient ready = mock(DockerClient.class);
        when(ready.isConnected()).thenReturn(true);
        when(starter.startClient(any())).thenReturn(unavailable, ready);
        try (MockedConstruction<AgentBayClient> construction =
                        mockConstruction(
                                AgentBayClient.class,
                                (client, context) -> {
                                    when(client.createContainer(
                                                    "fixture-image", Collections.emptyMap()))
                                            .thenReturn(
                                                    new ContainerCreateResult("fixture-container"));
                                });
                SandboxService service =
                        new SandboxService(
                                ManagerConfig.builder()
                                        .agentBayApiKey("fixture-key")
                                        .clientStarter(starter)
                                        .build())) {
            service.start();
            assertEquals(1, construction.constructed().size());
            assertSame(construction.constructed().get(0), service.getAgentBayClient());
            verify(unavailable).close();
            AgentBaySandbox sandbox = mock(AgentBaySandbox.class);
            when(sandbox.getImageId()).thenReturn("fixture-image");
            when(sandbox.getLabels()).thenReturn(Collections.emptyMap());
            when(sandbox.getUserId()).thenReturn("fixture-user");
            when(sandbox.getSessionId()).thenReturn("fixture-session");
            assertEquals("fixture-container", service.createAgentBayContainer(sandbox));
            BaseSandbox local = mock(BaseSandbox.class);
            IllegalStateException failure =
                    assertThrows(IllegalStateException.class, () -> service.createContainer(local));
            assertEquals(
                    "Local container client is unavailable; retry start()", failure.getMessage());
            verifyNoInteractions(local);
            service.start();
            service.start();
            assertEquals(1, construction.constructed().size());
            verify(starter, times(2)).startClient(any());
        }
        verify(ready).close();
    }

    @Test
    void concurrentDockerCloseDoesNotReplaceCapturedClientWithNull() throws Exception {
        CountDownLatch checked = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean pause = new AtomicBoolean();
        com.github.dockerjava.api.DockerClient raw =
                mock(com.github.dockerjava.api.DockerClient.class);
        DockerClient sdk =
                new DockerClient() {
                    @Override
                    public boolean isConnected() {
                        boolean connected = super.isConnected();
                        if (pause.compareAndSet(true, false)) {
                            checked.countDown();
                            try {
                                assertTrue(resume.await(3, TimeUnit.SECONDS));
                            } catch (InterruptedException failure) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(failure);
                            }
                        }
                        return connected;
                    }

                    @Override
                    public CreateContainerResponse createContainers(
                            com.github.dockerjava.api.DockerClient client,
                            String name,
                            String image,
                            List<String> ports,
                            Map<String, Integer> portMapping,
                            List<VolumeBinding> volumes,
                            Map<String, String> environment,
                            Map<String, Object> config) {
                        assertSame(
                                raw,
                                client,
                                "In-flight operation must retain its checked reference");
                        CreateContainerResponse result = new CreateContainerResponse();
                        result.setId("fixture-container");
                        return result;
                    }
                };
        setField(sdk, "client", raw);
        setField(sdk, "connected", true);
        pause.set(true);
        CompletableFuture<ContainerCreateResult> pending =
                CompletableFuture.supplyAsync(
                        () ->
                                sdk.createContainer(
                                        "fixture",
                                        "fixture",
                                        Collections.emptyList(),
                                        Collections.emptyList(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap()));
        try {
            assertTrue(checked.await(3, TimeUnit.SECONDS));
            sdk.close();
            resume.countDown();
            assertEquals("fixture-container", pending.get(3, TimeUnit.SECONDS).getContainerId());
            verify(raw).close();
            assertThrows(IllegalStateException.class, () -> sdk.containerNameExists("after-close"));
        } finally {
            resume.countDown();
            pending.cancel(true);
            sdk.close();
        }
    }

    private static void setField(DockerClient sdk, String name, Object value) throws Exception {
        Field field = DockerClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(sdk, value);
    }

    @Test
    void sandboxClientPreservesHealthFailureWhenCloseFails() {
        RuntimeException primary = new IllegalStateException("fixture health");
        RuntimeException cleanup = new IllegalStateException("fixture cleanup");
        SandboxHttpTransport transport = mock(SandboxHttpTransport.class);
        doThrow(cleanup).when(transport).close();
        try (MockedStatic<SandboxHttpTransport> factory = mockStatic(SandboxHttpTransport.class)) {
            factory.when(() -> SandboxHttpTransport.create(any())).thenReturn(transport);
            assertSame(
                    primary,
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    new SandboxHttpClient(
                                            ContainerModel.builder()
                                                    .baseUrl("http://127.0.0.1:1")
                                                    .sessionId("fixture")
                                                    .authToken("fixture")
                                                    .build(),
                                            1) {
                                        @Override
                                        public void waitUntilHealthy() {
                                            throw primary;
                                        }
                                    }));
            assertArrayEquals(new Throwable[] {cleanup}, primary.getSuppressed());
        }
    }

    @Test
    void trainingClientPreservesHealthFailureWhenCloseFails() {
        RuntimeException primary = new IllegalStateException("fixture health");
        RuntimeException cleanup = new IllegalStateException("fixture cleanup");
        SandboxHttpTransport transport = mock(SandboxHttpTransport.class);
        doThrow(cleanup).when(transport).close();
        try (MockedStatic<SandboxHttpTransport> factory = mockStatic(SandboxHttpTransport.class)) {
            factory.when(() -> SandboxHttpTransport.create(any())).thenReturn(transport);
            assertSame(
                    primary,
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    new TrainingSandboxClient("http://127.0.0.1:1/fixture", 1) {
                                        @Override
                                        public void waitUntilHealthy() {
                                            throw primary;
                                        }
                                    }));
            assertArrayEquals(new Throwable[] {cleanup}, primary.getSuppressed());
        }
    }
}
