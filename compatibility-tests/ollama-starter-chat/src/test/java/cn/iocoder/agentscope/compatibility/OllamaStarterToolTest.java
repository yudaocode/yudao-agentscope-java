/*
 * Copyright 2026 the original author or authors.
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
package cn.iocoder.agentscope.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.options.OllamaOptions;
import io.agentscope.extensions.model.ollama.options.ThinkOption.ThinkBoolean;
import io.agentscope.spring.boot.ollama.OllamaChatModelBuilderCustomizer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises a complete real Ollama tool round through the Spring Boot starter.
 *
 * @author 芋道源码
 */
class OllamaStarterToolTest {

    @Test
    void autoConfiguredAgentRunsJavaToolAndAnswersWithItsResult() {
        String endpoint =
                Objects.requireNonNull(
                        System.getProperty("ollama.base-url"), "Set -Dollama.base-url");
        String modelName = System.getProperty("ollama.tool-model", "qwen3:0.6b");
        assertThat(SpringBootVersion.getVersion())
                .isEqualTo(System.getProperty("compat.boot.version"));
        assertThat(System.getProperty("java.specification.version"))
                .isEqualTo(System.getProperty("compat.java.version"));

        try (ConfigurableApplicationContext context =
                new SpringApplicationBuilder(Application.class)
                        .web(WebApplicationType.NONE)
                        .run(
                                "--agentscope.model.provider=ollama",
                                "--agentscope.ollama.base-url=" + endpoint,
                                "--agentscope.ollama.model-name=" + modelName,
                                "--agentscope.agent.enabled=true",
                                "--agentscope.agent.max-iters=4",
                                "--agentscope.agent.sys-prompt=When asked for a code, use get_code"
                                    + " before answering.")) {
            Model model = context.getBean(Model.class);
            assertThat(model).isInstanceOf(OllamaChatModel.class);
            assertThat(model.getModelName()).isEqualTo(modelName);
            ReActAgent agent = context.getBean(ReActAgent.class);
            assertThat(agent.getModel()).isSameAs(model);

            Msg answer =
                    agent.call("Call get_code to find the code, then report that code.")
                            .block(Duration.ofSeconds(60));

            assertThat(context.getBean(CodeTool.class).calls).hasValue(1);
            assertThat(answer).isNotNull();
            assertThat(answer.getTextContent()).contains("RAIN42");
        }
    }

    /**
     * Registers the tool and sets bounded generation options for the local model.
     *
     * @author 芋道源码
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        CodeTool codeTool() {
            return new CodeTool();
        }

        @Bean
        Toolkit toolkit(CodeTool tool) {
            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(tool);
            return toolkit;
        }

        @Bean
        OllamaChatModelBuilderCustomizer ollamaOptions() {
            return builder ->
                    builder.stream(false)
                            .defaultOptions(
                                    OllamaOptions.builder()
                                            .thinkOption(ThinkBoolean.DISABLED)
                                            .temperature(0.0)
                                            .numThread(2)
                                            .numPredict(128)
                                            .build());
        }
    }

    /**
     * Returns a code absent from the messages supplied to the model.
     *
     * @author 芋道源码
     */
    public static final class CodeTool {
        private final AtomicInteger calls = new AtomicInteger();

        @Tool(name = "get_code", description = "Returns the current code.")
        public String getCode() {
            calls.incrementAndGet();
            return "RAIN42";
        }
    }
}
