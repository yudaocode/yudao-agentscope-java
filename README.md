<!-- YUDAO-COMPATIBILITY-INTRO-BEGIN -->

> 版本命名：保留当前源码基线的官方版本号；共用现代制品不加 JDK 后缀，隔离维护的兼容路线添加 `jdk8` / `jdk17` / `jdk25`。开发版本使用 SNAPSHOT。JDK 路线后缀不等于最低字节码版本，框架依赖仍按 Boot 版本选择。
> 完整版本规则与预览命名见 [VERSIONING.md](VERSIONING.md)。

# yudao-agentscope-java

yudao-agentscope-java 是 [AgentScope Java](https://github.com/agentscope-ai/agentscope-java) 的芋道社区兼容维护版本，并非官方发布。主要补充 **Spring Boot 2.x + JDK 8** 支持；`main` 以 JDK 17 为最低目标，由同一份核心和基础 starter 制品验证 Spring Boot 3.x、4.x 与 JDK 17、25，其他扩展仍需逐项验收。

## 仓库与版本线

| 平台 | 仓库地址 |
| --- | --- |
| Gitee | [yudao-agentscope-java](https://gitee.com/yudaocode/yudao-agentscope-java) |
| GitHub | [yudao-agentscope-java](https://github.com/yudaocode/yudao-agentscope-java) |

| 分支 | 目标环境 | 源码开发版本 | Maven Central | 状态 |
| --- | --- | --- | --- | --- |
| [`main`](https://github.com/yudaocode/yudao-agentscope-java/tree/main) | Boot 3.x / 4.x，JDK 17 / 25 | `2.1.0-SNAPSHOT` | [2.1.0-preview-20261119](https://central.sonatype.com/artifact/cn.iocoder.agentscope/agentscope-bom/2.1.0-preview-20261119) | 核心和基础 starter 已完成同 jar 四组合验证，其他扩展验收中 |
| [`main-jdk8`](https://github.com/yudaocode/yudao-agentscope-java/tree/main-jdk8) | Boot 2.x，JDK 8 | `2.1.0-jdk8-SNAPSHOT` | [2.1.0-jdk8-preview-20261119](https://central.sonatype.com/artifact/cn.iocoder.agentscope/agentscope-bom/2.1.0-jdk8-preview-20261119) | 核心、基础与 DashScope starter 已通过 Boot 2.7/JDK 8 验证，其他扩展移植中 |

AgentScope 的现代制品由 Boot 3/4 共用，版本不带 JDK 后缀。当前分支：**`main`**。

Maven groupId 为 **`cn.iocoder.agentscope`**，artifactId 和 Java 包名沿用官方。同一个应用只选择一条版本线，同一项目的模块使用同一版本；替换已有依赖时，也要移除对应官方 groupId 下的同类模块，避免重复类。

Boot 3/4 共用的 JDK 17+ 预览版 [2.1.0-preview-20261119](https://central.sonatype.com/artifact/cn.iocoder.agentscope/agentscope-bom/2.1.0-preview-20261119) 已发布到 Maven Central。首批发布范围包括核心、基础 starter 和独立沙箱 SDK。Durable 开发组件、示例以及 Micronaut/Quarkus 扩展待后续发布。

## 如何使用

在 Boot 3/4 宿主中引入 BOM 和基础 starter。基础 starter 提供配置与装配入口，模型 provider、凭据及 Agent 行为仍需按需配置：

```xml
<properties>
    <yudao-agentscope.version>2.1.0-preview-20261119</yudao-agentscope.version>
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

Boot 2.7/JDK 8 分支的支持模块、构建与 HTTP 依赖配置见 [Java 8 使用说明](https://github.com/yudaocode/yudao-agentscope-java/blob/main-jdk8/JAVA8-COMPATIBILITY.md)。

独立沙箱 SDK 通过 `cn.iocoder.agentscope:agentscope-runtime-sandbox-core` 按需引入，版本由 AgentScope BOM 管理。Boot 2 使用 `2.1.0-jdk8-SNAPSHOT`，Boot 3/4 共用 `2.1.0-preview-20261119`；不需要引入整个 AgentScope core。使用兼容 SandboxService 管理服务，或配置本机 Docker 与所需沙箱镜像。源码来源、构建、请求超时及资源关闭说明见 [Runtime sandbox 使用说明](agentscope-runtime-sandbox-core/README_zh.md)。外部平台与具体工具服务需按实际环境配置，不能将本地兼容测试理解为所有外部后端均已验收。

### 从源码构建开发版

需要调试源码时，在 JDK 17 下安装开发版：

```bash
git clone -b main https://github.com/yudaocode/yudao-agentscope-java.git
cd yudao-agentscope-java
mvn -pl agentscope-dependencies-bom,agentscope-distribution/agentscope-bom,agentscope-extensions/agentscope-spring-boot-starters/agentscope-spring-boot-starter -am -DskipTests -Dmaven.javadoc.skip=true install
```

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
