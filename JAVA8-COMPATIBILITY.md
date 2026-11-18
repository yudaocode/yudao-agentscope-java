# Spring Boot 2.7 / Java 8 使用说明

本分支基于上游提交 `30dc84b59e646f443dcf1870d66b3225e6210582` 进行源码兼容维护。
版本号 `2.1.0-jdk8-SNAPSHOT` 保留上游 2.1.0 开发基线，以 jdk8 标识 Boot 2 兼容路线；正式版本追加日期，未发布上游基线增加 preview。
这些坐标尚未发布 Maven Central 或远端 SNAPSHOT 仓库，需要先安装到本地 Maven 仓库。

## 支持范围

当前默认构建包含 92 个 Maven 模块、74 个运行库，主要入口如下；完整运行库由兼容 BOM 管理：

| artifactId | 用途 |
| --- | --- |
| `agentscope-core` | Agent、工具、消息、状态、MCP 与 HTTP/WebSocket 传输 |
| `agentscope-extensions-higress` | Higress MCP 客户端与网关工具搜索 |
| `agentscope-spring-boot-starter` | Boot 2.7 基础自动配置 |
| `agentscope-extensions-model-dashscope` | DashScope 模型实现 |
| `agentscope-dashscope-spring-boot-starter` | DashScope 模型自动配置 |
| `agentscope-runtime-sandbox-core` | 独立 SandboxService / Docker 沙箱 SDK |

`agentscope-java8-compat` 自动提供 core 与 runtime-sandbox 共用的基础辅助类；无需在应用中单独配置。其余模块是父 POM、聚合模块和 BOM。
新增 OpenAI、Ollama、Anthropic、Gemini 四模型与对应 Boot2 starter。Anthropic SDK 需要 Kotlin1.9.24；继承 Boot2 parent 时设置 `<kotlin.version>1.9.24</kotlin.version>`，避免父 POM 压回旧 stdlib。

Harness、AG-UI、JDBC、MySQL、PostgreSQL、Redis、MongoDB 已列入默认构建和 BOM；Durable、RAG-simple 等其余扩展及 `agentscope-all` 尚未纳入，源码保留。MongoDB 5.3.1 排除仅支持 Java 17 的可选原生 record codec，Java 8 使用 BSON/POJO codec。JSON schema 日期解析使用 Spring AI 的 ITU 兼容实现；Kubernetes 后端已完成 Java8 本地验证。Kubeconfig 解析与实际运行边界见下文。
独立沙箱模块的构建、配置与生命周期见 [Runtime sandbox 使用说明](agentscope-runtime-sandbox-core/README_zh.md)。

## 从源码安装

构建本仓库源码使用 JDK 17 和 Maven 3.9，生成 Java 8 字节码。
业务应用可以继续使用 JDK 8 编译、运行，不需要引入源码转换工具。

1. 先将 `yudao-spring-ai` 的 `main-jdk8` 分支安装到本地 Maven 仓库。
   本分支依赖其中的 MCP、Jackson 2 和 HTTP 兼容模块。
2. 在本仓库根目录运行：

```bash
# Maven 使用 JDK 17；JAVA8_HOME 指向实际的 JDK 8 安装目录
mvn -Djvm="$JAVA8_HOME/bin/java" clean install
```

`-Djvm` 指定测试实际使用的 JVM。
省略它时，测试通常使用 Maven 所在 JVM，不能据此判断已经在 JDK 8 上验证。

## Maven 配置

在 Boot 2.7 应用中导入 BOM，再按需选择一个 starter：

```xml
<properties>
    <yudao-agentscope.version>2.1.0-jdk8-SNAPSHOT</yudao-agentscope.version>
    <!-- 继承 spring-boot-starter-parent 2.7 时需要覆盖这两个属性 -->
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
        <artifactId>agentscope-dashscope-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Boot 2.7 parent 管理的旧 HTTP 组件会覆盖传递依赖的版本；上述两个属性确保 HTTP 客户端与 HttpCore 配套。
如果项目自行管理依赖、并非继承该 parent，应将兼容 BOM 放在 Boot BOM 前面，检查最终 HTTP 组件版本。
只需要基础自动配置时，将 starter 换为 `agentscope-spring-boot-starter`；只使用 Java API 时换为 `agentscope-core`。
替换已有官方依赖时，移除对应 `io.agentscope` 同类模块，避免重复类。

## 配置与调用

```yaml
agentscope:
  agent:
    enabled: true
  model:
    provider: dashscope
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    model-name: qwen-plus
```

注入 `io.agentscope.core.ReActAgent` 后，可调用 `agent.call("你好").block()`。
手动构建 Agent 时，通过 `.stateStore(store)` 配置上游新版 `AgentStateStore`，例如 `InMemoryAgentStateStore` 或 `JsonFileAgentStateStore`。

## 底层客户端差异

- `JdkHttpTransport` 的自定义客户端使用 `org.springframework.ai.compat.jdkhttp.HttpClient`。
- `JdkWebSocketTransport.create(...)` 的自定义客户端使用 `okhttp3.OkHttpClient`。
- HTTP/2 偏好交给底层库协商；明文 HTTP/1.1 服务可正常访问，不提供 JDK 客户端的 h2c Upgrade 行为。
- `JdkWebSocketTransport` 会按每次请求的 `connectTimeout` 限制完整握手；取消、超时或关闭传输层都会释放正在握手的连接。
- HTTP 正文工厂支持认证或 307/308 重定向后的重放；输入流 supplier 必须在每次调用时返回新的流。
- 客户端由应用传入时，传输层仅释放自身连接，不关闭应用持有的共享客户端或执行器。

这些差异集中在底层客户端定制入口；消息、模型与 Agent 的 Java 包名继续沿用 `io.agentscope.*`。

## 当前验证边界

52 模块在真实 JDK 8 上执行完整默认 clean install：9820 项（按 XML 汇总，避免嵌套测试统计重复），零失败/错误、69 次条件跳过。包含日期解析与 Bouncy Castle Java8 链接回归。Harness 与 AG-UI 在本地测试；第三方模型响应使用本地模拟，真实付费模型、全部 Kubernetes/远程沙箱后端未验收。默认运行依赖排除了 MongoDB 的 Java17 record codec，但静态 API 审计仍能发现第三方可选现代实现；字节码扫描通过不等于每条可选路径兼容。

## Memory、RAG 与调度扩展

默认构建与BOM新增9个运行库：Mem0、百炼长期记忆、ReMe；百炼、Dify、Haystack、RAGFlow检索接入；Scheduler common、Quartz调度。按需添加同groupId的对应artifactId；Agent核心由应用一并引入。示例见各扩展目录和原有测试。Quartz采用Java8官方路线2.4.1（https://www.quartz-scheduler.org/downloads/）；继承Boot2 parent且使用Quartz扩展时，设置 `<quartz.version>2.4.1</quartz.version>` 以覆盖父POM直接管理的旧版本。测试使用本地模拟服务，未验收远端账号/模型。

Java8源码构建采用串行Maven（默认或 `-T1`），避免Jabel并发初始化时的附加代理竞态；业务运行时不需要Jabel。

百炼SDK传递的旧BC1.78.1在Java8的AES-GCM ByteBuffer调用中产生NoSuchMethodError；兼容BOM统一bcprov/bcpkix/bcutil-jdk18on为1.87，并以堆/直接缓冲区的认证加密往返测试验证。构建报告的字节码通过不能替代这类API链接验证。

## 对象存储与技能仓库

BOM现管理 `agentscope-extensions-oss`、`agentscope-extensions-cos`、`agentscope-extensions-skill-git-repository`、`agentscope-extensions-skill-mysql-repository`、`agentscope-extensions-skill-postgresql-repository`。按需添加对应artifactId，版本沿用本分支；对象存储扩展配合core/Harness，技能仓库配合core。服务端地址、凭据和数据库连接由应用提供，当前验证使用本地Git仓库、模拟对象存储客户端与JDBC测试。

Git扩展使用JGit `5.13.5.202508271544-r`，这是已核验的Java8维护路线；保持源码基线OSS3.18.5、COS5.6.245、MySQL9.6.0、PostgreSQL42.7.11。JGit7.x需要现代JDK，Java8分支保留现有技能仓库API并适配进度回调。参见[Eclipse5.13.5说明](https://projects.eclipse.org/projects/technology.jgit/releases/5.13.5)。

## OpenAI Responses 与 Web starter

新增可选模块按需引用 `agentscope-openai-official-spring-boot-starter`、`agentscope-agui-spring-boot-starter`、`agentscope-chat-completions-web-starter`，坐标 groupId 与版本由上述 BOM 管理。OpenAI 官方 SDK 保留 4.99.0，配置 `agentscope.model.provider=openai-official`、`agentscope.openai-official.api-key` 和 `agentscope.openai-official.model-name`。AG-UI starter 需提供 `AguiAgentRegistry`，支持 MVC/WebFlux；Chat Completions starter 需提供 prototype 作用域的 `ReActAgent`，默认路径 `/v1/chat/completions`，可用 `agentscope.chat-completions.enabled=false` 关闭。详细配置见 [Java 8 使用说明](JAVA8-COMPATIBILITY.md)。

只使用 Java API 可按需引入 `agentscope-extensions-model-openai-official` 或 `agentscope-extensions-chat-completions-web`，无需 Web starter。

```yaml
agentscope:
  agent:
    enabled: true
  model:
    provider: openai-official
  openai-official:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4o
```

AG-UI 注册表类型为 `io.agentscope.core.agui.registry.AguiAgentRegistry`，具体包名与注册方式见该模块 README 和 starter 测试；引入注册表后自动匹配 MVC 或 WebFlux 栈，不应同时引入两套 Web 栈。Chat Completions Web starter 需要应用提供请求隔离的 prototype Agent，而非共享单例 Agent。

本批五个模块原有 640 项测试纳入上述完整构建。另新增 OpenAI 4.99.0 实际客户端的本地 HTTP 与 SSE 测试 2 项，在 JDK8 上通过，合计默认报告 9822 项、零失败/错误、69 条条件跳过；两条环境变量回退构建测试另以临时虚拟密钥运行通过，不发出远端请求。真实付费 OpenAI 服务未验收。运行依赖扫描 351 个 jar、144254 个基础类，均不高于 Java8 字节码；第三方可选路径的 API 限制仍按上文边界处理。

## XXL-Job 调度

按需引入 `cn.iocoder.agentscope:agentscope-extensions-scheduler-xxl-job`，版本由BOM管理；应用提供已经启动的 `com.xxl.job.core.executor.XxlJobExecutor`，再创建 `io.agentscope.extensions.scheduler.xxljob.XxlJobAgentScheduler`。XXL-Job SDK使用2.5.0，是本次Central元数据中较新的2.x发行版本（Java8字节码52）；官方3.x要求JDK17，3.3.2在真实Java8上已验证会报UnsupportedClassVersionError。参见[官方3.0版本说明](https://github.com/xuxueli/xxl-job/releases/tag/3.0.0)。

继承Boot2 parent且其直接管理XXL-Job时，设置 `<xxl-job.version>2.5.0</xxl-job.version>`；使用BOM导入时核对实际依赖树，避免旧SDK覆盖。保留AgentScope新版调度接口，仅将私有注册适配为2.5的 `registJobHandler`。原有35项测试及新增Groovy闭包运行测试1项全部在JDK8通过；没有启动真实远程调度中心。默认reactor由52扩到53模块，BOM由40扩到41运行库；前述52模块完整测试仍是独立历史检查点，本次单模块回归不混记为全53模块完整重跑。

运行闭包362jar /151009基础类均<=52；静态API审计新增Groovy9/10/16 VM插件引用，实际Java8的Groovy脚本执行已验证。可选现代插件本身不作为Java8路径提供。

## Nacos Prompt 与 Skill

Nacos Prompt/Skill 扩展按需引入 `agentscope-extensions-nacos-prompt`、`agentscope-extensions-nacos-skill`，同 groupId、版本由 BOM 管理。保留上游 Nacos Client `3.3.0`；应用提供 `com.alibaba.nacos.api.ai.AiService`，分别创建 `NacosPromptListener(aiService)`、`NacosSkillRepository(aiService, namespaceId)`。Prompt/Skill 原有 79 项测试在真实 JDK8 上通过；Nacos A2A 与 Nacos starter 尚未纳入本分支，远程 Nacos 服务需单独验收。

类全名为 `io.agentscope.core.nacos.prompt.NacosPromptListener`、`io.agentscope.core.nacos.skill.NacosSkillRepository`。提示词模块只需要小型Java8兼容库，不会因此强制依赖整个Agent核心；Skill仓库依赖Agent核心接口。SDK3.3.0保持不降版，测试包含模板、默认值、版本/label、Skill仓库行为，使用模拟AiService；不证明真实Nacos服务的认证、推送和网络行为。

默认reactor新增Nacos聚合器与两个库，56模块、43运行库；前述完整52检查点之后新增XXL36项和Nacos79项均为独立组件回归。运行闭包374jar/157413基础类<=52，API审计111项与XXL批相同，无新增必需JDK成员缺失。

## Jev / OpenAI Decisions 评判

Jev、OpenAI Decisions 评判扩展及两个 starter 也已纳入兼容 BOM：`agentscope-extensions-jev` / `agentscope-jev-spring-boot-starter`、`agentscope-extensions-openai-decisions` / `agentscope-openai-decisions-spring-boot-starter`。按需引入一套库或 starter；配置前缀分别为 `agentscope.jev`、`agentscope.openai-decisions`，API key 使用对应 `api-key` 属性，使用自己的服务时设置 `base-url`。可注入 `JevClient` / `OpenAIDecisionsClient`；未使用时不添加依赖，或设置对应 `enabled=false`。本批两个库与两个 starter 共 241 项 JDK8 测试通过（含本地真实 Chrome DOM 回归），远端付费评判服务未验收。

客户端类全名为 `io.agentscope.extensions.judge.jev.JevClient`、`io.agentscope.extensions.judge.openaidecisions.OpenAIDecisionsClient`。沿用上游 typed decision、上下文和中间件API；Jev浏览器适配需要显式选择Playwright依赖及本地浏览器可执行文件，不能把评判结果理解为绕过核心权限的依据。

默认两库单测Jev128（含1条浏览器条件跳过）、Decisions101，零失败/错误；浏览器用例随后以真实Java8+本机Chrome显式启用通过，因此最终去重报告128+101全部通过。两个新starter5+7项通过；修改starter父POM保留Spring配置处理器并加入Jabel后，全部11个已纳入starter共183项在真实Java8上重跑通过。只修复一处完全限定HexFormat调用以便既有桥接处理，不修改hash算法。

复现本地浏览器门禁（不连接付费模型）：

```bash
mvn -pl :agentscope-extensions-jev -Dtest=JevBrowserDriverTest \
  -Djev.browser="/path/to/chrome" -Djvm="$JAVA8_HOME/bin/java" test
```

该命令操作测试自己启动的127.0.0.1页面，不依赖用户已登录浏览器配置。新reactor61模块/47运行库；此前完整52模块门禁与后续组件门禁分别保留，不合并宣称全61一次完整重跑。

本批最终运行闭包381jar /158456基础类均<=52；静态API审计111项未增加，仍为已记录的第三方可选现代路径。

## Channels 渠道接入

Channels 扩展现已纳入 Java8 构建：`agentscope-extensions-channel-common` 与 `agentscope-extensions-channel-dingtalk` / `-feishu` / `-github` / `-gitlab` / `-wecom` / `-weixin`。同 groupId、版本由 BOM 管理；应用还需引入 `agentscope-harness`，使用 webhook/WebClient 的场景提供 Boot2 的 WebFlux 运行依赖并注册相应 Controller。通过对应 Channel 的 `fromProperties(channelId, routing, rawProperties)` 构建并交给 Harness Gateway；属性键遵循模块的 `*ChannelProperties`，如 GitHub 的 `token`/`webhookSecret`、GitLab 的 `token`/`webhookToken`。微信接入见[模块说明](agentscope-extensions/agentscope-extensions-channel/agentscope-extensions-channel-weixin/README.md)。

七个库共180项真实JDK8测试，零失败/错误、1项实服条件跳过；包括新增钉钉本地HTTP注册/WebSocket/ACK测试和GitHub/GitLab认证、事件分派、去重测试。未调用真实平台账号、未发送平台消息。

Java8渠道父POM管理Spring Framework5.3.39；Webhook Controller与WebFlux依赖为provided，应用需提供实际Web运行时。保持应用内Spring组件统一，不能混入Spring6/7。通过Gateway初始化Channel后再start，在应用关闭时stop。start可能解析平台机器人身份；真实账号接入由应用自行配置，测试仅使用本地模拟。

钉钉Stream复用已验证的AgentScope WebSocketTransport（Java8分支使用OkHttp后端），保持注册HTTP、ticket、回调/ACK与重连逻辑；stop会释放连接、握手、拥有的HTTP客户端与调度器，停止后不能重启同一实例。新增2项实际协议/生命周期回归；微信URLEncoder使用Java8的UTF-8名称重载。common仅依赖小型兼容库和可选Jackson注解；微信测试日志实现匹配SLF4J1.7。

原有GitHub/GitLab无测试，现各新增4项认证/分派/去重用例；两者不访问远端。七库计数common25、钉钉48、飞书12、GitHub4、GitLab4、企微16、微信71，其中微信实服1项条件跳过。运行闭包389jar/158554基础类均<=52，API111项与前批一致。支持范围现69模块/54运行库，验证仍为完整52检查点加后续逐批组件回归，不宣称一次完整69模块全跑。

## Complete69-module regression checkpoint

The entire current default69-module reactor passed a serial clean install on native Java1.8.0_472:10358 XML cases, zero failures/errors,71 conditional skips.
Every executed report records the actual Java8 VM; the fully disabled Docker sandbox class has no VM property and remains a skipped gap, not a successful runtime execution.
This gate includes the previously separate SDK/starter/XXL/Nacos/Judge/Channels batches, without simply summing old component runs.
Runtime dependency classpaths were regenerated after clean:69 files,389jars,158554 effective base classes, none above major52.
The independent native8 static member audit retains111 reviewed optional third-party references; it is not silently converted into a zero-finding claim.
Live platform credentials, the excluded27 library/extension/application directories and all sandbox/Kubernetes backends are not covered by this gate.

## Durable Java8 modules

Default scope expands69→82 modules,65 runtime libraries.
The compatibility BOM now includes runtime/worker, Harness/Claude/AG-UI adapters and JDBC/HTTP/sandbox/RocketMQ/objects/MCP extensions.
They retain upstream APIs and state/protocol formats; Java8 business code uses ordinary types instead of var/record/List.of syntax.
The runtime depends on the independent Java8 and ITU compatibility libraries, not on AgentScope core.
JDK9+ logging, bounded stream reads, HttpExchange resource closing and test-only classloader/process calls use Java8 equivalents without weakening assertions.

Select `agentscope-extensions-durable-jdbc` for `LocalBackend.open(Paths.get("./data/durable"))` (SQLite plus immutable files), and `-http` for the HTTP backend/client.
The caller owns backend/runtime/executor lifetimes and configures the HTTP authorizer.
Immutable object scope values are64-character hashes; use the protocol's scope/hash helpers.
MinIO8.6.0 and OSS3.18.5 remain available with the existing Java8 OkHttp4.12 route; their SDK dependencies are optional and consumers select them explicitly.
Durable remains an upstream experimental API and its deployment skip setting is retained; these modules currently require source installation.
See [the Boot2 consumer](compatibility-tests/boot2-durable) for native parent/BOM/version alignment and code.

All11 added runtime modules passed366 native8 component tests, zero failures/errors/skips, including actual separate-JVM recovery, local HTTP and process sandbox contracts.
A complete fresh82-module serial clean install then passed10724 cases on Java1.8.0_472, zero failures/errors,71 conditional skips.
The separate native Boot2 consumer passed2 cases: persistence/object integrity across context restarts and loopback HTTP.
This does not claim real S3/OSS/RocketMQ/remote MCP accounts or every sandbox backend.
After clean, runtime scope82 classpaths/409jars/170254 effective base classes contains no major>52; the static API audit retains111 reviewed third-party findings, with no new missing member signature compared with the69-module checkpoint.

## E2B backend on Java 8

Add these dependencies under the compatibility BOM. Harness is intentionally `provided` by the E2B backend, so applications must include it explicitly:

```xml
<dependency>
    <groupId>cn.iocoder.agentscope</groupId>
    <artifactId>agentscope-extensions-sandbox-e2b</artifactId>
</dependency>
<dependency>
    <groupId>cn.iocoder.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
</dependency>
```

Use the existing public API:

```java
E2bSandboxClientOptions options = new E2bSandboxClientOptions();
options.setApiKey(System.getenv("E2B_API_KEY"));
options.setTemplateId("base");
E2bSandboxClient client = new E2bSandboxClient(options, null);
try (Sandbox sandbox = client.create(new WorkspaceSpec(), null, options)) {
    sandbox.start();
    // Bind the sandbox to your Harness workflow using the upstream API.
}
```

The example types are in `io.agentscope.extensions.sandbox.e2b` and `io.agentscope.harness.agent.sandbox`. Real execution requires your E2B account, template and endpoint configuration. The Java8 port retains the original protocol and limits, replacing only newer JDK stream APIs with the existing Java8 compatibility helpers. Protobuf4.35.0 and OkHttp4.12.0 are retained.

When inheriting Boot2.7.18's parent, use the existing Jackson/HTTP/Logback overrides plus `okhttp.version=4.12.0`, `reactor-bom.version=2024.0.18`, and `commons-lang3.version=3.20.0`; a BOM import cannot replace inherited direct management. The complete independent consumer POM is [boot2-e2b](compatibility-tests/boot2-e2b/pom.xml).

After installing the default reactor with JDK17 build tools and native8 tests, run:

```sh
./mvnw -f compatibility-tests/boot2-e2b/pom.xml verify \
  -Dcompat.test.jvm="$JAVA8_HOME/bin/java"
```

No E2B account is used by that fixture: it checks the installed jar, Boot2 context/JSON setup, a real local platform HTTP request with dummy credentials, and the bounded snapshot reader. Upstream live-cloud tests remain conditional and are reported as skipped when `E2B_API_KEY` is absent. Kubernetes, AgentRun and Daytona remain outside this Java8 build; this port does not certify those backends.


The E2B component gate passed196 native8 executions, zero failures/errors,9 upstream live-cloud skips. The complete84-module serial clean install passed10920 executions on actual Java1.8.0_472, zero failures/errors,80 conditional skips. The fully disabled Docker sandbox class remains a skipped gap. An independent Boot2 parent consumer passed3 native8 cases with zero skips.

Runtime scan including the standalone consumer:85 classpath files,413 jars,170535 effective base classes, none above Java8 major52. The static API report keeps111 reviewed third-party findings; its missing member/class signature set is unchanged from the82-module checkpoint. All sandbox backends and live E2B acceptance remain outside this result.

## Higress MCP on Java 8

Boot 2.7 项目导入本分支的 `agentscope-bom` 后，按需添加：

```xml
<dependency>
    <groupId>cn.iocoder.agentscope</groupId>
    <artifactId>agentscope-extensions-higress</artifactId>
</dependency>
```

先按[从源码安装](#从源码安装)安装兼容版 Spring AI 与 AgentScope。使用自己的网关 MCP 地址和认证方式配置客户端。支持 `streamableHttpEndpoint`、`sseEndpoint` 和可选的 `toolSearch(query, topK)`。

```java
HigressMcpClientWrapper client = HigressMcpClientBuilder.create("higress")
        .streamableHttpEndpoint(gatewayEndpoint)
        .toolSearch("查询天气", 5)
        .buildAsync()
        .block(Duration.ofSeconds(10));
try {
    List<McpSchema.Tool> tools = client.listTools().block(Duration.ofSeconds(10));
    // 从网关返回的工具中选择要调用的工具
} finally {
    client.close();
}
```

[独立 Boot 2.7 消费验证](compatibility-tests/boot2-higress)使用安装后的扩展和本地 MCP HTTP 服务，检查初始化、自定义请求头与查询参数、工具搜索和 UTF-8 工具调用。真实 Higress 网关需要按部署环境验收。

## AgentRun and Daytona on Java 8

先按[从源码安装](#从源码安装)准备兼容版 Spring AI 和 AgentScope。在 Boot 2.7 应用导入兼容 BOM，显式引用 `agentscope-harness`，再按需选择后端。

| artifactId | Harness 文件系统配置 | 所需配置 |
| --- | --- | --- |
| `agentscope-extensions-sandbox-agentrun` | `AgentRunFilesystemSpec` | API key、模板、MCP 地址，以及账号和区域或自定义数据面地址 |
| `agentscope-extensions-sandbox-daytona` | `DaytonaFilesystemSpec` | API key、控制面和工具端点、快照或镜像 |

这些依赖的 groupId 为 `cn.iocoder.agentscope`。将对应的 FilesystemSpec 传给 `HarnessAgent.builder().filesystem(...)`，并使用应用自己的凭据配置。

[AgentRun Boot2 消费验证](compatibility-tests/boot2-agentrun)覆盖状态持久化/恢复和释放时的数据面认证请求；[Daytona Boot2 消费验证](compatibility-tests/boot2-daytona)覆盖创建、就绪、工作区初始化和释放。两项经本地 HTTP 服务验证；真实云环境按部署验收。

## Studio, Admin and Agent Protocol on Java 8

导入兼容版 `agentscope-bom` 后，按需引用相应模块（groupId 均为 `cn.iocoder.agentscope`）：

| 模块 | Boot 2.7 接入方式 |
| --- | --- |
| `agentscope-extensions-studio` | 另引 `agentscope-core`。通过 `StudioConfig.builder().studioUrl(...)` 配置 Studio，使用 `StudioClient` 注册和上报。 |
| `agentscope-admin-spring-boot-starter` | 另引 `agentscope-harness` 及需要的 Boot Web/Actuator starter。设置 `agentscope.admin.enabled=true`。按需添加 `org.springdoc:springdoc-openapi-ui:1.8.0`，管理接口文档在 `/v3/api-docs/agentscope-admin`。 |
| `agentscope-extensions-agent-protocol` | 在 Boot Servlet 宿主启用 `agentscope.agent-protocol.enabled=true`，提供 `AgentFactory` 或 `HarnessAgent`，按部署配置 `agentscope.agent-protocol.task-store-path`。 |

独立 Boot2 消费分别见 [Studio](compatibility-tests/boot2-studio)、[Admin](compatibility-tests/boot2-agent-admin) 和 [Agent Protocol](compatibility-tests/boot2-agent-protocol)。Studio 使用本地 HTTP 服务验证注册、进程号和消息等请求；Admin 与 Agent Protocol 使用实际 Boot2 Servlet 宿主验证 HTTP 接口。

## Kubernetes and Training on Java 8

在 Boot 2.7 应用中导入兼容的 `agentscope-bom`，再按需选择下列模块。groupId 均为 `cn.iocoder.agentscope`。

| artifactId | 应用接入 |
| --- | --- |
| `agentscope-extensions-sandbox-kubernetes` | 显式添加 `agentscope-harness`。通过 `KubernetesFilesystemSpec` 配置应用控制的 Kubernetes 环境，交给 `HarnessAgent.builder().filesystem(...)`。构建前安装含空响应体跳转修复的 Spring AI Java8 HTTP 兼容库。 |
| `agentscope-extensions-training` | 使用扩展的 `TrainingConfig`、`TrainingRunner` 或 `TrinityClient`，按应用配置训练端点。 |

[Kubernetes Boot2 安装包测试](compatibility-tests/boot2-kubernetes)覆盖 Fabric8 6.14.0 的 Kubeconfig 解析和本地沙箱网关空响应体跳转。
[Training Boot2 安装包测试](compatibility-tests/boot2-training)覆盖本地 Trinity HTTP 请求。
实际 Kubernetes 集群与真实训练服务按部署环境验收。
