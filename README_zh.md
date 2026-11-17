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

[**English Homepage**](https://github.com/agentscope-ai/agentscope-java/blob/main/README.md) | [**日本語ホームページ**](https://github.com/agentscope-ai/agentscope-java/blob/main/README_ja.md) | [**文档**](https://java.agentscope.io/dev/zh/intro.html)

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

## 什么是 AgentScope Java 2.0？

AgentScope Java 2.0 是面向企业级、分布式、生产环境的智能体框架，提供与模型能力相匹配的核心 Harness 抽象，可支持长期、稳定、安全可控的智能体任务执行。

- [**事件系统** →](https://java.agentscope.io/dev/zh/docs/building-blocks/message-and-event.html) 统一的事件流，32 种类型化事件，服务于前端实时渲染与 human-in-the-loop。
- [**权限系统** →](https://java.agentscope.io/dev/zh/docs/building-blocks/permission-system.html) 工具调用决策机制：允许 / 用户审批 / 拒绝。
- [**Middleware** →](https://java.agentscope.io/dev/zh/docs/building-blocks/middleware.html) 基于 AOP 模式的 hook 事件拦截机制，灵活扩展推理-行动循环。
- [**Workspace 与沙箱** →](https://java.agentscope.io/dev/zh/docs/harness/workspace.html) 在隔离环境中执行工具 —— 本地、Docker、Kubernetes 或 AgentRun 云沙箱。
- [**多智能体编排** →](https://java.agentscope.io/dev/zh/docs/harness/subagent.html) 多种子智能体定义模式，`agent_spawn` / `agent_send` + 事件流实时转发。
- [**分布式部署** →](https://java.agentscope.io/dev/zh/docs/others/going-to-production.html) 真正的分布式 session 与 memory 管理（Redis / MySQL / PostgreSQL / OSS / COS），跨副本 session 恢复。
- [**智能体进化** →](https://help.aliyun.com/zh/document_detail/3042583.html) 提供 Agent 全栈观测与审计、Agent 评估与实验、Agent 资产管理与持续优化等能力。

<img src="./docs/imgs/landscape.png" alt="agentscope" width="100%"/>

## 新闻
<!-- BEGIN NEWS -->
- **[2026-07] `v2.0.0 GA`：** 首个正式版本发布！双层 Agent 架构、事件流、权限系统、Middleware、Workspace 沙箱、多智能体编排、分布式部署全面就绪。 [文档](https://java.agentscope.io/) | [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0)
- **[2026-07] `v2.0.0-RC5`：** 模型提供商模块化拆分；统一 DataBlock 多模态支持；原生结构化输出；Channel IM 平台接入；腾讯云 COS 状态持久化。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC5)
- **[2026-06] `v2.0.0-RC4`：** 异步工具执行与定时唤醒调度；子 agent 跨副本路由与 session 恢复。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC4)
- **[2026-06] `v2.0.0-RC3`：** `call()` / `streamEvents()` 共享执行核心；`AgentResultEvent`、`CustomEvent`、`HintBlockEvent` 新事件类型。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC3)
- **[2026-06] `v2.0.0-RC2`：** Agent 完全无状态改造；DistributedBackend 一键配置；子 agent 事件流转发；Channel 模块（钉钉、飞书、企业微信）；A2A / AG-UI 协议支持。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC2)
- **[2026-05] `v2.0.0-RC1`：** 首个 2.0 RC 版本，包含 Harness 工程化、企业级分布式部署、事件流 / 消息模型 / Middleware / HITL 全面重构。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.0-RC1)
- **[2026-05] `v1.1.0`：** 引入 `agentscope-harness` 与 `HarnessAgent`；Workspace 工作区（人格 / 记忆 / 技能 / 子 agent）；可插拔文件系统（本地 / 共享存储 / 沙箱）；Session 持久化与跨进程恢复；分层记忆与上下文压缩；声明式子 agent 编排。 [Release Notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v1.1.0-RC1)
<!-- END NEWS -->

## 社区

欢迎加入我们的社区

| [Discord](https://discord.gg/eYMpfnkG8h)                     | 钉钉                                                                    | 微信                                                           |
|--------------------------------------------------------------|-----------------------------------------------------------------------|--------------------------------------------------------------|
| <img src="./docs/imgs/discord.png" width="100" height="100"> | <img src="./docs/imgs/dingtalk_qr_code.png" width="100" height="100"> | <img src="./docs/imgs/wechat.jpeg" width="100" height="100"> |

## 快速开始

### 安装

> AgentScope Java 需要 **JDK 17** 及以上版本。

#### Maven

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>2.0.3</version>
</dependency>
```

2.0 中模型提供商已拆分为独立扩展模块，需额外引入所需的模型依赖。例如使用 DashScope：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-model-dashscope</artifactId>
    <version>2.0.3</version>
</dependency>
```

其他可选：`agentscope-extensions-model-openai`、`agentscope-extensions-model-anthropic`、`agentscope-extensions-model-gemini`、`agentscope-extensions-model-ollama`。详见[模型文档](https://java.agentscope.io/dev/zh/docs/building-blocks/model.html)。

文本和图片的类型化分类可使用 [OpenAI Decisions](docs/v2/zh/integration/judge/openai-decisions.md)，需要包含该扩展的 `2.1.0-SNAPSHOT` 构建。

如果希望直接使用 `ReActAgent` 的推理和工具 API、自行组合工程能力，可依赖 `agentscope-core` 和所需模型扩展。

## Hello AgentScope!

推荐在应用启动时定义共享 Builder，每次请求用 `builder.build()` 创建新 Agent，执行结束后关闭。新实例通过相同的身份和持久化配置延续会话。运行前设置 `DASHSCOPE_API_KEY`：

```java
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Paths;

public class FirstAgent {
    // 应用启动时配置共享 Builder；请求期间只调用 build()。
    private static final HarnessAgent.Builder AGENT_BUILDER = HarnessAgent.builder()
            .name("assistant")
            .agentId("assistant")
            .sysPrompt("你是一个有用的 AI 助手。")
            .model("dashscope:qwen-plus")
            .workspace(Paths.get(".agentscope/workspace"));

    public static void main(String[] args) {
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId("demo").userId("alice").build();
            agent.call(new UserMessage("你好！"), ctx).block();
        }

        // COMMENT_帮我把今天的关键点列三条。
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId("demo").userId("alice").build();
            agent.streamEvents(new UserMessage("帮我把今天的关键点列三条。"), ctx)
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

普通问答、多轮对话和工作流节点从 `call` / `streamEvents` 开始，详见[快速开始](https://java.agentscope.io/dev/zh/docs/quickstart)。两者默认也会记录 Harness 会话历史。需要关闭页面后继续执行、任务排队或中断后续做时，再引入 [AgentSession](https://java.agentscope.io/dev/zh/docs/harness/session-log)；[可恢复聊天示例](./agentscope-examples/agents/agentscope-chat/README_zh.md)提供完整接入流程。

## 核心设计

AgentScope Java 2.0 从"构建一个智能体"的工具箱，迈向**面向生产环境运行智能体**的完整平台。升级围绕三大主题展开：

### 1 · Harness 工程化 —— 长期运行、复杂任务的工程底座

裸的 ReAct 循环只解决"一次推理"。**HarnessAgent** 在 ReActAgent 之上通过 Middleware 与 Toolkit 叠加工程基础设施，核心推理循环原样保留，能力按需叠加：

- **自进化与技能仓库** —— 成功模式自动沉淀为 Markdown 技能，跨会话共享、按需加载
- **分层记忆** —— 上下文对话 + agent 自维护的 `MEMORY.md` + 磁盘事实流水账，自动压缩控制 prompt 体量
- **子智能体** —— Markdown 声明子 agent 规格，`agent_spawn` / `agent_send` 按需启动，同步与后台委派两种模式
- **上下文自动管理** —— 结构化压缩保留目标 / 状态 / 关键发现 / 下一步；超大工具结果落盘只留占位符
- **Plan Mode** —— 只读规划态编排长任务，计划文件持久化并驱动执行

### 2 · 企业级分布式部署

生产环境的智能体要服务多租户、安全执行不可信代码、滚动发布不丢上下文。AgentScope 2.0 天然面向无状态水平扩展：

- **多租户隔离** —— `session` / `user` / `agent` / `org` 多维度状态隔离，`RuntimeContext` 键贯穿工作区、KV 命名空间、沙箱状态槽
- **安全沙箱** —— 本地子进程 / Docker / Kubernetes / E2B 云沙箱任选，支持快照恢复
- **权限管控** —— 三态决策（允许 / 审批 / 拒绝），敏感操作强制 HITL 审批
- **会话恢复** —— `AgentStateStore`（内存 / JSON 文件 / MySQL / Redis / PostgreSQL）支撑零停机滚动发布

### 3 · 底层框架升级 —— 更轻、更顺手的核心抽象

消息、事件、扩展机制更小、更正交；HITL 与事件流式是框架运行的一部分，不是外挂层：

- **事件流** —— 32 种类型化事件，模型调用、文本增量、工具执行、用户确认全部实时流出
- **消息模型** —— 文本 / 文件 / 图片 / 音视频 / 工具结果统一收敛到 `ContentBlock`，按 role 严格校验
- **Middleware** —— `onAgent` / `onReasoning` / `onActing` / `onModelCall` / `onSystemPrompt` / `onAgentStateReady` 六阶段取代 v1 扁平 hook
- **HITL 一等公民** —— 确认工具参数、审批敏感操作、交给外部系统执行，智能体在暂停点精确恢复

完整架构概览请参阅[文档](https://java.agentscope.io/dev/zh/docs/index.html)。

## 贡献

我们欢迎社区的贡献！请参阅 [CONTRIBUTING_zh.md](./CONTRIBUTING_zh.md) 了解详情。

## 许可

AgentScope 基于 Apache License 2.0 发布。


## 贡献者

感谢所有贡献者：

<a href="https://github.com/agentscope-ai/agentscope-java/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=agentscope-ai/agentscope-java&max=999&columns=12&anon=1" />
</a>
