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
package io.agentscope.extensions.sandbox.e2b;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Consumes the installed E2B backend under an independent Boot 2 parent on Java 8.
 *
 * @author 芋道源码
 */
@Timeout(30)
class Native8E2bConsumerTest {

    @Test
    void boot2ContextConstructsInstalledSandboxClient() throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        assertEquals(
                1,
                Collections.list(
                                getClass()
                                        .getClassLoader()
                                        .getResources(
                                                "io/agentscope/extensions/sandbox/e2b/E2bSandboxClient.class"))
                        .size());
        assertTrue(
                E2bSandboxClient.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toString()
                        .endsWith(".jar"));
        new ApplicationContextRunner()
                .withBean(E2bSandboxClient.class, E2bSandboxClient::new)
                .run(
                        context -> {
                            assertTrue(context.isRunning());
                            assertNotNull(context.getBean(E2bSandboxClient.class));
                            assertEquals(
                                    "2.7.18",
                                    org.springframework.boot.SpringBootVersion.getVersion());
                            assertEquals("2.21.4", new ObjectMapper().version().toString());
                        });
    }

    @Test
    void actualPlatformClientSendsLocalCreateRequest() throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setHeader("Content-Type", "application/json")
                            .setBody("{\"sandboxID\":\"native8-fixture\"}"));
            server.start();
            E2bSandboxClientOptions options = new E2bSandboxClientOptions();
            options.setApiKey("dummy-native8-key");
            options.setApiBaseUrl(server.url("/").toString());
            new E2bPlatformHttp(options)
                    .createSandbox(
                            "fixture-template",
                            60,
                            Collections.singletonMap("COMPAT_MESSAGE", "预算 café 你好"));
            RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("POST", request.getMethod());
            assertEquals("/sandboxes", request.getPath());
            JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
            assertEquals("fixture-template", body.get("templateID").asText());
            assertEquals(60, body.get("timeout").asInt());
            assertEquals("预算 café 你好", body.get("envVars").get("COMPAT_MESSAGE").asText());
        }
    }

    @Test
    void snapshotReferenceBoundedReaderWorksOnNative8() throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        byte[] reference = E2bSnapshotRefs.encodeSnapshotId("native8-快照");
        assertEquals("native8-快照", E2bSnapshotRefs.decodeSnapshotIdIfPresent(reference));
        assertNotNull(E2bSnapshotRefs.peekSnapshotId(new ByteArrayInputStream(reference)));
        assertThrows(
                java.io.IOException.class,
                () -> E2bSnapshotRefs.peekSnapshotId(new ByteArrayInputStream(new byte[5000])));
    }
}
