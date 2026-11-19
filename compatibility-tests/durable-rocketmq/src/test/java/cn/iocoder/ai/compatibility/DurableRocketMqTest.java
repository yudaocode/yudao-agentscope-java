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

package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.durable.DurableJson;
import io.agentscope.durable.DurableRuntime;
import io.agentscope.durable.DurableTypes.ExecutionRequest;
import io.agentscope.durable.DurableTypes.ExecutionResult;
import io.agentscope.durable.DurableTypes.ExecutionState;
import io.agentscope.durable.DurableTypes.SessionKey;
import io.agentscope.durable.DurableTypes.ThreadKey;
import io.agentscope.durable.DurableTypes.TurnStatus;
import io.agentscope.durable.Executor;
import io.agentscope.durable.SessionBackend;
import io.agentscope.durable.TransactionalSessionBackend;
import io.agentscope.durable.Wakeups;
import io.agentscope.durable.agentscope.AgentScopeAdapter;
import io.agentscope.extensions.durable.jdbc.JdbcRecordStore;
import io.agentscope.extensions.durable.rocketmq.RocketMqRemoteExecutor;
import io.agentscope.extensions.durable.rocketmq.RocketMqWorker;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Checks real RocketMQ delivery and worker crash recovery with separate PostgreSQL ledgers.
 *
 * @author 芋道源码
 */
public class DurableRocketMqTest {
    private static final SessionKey SESSION = new SessionKey("fixture", "rocketmq");
    private static final ThreadKey THREAD = new ThreadKey(SESSION, "main");
    private static final String ROUTE = "hands";

    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"normal", "crash-after-effect"})
    @Timeout(100)
    void sendsAndRecoversAcrossWorkerProcesses(String mode) throws Exception {
        String endpoint = required("durable.mq.endpoint");
        String jdbc = required("durable.mq.jdbc");
        String namespace = required("durable.mq.namespace") + "-" + mode;
        String schema = "durable_" + UUID.randomUUID().toString().replace("-", "");
        String workerSchema = schema + "_worker";
        ddl(jdbc, "CREATE SCHEMA " + schema);
        Process first = null;
        Process replacement = null;
        try {
            ddl(jdbc, "CREATE SCHEMA " + workerSchema);
            try (ConfigurableApplicationContext boot = startBoot();
                    JdbcRecordStore brain = open(jdbc, schema)) {
                SessionBackend backend =
                        new TransactionalSessionBackend(brain).withWakeups(Wakeups.local());
                assertBoot(required("durable.mq.boot"), required("durable.mq.jdk"));
                AtomicInteger modelCalls = new AtomicInteger();
                RocketMqRemoteExecutor executor =
                        RocketMqRemoteExecutor.builder()
                                .namespace(namespace)
                                .clientConfiguration(client(endpoint))
                                .pollInterval(Duration.ofMillis(50))
                                .retryInterval(Duration.ofSeconds(1))
                                .build();
                try (DurableRuntime runtime =
                        DurableRuntime.builder()
                                .backend(backend)
                                .harness("agent", "v1", adapter(modelCalls))
                                .executor(ROUTE, executor)
                                .pollInterval(Duration.ofMillis(50))
                                .build()) {
                    first = startWorker("first", mode, endpoint, jdbc, workerSchema, namespace);
                    assertWorkerReady(first, "first");
                    runtime.createSession(SESSION, "agent");
                    runtime.start();
                    String turnId =
                            runtime.getSession(SESSION).submit("request", "Use echo").turnId();
                    Path effects = directory.resolve("effects");
                    if ("crash-after-effect".equals(mode)) {
                        await(
                                "first worker produced effect",
                                first,
                                () -> Files.isRegularFile(effects) && !read(effects).isEmpty(),
                                () -> read(directory.resolve("first.log")));
                        first.destroyForcibly();
                        assertTrue(
                                first.waitFor(10, TimeUnit.SECONDS), "Killed worker did not exit");
                        assertFalse(first.isAlive());
                        assertTrue(first.exitValue() != 0);
                        replacement =
                                startWorker(
                                        "replacement",
                                        "normal",
                                        endpoint,
                                        jdbc,
                                        workerSchema,
                                        namespace);
                        assertWorkerReady(replacement, "replacement");
                    }
                    await(
                            "turn reaches a terminal state",
                            replacement == null ? first : replacement,
                            () ->
                                    runtime.getSession(SESSION)
                                            .getTurn(turnId)
                                            .status()
                                            .status()
                                            .terminal(),
                            () ->
                                    "status="
                                            + runtime.getSession(SESSION).getTurn(turnId).status()
                                            + ", failure="
                                            + runtime.lastFailure(THREAD)
                                            + ", first="
                                            + read(directory.resolve("first.log"))
                                            + ", replacement="
                                            + read(directory.resolve("replacement.log")));
                    assertEquals(
                            TurnStatus.SUCCEEDED,
                            runtime.getSession(SESSION).getTurn(turnId).status().status());
                    assertEquals(
                            "recovered: recorded effect",
                            runtime.getSession(SESSION)
                                    .getTurn(turnId)
                                    .status()
                                    .output()
                                    .path("content")
                                    .get(0)
                                    .path("text")
                                    .asText());
                    assertNull(runtime.lastFailure(THREAD));
                    assertEquals(2, modelCalls.get());
                    List<String> completedEffects =
                            Files.readAllLines(effects, StandardCharsets.UTF_8);
                    assertEquals(1, completedEffects.size());
                    long applied =
                            backend.events(SESSION, null, 100).events().stream()
                                    .filter(event -> "tool.result.applied".equals(event.type()))
                                    .count();
                    assertEquals(1, applied);
                    if ("crash-after-effect".equals(mode)) {
                        List<String> queries =
                                Files.readAllLines(
                                        directory.resolve("queries"), StandardCharsets.UTF_8);
                        assertFalse(queries.isEmpty());
                        assertTrue(queries.stream().allMatch(completedEffects.get(0)::equals));
                    }
                }
            }
        } finally {
            stop(replacement);
            stop(first);
            ddl(jdbc, "DROP SCHEMA IF EXISTS " + workerSchema + " CASCADE");
            ddl(jdbc, "DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private Process startWorker(
            String name, String mode, String endpoint, String jdbc, String schema, String namespace)
            throws IOException {
        String classpath =
                System.getProperty(
                        "surefire.test.class.path", System.getProperty("java.class.path"));
        return new ProcessBuilder(
                        Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        classpath,
                        DurableRocketMqTest.class.getName(),
                        name,
                        mode,
                        endpoint,
                        jdbc,
                        schema,
                        namespace,
                        directory.toString(),
                        required("durable.mq.boot"),
                        required("durable.mq.jdk"))
                .redirectErrorStream(true)
                .redirectOutput(directory.resolve(name + ".log").toFile())
                .start();
    }

    private void assertWorkerReady(Process process, String name) throws InterruptedException {
        await(
                name + " worker started",
                process,
                () -> Files.exists(directory.resolve(name + ".ready")),
                () -> read(directory.resolve(name + ".log")));
    }

    public static void main(String[] args) throws Exception {
        Path directory = Paths.get(args[6]);
        try (ConfigurableApplicationContext boot = startBoot();
                JdbcRecordStore records = open(args[3], args[4]);
                RocketMqWorker worker =
                        RocketMqWorker.builder()
                                .namespace(args[5])
                                .executor(ROUTE, provider(directory, args[1]))
                                .recordStore(records)
                                .clientConfiguration(client(args[2]))
                                .authorize(
                                        command -> {
                                            if (!SESSION.equals(command.request().session())) {
                                                throw new IllegalArgumentException(
                                                        "Unexpected fixture session");
                                            }
                                        })
                                .pollInterval(Duration.ofMillis(50))
                                .leaseDuration(Duration.ofSeconds(2))
                                .build()) {
            assertBoot(args[7], args[8]);
            worker.start();
            Files.createFile(directory.resolve(args[0] + ".ready"));
            Thread.sleep(TimeUnit.SECONDS.toMillis(110));
        }
    }

    private static Executor provider(Path directory, String mode) {
        return new Executor() {
            @Override
            public Capabilities capabilities() {
                return new Capabilities(false, true, true);
            }

            @Override
            public ExecutionResult startExecution(ExecutionRequest request) {
                append(directory.resolve("effects"), request.executionId());
                if ("crash-after-effect".equals(mode)) {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(90));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
                return result(request);
            }

            @Override
            public ExecutionResult query(ExecutionRequest request, String handle) {
                String persisted = read(directory.resolve("effects")).trim();
                if (!request.executionId().equals(persisted)) {
                    throw new IllegalStateException(
                            "Recovery tried to query a different execution");
                }
                append(directory.resolve("queries"), request.executionId());
                return result(request);
            }

            @Override
            public ExecutionResult cancel(ExecutionRequest request, String handle) {
                throw new AssertionError("Unexpected cancellation");
            }
        };
    }

    private static ExecutionResult result(ExecutionRequest request) {
        return new ExecutionResult(
                ExecutionState.SUCCEEDED,
                request.executionId(),
                DurableJson.value("recorded effect"),
                null);
    }

    private static AgentScopeAdapter adapter(AtomicInteger modelCalls) {
        Model model =
                new Model() {
                    @Override
                    public String getModelName() {
                        return "rocketmq-fixture";
                    }

                    @Override
                    public Flux<ChatResponse> stream(
                            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                        modelCalls.incrementAndGet();
                        java.util.Optional<ToolResultBlock> applied =
                                messages.stream()
                                        .flatMap(
                                                message ->
                                                        message
                                                                .getContentBlocks(
                                                                        ToolResultBlock.class)
                                                                .stream())
                                        .filter(
                                                result ->
                                                        "call".equals(result.getId())
                                                                && !result.isSuspended())
                                        .findFirst();
                        if (applied.isPresent()) {
                            String text =
                                    applied.get().getOutput().stream()
                                            .filter(TextBlock.class::isInstance)
                                            .map(TextBlock.class::cast)
                                            .map(TextBlock::getText)
                                            .findFirst()
                                            .orElse("");
                            if (!text.contains("recorded effect")) {
                                return Flux.error(
                                        new IllegalStateException("Unexpected result: " + text));
                            }
                            return Flux.just(
                                    ChatResponse.builder()
                                            .content(
                                                    Collections.singletonList(
                                                            TextBlock.builder()
                                                                    .text(
                                                                            "recovered: recorded"
                                                                                    + " effect")
                                                                    .build()))
                                            .build());
                        }
                        return Flux.just(
                                ChatResponse.builder()
                                        .content(
                                                Collections.singletonList(
                                                        ToolUseBlock.builder()
                                                                .id("call")
                                                                .name("echo")
                                                                .input(
                                                                        Collections.singletonMap(
                                                                                "value", 7))
                                                                .build()))
                                        .build());
                    }
                };
        return AgentScopeAdapter.forReAct(
                binding -> {
                    Toolkit toolkit = new Toolkit();
                    toolkit.registerSchema(
                            ToolSchema.builder()
                                    .name("echo")
                                    .description("Fixture tool")
                                    .parameters(Collections.singletonMap("type", "object"))
                                    .build());
                    return ReActAgent.builder()
                            .name("agent")
                            .model(model)
                            .toolkit(toolkit)
                            .permissionContext(
                                    PermissionContextState.builder()
                                            .mode(PermissionMode.BYPASS)
                                            .build());
                },
                Collections.singletonMap("echo", ROUTE));
    }

    private static ConfigurableApplicationContext startBoot() {
        return new SpringApplicationBuilder(Fixture.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", "logging.level.root=warn")
                .run();
    }

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing test property " + key);
        }
        return value;
    }

    private static void assertBoot(String boot, String jdk) {
        assertEquals(boot, SpringBootVersion.getVersion());
        assertEquals(jdk, System.getProperty("java.specification.version"));
    }

    private static JdbcRecordStore open(String jdbc, String schema) {
        Properties properties = new Properties();
        properties.setProperty("currentSchema", schema);
        return JdbcRecordStore.postgres(jdbc, properties);
    }

    private static ClientConfiguration client(String endpoint) {
        return ClientConfiguration.newBuilder()
                .setEndpoints(endpoint)
                .enableSsl(false)
                .setRequestTimeout(Duration.ofSeconds(4))
                .build();
    }

    private static void ddl(String jdbc, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbc);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void await(
            String description,
            Process process,
            BooleanSupplier reached,
            Supplier<String> diagnostics)
            throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!reached.getAsBoolean() && process.isAlive() && System.nanoTime() < end) {
            Thread.sleep(50);
        }
        assertTrue(reached.getAsBoolean(), () -> description + ": " + diagnostics.get());
    }

    private static void stop(Process process) throws InterruptedException {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Fixture worker did not exit");
        }
    }

    private static String read(Path file) {
        if (!Files.isRegularFile(file)) return "";
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void append(Path file, String line) {
        try {
            Files.write(
                    file,
                    Collections.singletonList(line),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @author 芋道源码
     */
    @Configuration(proxyBeanMethods = false)
    public static class Fixture {}
}
