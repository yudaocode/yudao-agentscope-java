# Spring Boot 2.7 / Java 8 使用说明

本分支基于上游提交 `30dc84b59e646f443dcf1870d66b3225e6210582` 进行源码兼容维护。
版本号 `2.0.0-SNAPSHOT` 表示本仓库的 Boot 2 版本线，不对应上游功能版本号。
这些坐标尚未发布 Maven Central 或远端 SNAPSHOT 仓库，需要先安装到本地 Maven 仓库。

## 支持范围

当前默认构建包含 11 个 Maven 模块，其中可供应用使用的库为：

| artifactId | 用途 |
| --- | --- |
| `agentscope-core` | Agent、工具、消息、状态、MCP 与 HTTP/WebSocket 传输 |
| `agentscope-spring-boot-starter` | Boot 2.7 基础自动配置 |
| `agentscope-extensions-model-dashscope` | DashScope 模型实现 |
| `agentscope-dashscope-spring-boot-starter` | DashScope 模型自动配置 |

其余模块是父 POM、聚合模块和 BOM。
Harness、Durable、其他模型与存储等扩展，以及 `agentscope-all` 尚未列入本分支默认构建和 BOM；源码保留，后续逐项适配。
独立的 AgentScope Runtime Sandbox SDK 也不属于本次支持范围。

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
    <yudao-agentscope.version>2.0.0-SNAPSHOT</yudao-agentscope.version>
    <!-- 继承 spring-boot-starter-parent 2.7 时需要覆盖这两个属性 -->
    <httpclient5.version>5.6.1</httpclient5.version>
    <httpcore5.version>5.4</httpcore5.version>
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
