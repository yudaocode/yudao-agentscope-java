<!-- YUDAO-COMPATIBILITY-INTRO-BEGIN -->

> 版本命名：保留当前源码基线的官方版本号；共用现代制品不加 JDK 后缀，隔离维护的兼容路线添加 `jdk8` / `jdk17` / `jdk25`。SNAPSHOT 尚未发布 Maven Central。JDK 路线后缀不等于最低字节码版本，框架依赖仍按 Boot 版本选择。
> 完整版本规则与预览命名见 [VERSIONING.md](VERSIONING.md)。

# yudao-agentscope-java

yudao-agentscope-java 是 [AgentScope Java](https://github.com/agentscope-ai/agentscope-java) 的芋道社区兼容维护版本，并非官方发布。主要补充 **Spring Boot 2.x + JDK 8** 支持；`main` 以 JDK 17 为最低目标，由同一份核心和基础 starter 制品验证 Spring Boot 3.x、4.x 与 JDK 17、25，其他扩展仍需逐项验收。

## 仓库与版本线

| 平台 | 仓库地址 |
| --- | --- |
| Gitee | [yudao-agentscope-java](https://gitee.com/yudaocode/yudao-agentscope-java) |
| GitHub | [yudao-agentscope-java](https://github.com/yudaocode/yudao-agentscope-java) |

| 分支 | 目标环境 | 源码开发版本 | 发布规则 | 状态 |
| --- | --- | --- | --- | --- |
| [`main`](https://github.com/yudaocode/yudao-agentscope-java/tree/main) | Boot 3.x / 4.x，JDK 17 / 25 | `2.1.0-SNAPSHOT` | 官方版本 + 日期 | 核心和基础 starter 已完成同 jar 四组合验证，其他扩展验收中 |
| [`main-jdk8`](https://github.com/yudaocode/yudao-agentscope-java/tree/main-jdk8) | Boot 2.x，JDK 8 | `2.1.0-jdk8-SNAPSHOT` | 官方版本 + 路线 + 日期 | 核心、基础与 DashScope starter 已通过 Boot 2.7/JDK 8 验证，其他扩展移植中 |

AgentScope 的现代制品由 Boot 3/4 共用，版本不带 JDK 后缀。当前分支：**`main-jdk8`**。

Maven groupId 为 **`cn.iocoder.agentscope`**，artifactId 和 Java 包名沿用官方。同一个应用只选择一条版本线，同一项目的模块使用同一版本；替换已有依赖时，也要移除对应官方 groupId 下的同类模块，避免重复类。

**当前仅提供源码开发版，尚未发布这些坐标到 Maven Central 或远端 SNAPSHOT 仓库。** 下面的 SNAPSHOT 示例须先从源码安装到本地仓库。发布版本按 `官方版本[-jdk数字][-preview]-YYYYMMDD` 命名；共用制品省略 JDK，未发布上游基线增加 preview，无同日序号。

2026-11-17 前一检查点 **69 模块完整串行 `clean install`** 已在原生 JDK8 上跑完：10,358 项，失败/错误均为 0，条件跳过 71 项。此前分批门禁继续保留；本次提供全默认范围的一次完整检查点，不包含仍被排除的可选扩展和真实平台账号验收。

此前 **82 模块完整原生 JDK8 `clean install`** 已通过：10,724 项，零失败/错误，71 项条件跳过。新增 Durable runtime/worker、Harness/Claude/AG-UI 适配器及 JDBC/HTTP/sandbox/RocketMQ/objects/MCP 扩展；11 个运行库单独通过 366 项，另有 2 项原生 Boot2 消费测试。

当前默认范围已扩为 **84 模块、66 个运行库**，新增 **E2B 沙箱后端**。完整原生JDK8 `clean install` 通过 **10920项**，零失败/错误，80项条件跳过；E2B组件196项中9项真实云条件跳过，独立Boot2消费3项全部通过、零跳过。使用时按BOM引入 `agentscope-extensions-sandbox-e2b`，并显式引入其provided依赖 `agentscope-harness`；完整配置、Java调用示例与Boot2父POM覆盖见 [Java8兼容说明](JAVA8-COMPATIBILITY.md#e2b-backend-on-java-8) 和 [独立消费者](compatibility-tests/boot2-e2b)。真实E2B云环境及其他未列入默认构建的沙箱后端尚未验收。

## 如何使用

当前默认构建包含 84 个 Maven 模块、66 个运行库：核心、Harness、AG-UI、独立 runtime-sandbox、DashScope/OpenAI/OpenAI 官方 Responses/Ollama/Anthropic/Gemini 及其 starter、AG-UI MVC/WebFlux starter、Chat Completions Web SDK 与 starter，以及 JDBC/MySQL/PostgreSQL/Redis/MongoDB 存储，OSS/COS 对象存储、Git/MySQL/PostgreSQL 技能仓库，以及 Mem0/百炼/ReMe Memory、百炼/Dify/Haystack/RAGFlow RAG 与 Quartz/XXL-Job 调度。已纳入真实 JDK 8 回归；其余扩展源码保留，未列入兼容 BOM。详细接入方式见 [Java 8 使用说明](JAVA8-COMPATIBILITY.md)。

Durable 按需引入 `agentscope-durable-runtime`、`agentscope-durable-worker` 或 `agentscope-extensions-durable-jdbc` / `-http` 等（同 groupId、由 BOM 管理）。`LocalBackend.open(Paths.get("./data/durable"))` 提供 SQLite 和不可变文件存储；业务保存并关闭返回的 `SessionBackend`。不要在 JDK8 业务源码中直接使用上游示例里的 `var`、`record` 或 `List.of`。完整原生 Boot2 示例见 [boot2-durable](compatibility-tests/boot2-durable)。对象存储的 MinIO/OSS SDK 为可选依赖，使用对应后端时需自行按 BOM 引入。

```xml
<properties>
    <yudao-agentscope.version>2.1.0-jdk8-SNAPSHOT</yudao-agentscope.version>
    <!-- 继承 Boot 2.7 parent 时，覆盖其旧版 HTTP 组件管理 -->
    <httpclient5.version>5.6.5</httpclient5.version>
    <httpcore5.version>5.4.4</httpcore5.version>
    <kotlin.version>1.9.24</kotlin.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.iocoder.agentscope</groupId>
            <artifactId>agentscope-bom</artifactId>
            <version>${yudao-agentscope.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>cn.iocoder.agentscope</groupId>
        <artifactId>agentscope-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

只使用 Java 核心 API 时，可将 starter 换为同 groupId 的 `agentscope-core`；Java 导入仍为 `io.agentscope.*`。模型扩展按实际需要单独引入，不能将核心和基础 starter 的验证结果理解为全部扩展兼容。

使用 DashScope 自动配置时，将上述依赖的 artifactId 换为 `agentscope-dashscope-spring-boot-starter`，配置 `agentscope.dashscope.api-key`、`agentscope.dashscope.model-name` 和 `agentscope.agent.enabled=true`。可注入 `ReActAgent` 后调用 `agent.call("你好").block()`。

新增可选模块按需引用 `agentscope-openai-official-spring-boot-starter`、`agentscope-agui-spring-boot-starter`、`agentscope-chat-completions-web-starter`，坐标 groupId 与版本由上述 BOM 管理。OpenAI 官方 SDK 保留 4.99.0，配置 `agentscope.model.provider=openai-official`、`agentscope.openai-official.api-key` 和 `agentscope.openai-official.model-name`。AG-UI starter 需提供 `AguiAgentRegistry`，支持 MVC/WebFlux；Chat Completions starter 需提供 prototype 作用域的 `ReActAgent`，默认路径 `/v1/chat/completions`，可用 `agentscope.chat-completions.enabled=false` 关闭。详细配置见 [Java 8 使用说明](JAVA8-COMPATIBILITY.md)。

XXL-Job 扩展按需引入 `agentscope-extensions-scheduler-xxl-job`，默认使用已核验 Java8 字节码的 XXL-Job `2.5.0`；应用自行启动并传入 `XxlJobExecutor`。继承 Boot2 parent 时，还需设置 `<xxl-job.version>2.5.0</xxl-job.version>` 以统一直接管理的版本。客户端注册/任务执行与 Groovy 运行测试在真实 JDK8 上共 36 项通过；远程调度中心未验收。

Nacos Prompt/Skill 扩展按需引入 `agentscope-extensions-nacos-prompt`、`agentscope-extensions-nacos-skill`，同 groupId、版本由 BOM 管理。保留上游 Nacos Client `3.3.0`；应用提供 `com.alibaba.nacos.api.ai.AiService`，分别创建 `NacosPromptListener(aiService)`、`NacosSkillRepository(aiService, namespaceId)`。Prompt/Skill 原有 79 项测试在真实 JDK8 上通过；Nacos A2A 与 Nacos starter 尚未纳入本分支，远程 Nacos 服务需单独验收。

Jev、OpenAI Decisions 评判扩展及两个 starter 也已纳入兼容 BOM：`agentscope-extensions-jev` / `agentscope-jev-spring-boot-starter`、`agentscope-extensions-openai-decisions` / `agentscope-openai-decisions-spring-boot-starter`。按需引入一套库或 starter；配置前缀分别为 `agentscope.jev`、`agentscope.openai-decisions`，API key 使用对应 `api-key` 属性，使用自己的服务时设置 `base-url`。可注入 `JevClient` / `OpenAIDecisionsClient`；未使用时不添加依赖，或设置对应 `enabled=false`。本批两个库与两个 starter 共 241 项 JDK8 测试通过（含本地真实 Chrome DOM 回归），远端付费评判服务未验收。

Channels 扩展现已纳入 Java8 构建：`agentscope-extensions-channel-common` 与 `agentscope-extensions-channel-dingtalk` / `-feishu` / `-github` / `-gitlab` / `-wecom` / `-weixin`。同 groupId、版本由 BOM 管理；应用还需引入 `agentscope-harness`，使用 webhook/WebClient 的场景提供 Boot2 的 WebFlux 运行依赖并注册相应 Controller。通过对应 Channel 的 `fromProperties(channelId, routing, rawProperties)` 构建并交给 Harness Gateway；属性键遵循模块的 `*ChannelProperties`，如 GitHub 的 `token`/`webhookSecret`、GitLab 的 `token`/`webhookToken`。微信接入见[模块说明](agentscope-extensions/agentscope-extensions-channel/agentscope-extensions-channel-weixin/README.md)。

七个库共180项真实JDK8测试，零失败/错误、1项实服条件跳过；包括新增钉钉本地HTTP注册/WebSocket/ACK测试和GitHub/GitLab认证、事件分派、去重测试。未调用真实平台账号、未发送平台消息。

源码构建使用 JDK 17、Maven 3.9，业务应用继续使用 JDK 8。先安装 `yudao-spring-ai` 的 `main-jdk8` 分支，再在本仓库执行：

```bash
# JAVA8_HOME 指向实际 JDK 8 目录，用它执行测试
mvn -Djvm="$JAVA8_HOME/bin/java" clean install
```

独立沙箱 SDK 通过 `cn.iocoder.agentscope:agentscope-runtime-sandbox-core` 按需引入，版本由 AgentScope BOM 管理。Boot 2 使用 `2.1.0-jdk8-SNAPSHOT`，Boot 3/4 共用 `2.1.0-SNAPSHOT`；不需要引入整个 AgentScope core。使用兼容 SandboxService 管理服务，或配置本机 Docker 与所需沙箱镜像。源码来源、构建、请求超时及资源关闭说明见 [Runtime sandbox 使用说明](agentscope-runtime-sandbox-core/README_zh.md)。外部平台与具体工具服务需按实际环境配置，不能将本地兼容测试理解为所有外部后端均已验收。

下方逐字保留官方项目原始 README，其中的官方 Maven 坐标和发布状态仅针对上游项目；使用本兼容版时，以本节坐标、版本线和支持范围为准。
<!-- YUDAO-COMPATIBILITY-INTRO-END -->

---

<p align="center">
  <img
    src="https://img.alicdn.com/imgextra/i1/O1CN01nTg6w21NqT5qFKH1u_!!6000000001621-55-tps-550-550.svg"
    alt="AgentScope Logo"
    width="200"
  />
</p>

<span align="center">

[**中文主页**](https://github.com/agentscope-ai/agentscope-java/blob/main/README_zh.md) | [**日本語ホームページ**](https://github.com/agentscope-ai/agentscope-java/blob/main/README_ja.md) | [**Documentation**](https://java.agentscope.io/)

</span>

<p align="center">
    <a href="https://discord.gg/eYMpfnkG8h">
        <img
            src="https://img.shields.io/badge/Discord-Join%20Us-5865F2?logo=discord&logoColor=white"
            alt="discord"
        />
    </a>
    <a href="https://java.agentscope.io/">
        <img
            src="https://img.shields.io/badge/Docs-English%7C%E4%B8%AD%E6%96%87-blue?logo=markdown"
            alt="docs"
        />
    </a>
    <a href="https://img.shields.io/maven-central/v/io.agentscope/agentscope">
        <img
            src="https://img.shields.io/maven-central/v/io.agentscope/agentscope?color=green"
            alt="Maven Central"
        />
    </a>
    <a href="#">
        <img
            src="https://img.shields.io/badge/JDK-17%2B-orange?logo=openjdk"
            alt="JDK 17+"
        />
    </a>
    <a href="./LICENSE">
        <img
            src="https://img.shields.io/badge/license-Apache--2.0-black"
            alt="license"
        />
    </a>
    <a href="https://deepwiki.com/agentscope-ai/agentscope-java">
        <img
            src="https://deepwiki.com/badge.svg"
            alt="Ask DeepWiki"
        />
    </a>
</p>

## What is AgentScope Java 2.0?

AgentScope Java 2.0 is a production-ready framework for building distributed, enterprise-grade agents, providing essential abstractions that work with rising model capability and built-in support for long-running, safely-controlled agent execution.

- [**Event System** →](https://java.agentscope.io/dev/en/docs/building-blocks/message-and-event.html) A unified event stream with 32 typed events for real-time frontend rendering and human-in-the-loop.
- [**Permission System** →](https://java.agentscope.io/dev/en/docs/building-blocks/permission-system.html) Tool-call gating: allow / require user approval / deny.
- [**Middleware** →](https://java.agentscope.io/dev/en/docs/building-blocks/middleware.html) AOP-style hook interception for flexibly extending the reasoning-acting loop.
- [**Workspace & Sandbox** →](https://java.agentscope.io/dev/en/docs/harness/workspace.html) Run tools in isolated environments — local, Docker, Kubernetes, or AgentRun cloud sandbox.
- [**Multi-Agent Orchestration** →](https://java.agentscope.io/dev/en/docs/harness/subagent.html) Multiple subagent definition patterns with `agent_spawn` / `agent_send` and real-time event forwarding.
- [**Distributed Deployment** →](https://java.agentscope.io/dev/en/docs/others/going-to-production.html) True distributed session and memory management (Redis / MySQL / PostgreSQL / OSS / COS) with cross-replica session recovery.
- [**Agent Evolution** →](https://help.aliyun.com/zh/document_detail/3042583.html) Provides Agent observability and auditing, Agent evaluation and experimentation, and Agent asset management and continuous optimization.

<img src="./docs/imgs/landscape.png" alt="agentscope" width="100%"/>

## News
<!-- BEGIN NEWS -->
- **[2026-07] `v2.0.0 GA`:** First production-ready release! Dual-layer agent architecture, event stream, permission system, middleware, workspace sandbox, multi-agent orchestration, and distributed deployment all ready. [Docs](https://java.agentscope.io/) | [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0)
- **[2026-07] `v2.0.0-RC5`:** Model provider modularization; unified DataBlock multimodal support; native structured output; Channel IM integration; Tencent Cloud COS state persistence. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC5)
- **[2026-06] `v2.0.0-RC4`:** Async tool execution and scheduled wakeup dispatching; subagent cross-replica routing and session recovery. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC4)
- **[2026-06] `v2.0.0-RC3`:** Unified `call()` / `streamEvents()` execution core; `AgentResultEvent`, `CustomEvent`, `HintBlockEvent` new event types. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC3)
- **[2026-06] `v2.0.0-RC2`:** Fully stateless agent; one-line DistributedBackend; subagent event forwarding; Channel module (DingTalk, Feishu, WeCom); A2A / AG-UI protocol support. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC2)
- **[2026-05] `v2.0.0-RC1`:** First 2.0 RC — harness engineering, enterprise distributed deployment, event stream / message model / middleware / HITL full redesign. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC1)
- **[2026-05] `v1.1.0`:** Introduced `agentscope-harness` and `HarnessAgent`; Workspace (persona / memory / skills / subagents); pluggable filesystem (local / shared store / sandbox); session persistence with cross-process resume; layered memory and context compaction; declarative subagent orchestration. [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v1.1.0-RC1)
<!-- END NEWS -->

## Community

Welcome to join our community on

| [Discord](https://discord.gg/eYMpfnkG8h)                     | DingTalk                                                              | WeChat                                                       |
|--------------------------------------------------------------|-----------------------------------------------------------------------|--------------------------------------------------------------|
| <img src="./docs/imgs/discord.png" width="100" height="100"> | <img src="./docs/imgs/dingtalk_qr_code.png" width="100" height="100"> | <img src="./docs/imgs/wechat.jpeg" width="100" height="100"> |

## Quickstart

### Installation

> AgentScope Java requires **JDK 17** or higher.

#### Maven

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>2.0.3</version>
</dependency>
```

Model providers are shipped as separate extension modules in 2.0. Add the one you need — for example, DashScope:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-model-dashscope</artifactId>
    <version>2.0.3</version>
</dependency>
```

Other options: `agentscope-extensions-model-openai`, `agentscope-extensions-model-anthropic`, `agentscope-extensions-model-gemini`, `agentscope-extensions-model-ollama`. See the [Model docs](https://java.agentscope.io/dev/en/docs/building-blocks/model.html) for details.

For typed text and image classification, see [OpenAI Decisions](docs/v2/en/integration/judge/openai-decisions.md), available in `2.1.0-SNAPSHOT` builds containing the extension.

To use the reasoning and tool APIs of `ReActAgent` and compose application capabilities yourself, depend on `agentscope-core` and your model extension.

## Hello AgentScope!

Configure a shared Builder at startup, then use `builder.build()` to create and close an Agent for each request. The same identity and persistent storage keep the conversation across instances. Set `DASHSCOPE_API_KEY` before running:

```java
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Paths;

public class FirstAgent {
    // Configure once at startup; requests only call build().
    private static final HarnessAgent.Builder AGENT_BUILDER = HarnessAgent.builder()
            .name("assistant")
            .agentId("assistant")
            .sysPrompt("You are a helpful AI assistant.")
            .model("dashscope:qwen-plus")
            .workspace(Paths.get(".agentscope/workspace"));

    public static void main(String[] args) {
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId("demo").userId("alice").build();
            agent.call(new UserMessage("Hello!"), ctx).block();
        }

        // COMMENT_Summarize today in three bullets.
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId("demo").userId("alice").build();
            agent.streamEvents(new UserMessage("Summarize today in three bullets."), ctx)
                    .doOnNext(event -> {
                        switch (event.getType()) {
                            case TEXT_BLOCK_DELTA -> System.out.print(
                                    ((TextBlockDeltaEvent) event).getDelta());
                            case TOOL_CALL_START -> System.out.println(
                                    "\n[tool] " + ((ToolCallStartEvent) event).getToolCallName());
                            default -> { }
                        }
                    })
                    .blockLast();
        }
    }
}
```

Start with `call` / `streamEvents` for ordinary replies, multi-turn chat and workflow steps; follow the [Quick Start](https://java.agentscope.io/dev/en/docs/quickstart). Both also record Harness conversation history by default. Introduce [AgentSession](https://java.agentscope.io/dev/en/docs/harness/session-log) when work must continue after a page closes, queue while busy or resume after interruption. The [recoverable chat example](./agentscope-examples/agents/agentscope-chat/README.md) shows the full integration.

## Key Design

AgentScope Java 2.0 is a major step up from a "build an agent" toolkit toward a complete platform for **running agents in production**. The improvements fall into three focus areas:

### 1 · Harness Engineering — built for long-running, complex tasks

A bare ReAct loop solves one reasoning turn. **HarnessAgent** layers engineering infrastructure on top of ReActAgent via Middleware and Toolkit channels — the reasoning core stays untouched, capabilities layer on:

- **Self-evolution & skill repository** — successful patterns auto-save as Markdown skills, shared across sessions and loaded on demand
- **Layered memory** — in-context conversation + agent-curated `MEMORY.md` + on-disk fact log, with auto-compaction to bound the prompt
- **Sub-agents** — declare child agent specs in Markdown, `agent_spawn` / `agent_send` at runtime, sync or background delegation
- **Auto context management** — structured compaction preserves goals / state / findings / next steps; oversized tool results offload to disk
- **Plan Mode** — read-only planning state for long tasks, plan files persist and drive execution

### 2 · Enterprise-grade distributed deployment

Production agents must serve many tenants, run untrusted code safely, and survive rolling restarts. AgentScope 2.0 is built for stateless horizontal scaling:

- **Multi-tenant isolation** — `session` / `user` / `agent` / `org` dimension isolation, `RuntimeContext` keys flow through workspace paths, KV namespaces, and sandbox state slots
- **Secure sandbox** — local subprocess / Docker / Kubernetes / E2B cloud sandbox, with snapshot and resume
- **Permission control** — three-state engine (allow / approve / deny), sensitive tools require HITL approval
- **Session recovery** — `AgentStateStore` (in-memory / JSON file / MySQL / Redis / PostgreSQL) backs zero-downtime rolling deploys

### 3 · Foundation framework — a leaner, more developer-friendly core

Messages, events, and the extension model are smaller, more orthogonal — HITL and event streaming are part of how the framework runs, not add-ons:

- **Event stream** — 32 typed events covering model calls, text deltas, tool execution, and user confirmations in real time
- **Message model** — text / files / images / audio / video / tool results unified into `ContentBlock`, role-strict validation at construction
- **Middleware** — `onAgent` / `onReasoning` / `onActing` / `onModelCall` / `onSystemPrompt` / `onAgentStateReady` six stages replace v1's flat hooks
- **HITL first class** — confirm tool arguments, approve sensitive actions, hand off to external systems, agent pauses and resumes exactly

For the complete architecture overview, see the [documentation](https://java.agentscope.io/dev/en/docs/index.html).

## Contributing

We welcome contributions from the community! Please refer to our [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines
on how to contribute.

## License

AgentScope is released under Apache License 2.0.

## Contributors

All thanks to our contributors:

<a href="https://github.com/agentscope-ai/agentscope-java/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=agentscope-ai/agentscope-java&max=999&columns=12&anon=1" />
</a>
