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
package io.agentscope.compatibility.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.agentscope.extensions.sandbox.kubernetes.client.config.DirectConnectionConfig;
import io.agentscope.extensions.sandbox.kubernetes.client.internal.SandboxConnector;
import io.fabric8.kubernetes.client.Config;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.compat.jdkhttp.HttpResponse;

/**
 * Exercises the installed Kubernetes extension and its transport in a Java 8 Boot host.
 *
 * @author 芋道源码
 */
@Timeout(20)
class Native8KubernetesConsumerTest {

    @Test
    void downloadsAfterARealBodylessGatewayRedirect() throws Exception {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/download/folder",
                exchange -> {
                    try {
                        if ("/download/folder%2Fdata".equals(exchange.getRequestURI().getRawPath())) {
                            exchange.getResponseHeaders().set("Location", "/download/folder/data");
                            exchange.sendResponseHeaders(307, -1);
                            return;
                        }
                        byte[] body = "工作区文件".getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(200, body.length);
                        try (OutputStream stream = exchange.getResponseBody()) {
                            stream.write(body);
                        }
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        SandboxConnector connector =
                new SandboxConnector(
                        "fixture",
                        "default",
                        new DirectConnectionConfig("http://127.0.0.1:" + server.getAddress().getPort()),
                        null,
                        null,
                        null,
                        null);
        try {
            connector.connect();
            HttpResponse<byte[]> response =
                    connector.sendRequestForBytes("GET", "download/folder%2Fdata", null, null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.uri().getPath()).isEqualTo("/download/folder/data");
            assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("工作区文件");
        } finally {
            try {
                connector.close();
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void readsKubeconfigUsingTheInstalledFabric8Client() {
        Config config =
                Config.fromKubeconfig(
                        "apiVersion: v1\nkind: Config\n"
                                + "current-context: local\n"
                                + "clusters:\n- name: local\n"
                                + "  cluster:\n    server: http://127.0.0.1:1\n"
                                + "contexts:\n- name: local\n"
                                + "  context:\n    cluster: local\n    namespace: fixture\n"
                                + "users: []\n");
        assertThat(config.getMasterUrl()).isEqualTo("http://127.0.0.1:1/");
        assertThat(config.getNamespace()).isEqualTo("fixture");
    }
}
