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
package io.agentscope.extensions.higress;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.modelcontextprotocol.spec.McpSchema;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Verifies the installed Higress extension with Boot 2 dependency management on Java 8.
 *
 * @author 芋道源码
 */
@Timeout(20)
class Native8HigressConsumerTest {

    @Test
    void findsAndCallsAToolThroughTheInstalledMcpTransport() throws Exception {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        String query = "搜索回显工具";
        String input = "你好，Higress";
        ObjectMapper mapper = new ObjectMapper();
        try (MockWebServer server = new MockWebServer()) {
            server.setDispatcher(
                    new Dispatcher() {
                        @Override
                        public MockResponse dispatch(RecordedRequest request) {
                            if (!"POST".equals(request.getMethod())) {
                                return new MockResponse().setResponseCode(405);
                            }
                            if (!"/mcp?namespace=compat".equals(request.getPath())
                                    || !"native8".equals(request.getHeader("X-Compat-Client"))) {
                                return new MockResponse().setResponseCode(400);
                            }
                            try {
                                return respond(mapper, request, query, input);
                            } catch (Exception error) {
                                return new MockResponse()
                                        .setResponseCode(500)
                                        .setBody(error.toString());
                            }
                        }
                    });
            server.start();
            Duration timeout = Duration.ofSeconds(5);
            HigressMcpClientWrapper client =
                    HigressMcpClientBuilder.create("compat")
                            .streamableHttpEndpoint(server.url("/mcp").toString())
                            .header("X-Compat-Client", "native8")
                            .queryParam("namespace", "compat")
                            .toolSearch(query, 2)
                            .timeout(timeout)
                            .initializationTimeout(timeout)
                            .buildAsync()
                            .block(timeout);
            assertThat(client).isNotNull();
            try {
                List<McpSchema.Tool> tools = client.listTools().block(timeout);
                assertThat(tools).extracting(McpSchema.Tool::name).containsExactly("echo");
                assertThat(tools.get(0).inputSchema()).containsEntry("type", "object");
                McpSchema.CallToolResult reply =
                        client.callTool(
                                        "echo",
                                        Collections.<String, Object>singletonMap("message", input))
                                .block(timeout);
                assertThat(reply).isNotNull();
                assertThat(reply.isError()).isFalse();
                assertThat(reply.content()).hasSize(1);
                assertThat(((McpSchema.TextContent) reply.content().get(0)).text())
                        .isEqualTo(input);
            } finally {
                client.close();
            }
        }
    }

    private static MockResponse respond(
            ObjectMapper mapper, RecordedRequest request, String query, String input)
            throws Exception {
        JsonNode message = mapper.readTree(request.getBody().readUtf8());
        JsonNode params = message.path("params");
        ObjectNode envelope = mapper.createObjectNode().put("jsonrpc", "2.0");
        envelope.set("id", message.path("id"));
        ObjectNode result = envelope.putObject("result");
        switch (message.path("method").asText()) {
            case "initialize":
                result.set("protocolVersion", params.path("protocolVersion"));
                result.putObject("capabilities").putObject("tools");
                result.putObject("serverInfo").put("name", "local").put("version", "1");
                break;
            case "notifications/initialized":
                return new MockResponse().setResponseCode(202);
            case "tools/list":
                result.putArray("tools")
                        .addObject()
                        .put("name", HigressMcpClientWrapper.TOOL_SEARCH_NAME)
                        .putObject("inputSchema")
                        .put("type", "object");
                break;
            case "tools/call":
                JsonNode arguments = params.path("arguments");
                String name = params.path("name").asText();
                result.put("isError", false);
                if (HigressMcpClientWrapper.TOOL_SEARCH_NAME.equals(name)
                        && query.equals(arguments.path("query").asText())
                        && arguments.path("topK").asInt() == 2) {
                    result.putArray("content");
                    ObjectNode tool =
                            result.putObject("structuredContent")
                                    .putArray("tools")
                                    .addObject()
                                    .put("name", "echo")
                                    .put("description", "Echo");
                    tool.putObject("inputSchema").put("type", "object");
                } else if ("echo".equals(name)
                        && input.equals(arguments.path("message").asText())) {
                    result.putArray("content").addObject().put("type", "text").put("text", input);
                } else {
                    return new MockResponse().setResponseCode(400).setBody(name);
                }
                break;
            default:
                return new MockResponse().setResponseCode(400);
        }
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(envelope));
    }
}
