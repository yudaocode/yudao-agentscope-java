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

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.ollama.OllamaChatModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exercises the installed Ollama starter against the native Ollama chat API.
 *
 * @author 芋道源码
 */
class OllamaStarterChatTest {

    private static ConfigurableApplicationContext context;

    private static Model model;

    @BeforeAll
    static void startApplication() {
        String endpoint =
                Objects.requireNonNull(
                        System.getProperty("ollama.base-url"), "Set -Dollama.base-url");
        String modelName =
                Objects.requireNonNull(System.getProperty("ollama.model"), "Set -Dollama.model");
        assertThat(SpringBootVersion.getVersion())
                .isEqualTo(System.getProperty("compat.boot.version"));
        assertThat(System.getProperty("java.specification.version"))
                .isEqualTo(System.getProperty("compat.java.version"));

        context =
                new SpringApplicationBuilder(Application.class)
                        .web(WebApplicationType.NONE)
                        .run(
                                "--agentscope.model.provider=ollama",
                                "--agentscope.ollama.base-url=" + endpoint,
                                "--agentscope.ollama.model-name=" + modelName);
        model = context.getBean(Model.class);
        assertThat(model).isInstanceOf(OllamaChatModel.class);
        assertThat(model.getModelName()).isEqualTo(modelName);
    }

    @AfterAll
    static void closeApplication() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void completesChatWithUsage() {
        List<ChatResponse> responses = chat("Name a color in a short full sentence.", false);

        assertThat(responses).hasSize(1);
        ChatResponse completed = responses.get(0);
        assertThat(hasText(completed)).isTrue();
        assertCompleted(completed);
    }

    @Test
    void streamsTextDeltasAndTerminalUsage() {
        List<ChatResponse> responses = chat("Give a short sentence about a sunny day.", true);

        assertThat(responses.stream().filter(OllamaStarterChatTest::hasText).count())
                .isGreaterThan(1);
        assertThat(responses).last().satisfies(OllamaStarterChatTest::assertCompleted);
    }

    private static List<ChatResponse> chat(String question, boolean stream) {
        Msg prompt = Msg.builder().role(MsgRole.USER).textContent(question).build();
        GenerateOptions options =
                GenerateOptions.builder().stream(stream).temperature(0.0).maxTokens(24).build();
        List<ChatResponse> responses =
                model.stream(Collections.singletonList(prompt), Collections.emptyList(), options)
                        .collectList()
                        .block(Duration.ofSeconds(60));
        assertThat(responses).isNotNull();
        return responses;
    }

    private static boolean hasText(ChatResponse response) {
        return response.getContent().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .anyMatch(text -> !text.trim().isEmpty());
    }

    private static void assertCompleted(ChatResponse response) {
        assertThat(response.getFinishReason()).isNotBlank();
        assertThat(response.getMetadata()).containsEntry("model", model.getModelName());
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getInputTokens()).isPositive();
        assertThat(response.getUsage().getOutputTokens()).isPositive();
    }

    /**
     * Loads the starter through Spring Boot discovery.
     *
     * @author 芋道源码
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {}
}
