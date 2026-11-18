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
package io.agentscope.compatibility.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Boots a real servlet host with the installed AgentScope Admin starter on Java 8.
 *
 * @author 芋道源码
 */
@SpringBootTest(
        classes = Native8AgentAdminConsumerTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"agentscope.admin.enabled=true"})
class Native8AgentAdminConsumerTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void exposesHealthAndTheGroupedAdminDocumentation() {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        ResponseEntity<JsonNode> health = http.getForEntity("/actuator/health", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().path("status").asText()).isEqualTo("UP");

        ResponseEntity<JsonNode> docs =
                http.getForEntity("/v3/api-docs/agentscope-admin", JsonNode.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().path("paths").has("/v1/admin/sessions")).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {}
}
