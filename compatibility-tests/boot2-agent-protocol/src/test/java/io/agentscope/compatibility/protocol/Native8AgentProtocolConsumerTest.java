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
package io.agentscope.compatibility.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.extensions.agentprotocol.AgentFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests the installed Agent Protocol auto-configuration in a native Java 8 servlet host.
 *
 * @author 芋道源码
 */
@SpringBootTest(
        classes = Native8AgentProtocolConsumerTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.address=127.0.0.1",
            "agentscope.agent-protocol.enabled=true",
            "agentscope.agent-protocol.task-store-path=target/fixture-tasks"
        })
class Native8AgentProtocolConsumerTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void returnsTheProtocolResponseForAMissingTask() {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        ResponseEntity<JsonNode> response =
                http.getForEntity("/tasks/local-fixture-task", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("task_id").asText()).isEqualTo("local-fixture-task");
        assertThat(response.getBody().path("status").asText()).isEqualTo("error");
        assertThat(response.getBody().path("error").asText()).isEqualTo("task not found");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        AgentFactory agentFactory() {
            return request -> {
                throw new IllegalStateException("The lookup must not create an agent");
            };
        }
    }
}
