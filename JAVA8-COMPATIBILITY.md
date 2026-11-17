# Spring Boot 2.7 / Java 8 使用说明

本分支基于上游提交 `30dc84b59e646f443dcf1870d66b3225e6210582` 进行源码兼容维护。
版本号 `2.1.0-jdk8-SNAPSHOT` 保留上游 2.1.0 开发基线，以 jdk8 标识 Boot 2 兼容路线；正式版本追加日期，未发布上游基线增加 preview。
这些坐标尚未发布 Maven Central 或远端 SNAPSHOT 仓库，需要先安装到本地 Maven 仓库。

## 支持范围

当前默认构建包含 47 个 Maven 模块、35 个运行库，主要入口如下；完整运行库由兼容 BOM 管理：

| artifactId | 用途 |
| --- | --- |
| `agentscope-core` | Agent、工具、消息、状态、MCP 与 HTTP/WebSocket 传输 |
| `agentscope-spring-boot-starter` | Boot 2.7 基础自动配置 |
| `agentscope-extensions-model-dashscope` | DashScope 模型实现 |
| `agentscope-dashscope-spring-boot-starter` | DashScope 模型自动配置 |
| `agentscope-runtime-sandbox-core` | 独立 SandboxService / Docker 沙箱 SDK |

`agentscope-java8-compat` 自动提供 core 与 runtime-sandbox 共用的基础辅助类；无需在应用中单独配置。其余模块是父 POM、聚合模块和 BOM。
新增 OpenAI、Ollama、Anthropic、Gemini 四模型与对应 Boot2 starter。Anthropic SDK 需要 Kotlin1.9.24；继承 Boot2 parent 时设置 `<kotlin.version>1.9.24</kotlin.version>`，避免父 POM 压回旧 stdlib。

Harness、AG-UI、JDBC、MySQL、PostgreSQL、Redis、MongoDB 已列入默认构建和 BOM；Durable、Channels、RAG-simple、XXL-Job 等其余扩展及 `agentscope-all` 尚未纳入，源码保留。MongoDB 5.3.1 排除仅支持 Java 17 的可选原生 record codec，Java 8 使用 BSON/POJO codec。JSON schema 日期解析使用 Spring AI 的 ITU 兼容实现；可选 Kubernetes 后端尚未验收，其 SnakeYAML Engine 2.7 仍有新版 JDK API 引用，不属于 Java 8 已验证路径。
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

47 模块在真实 JDK 8 上执行完整默认 clean install：9180 项（按 XML 汇总，避免嵌套测试统计重复），零失败/错误、67 次条件跳过。包含日期解析与 Bouncy Castle Java8 链接回归。Harness 与 AG-UI 在本地测试；第三方模型响应使用本地模拟，真实付费模型、全部 Kubernetes/远程沙箱后端未验收。默认运行依赖排除了 MongoDB 的 Java17 record codec，但静态 API 审计仍能发现第三方可选现代实现；字节码扫描通过不等于每条可选路径兼容。

## Memory、RAG 与调度扩展

默认构建与BOM新增9个运行库：Mem0、百炼长期记忆、ReMe；百炼、Dify、Haystack、RAGFlow检索接入；Scheduler common、Quartz调度。按需添加同groupId的对应artifactId；Agent核心由应用一并引入。示例见各扩展目录和原有测试。Quartz采用Java8官方路线2.4.1（https://www.quartz-scheduler.org/downloads/）；继承Boot2 parent且使用Quartz扩展时，设置 `<quartz.version>2.4.1</quartz.version>` 以覆盖父POM直接管理的旧版本。测试使用本地模拟服务，未验收远端账号/模型。

Java8源码构建采用串行Maven（默认或 `-T1`），避免Jabel并发初始化时的附加代理竞态；业务运行时不需要Jabel。

百炼SDK传递的旧BC1.78.1在Java8的AES-GCM ByteBuffer调用中产生NoSuchMethodError；兼容BOM统一bcprov/bcpkix/bcutil-jdk18on为1.87，并以堆/直接缓冲区的认证加密往返测试验证。构建报告的字节码通过不能替代这类API链接验证。

## 对象存储与技能仓库

BOM现管理 `agentscope-extensions-oss`、`agentscope-extensions-cos`、`agentscope-extensions-skill-git-repository`、`agentscope-extensions-skill-mysql-repository`、`agentscope-extensions-skill-postgresql-repository`。按需添加对应artifactId，版本沿用本分支；对象存储扩展配合core/Harness，技能仓库配合core。服务端地址、凭据和数据库连接由应用提供，当前验证使用本地Git仓库、模拟对象存储客户端与JDBC测试。

Git扩展使用JGit `5.13.5.202508271544-r`，这是已核验的Java8维护路线；保持源码基线OSS3.18.5、COS5.6.245、MySQL9.6.0、PostgreSQL42.7.11。JGit7.x需要现代JDK，Java8分支保留现有技能仓库API并适配进度回调。参见[Eclipse5.13.5说明](https://projects.eclipse.org/projects/technology.jgit/releases/5.13.5)。
