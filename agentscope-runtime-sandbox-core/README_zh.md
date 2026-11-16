# Runtime sandbox SDK

本模块保留 [agentscope-runtime-java](https://github.com/agentscope-ai/agentscope-runtime-java)
的 `io.agentscope.runtime.sandbox.*` API，可独立于 AgentScope core 和 Spring 上下文使用。

| 分支 | 制品版本 | 运行环境 |
| --- | --- | --- |
| main | 3.0.0-SNAPSHOT | JDK17+，Boot3/4 共用 |
| main-jdk8 | 2.0.0-SNAPSHOT | JDK8+，用于 Boot2 |

坐标为 `cn.iocoder.agentscope:agentscope-runtime-sandbox-core`。目前仅提供源码开发版，
尚未发布到 Maven Central 或远端 SNAPSHOT 仓库，需先安装到本地。

在仓库根目录使用 JDK17、已安装的 Maven3.9+ 构建：

```bash
mvn -pl agentscope-dependencies-bom,agentscope-distribution/agentscope-bom,agentscope-runtime-sandbox-core -am -DskipTests install
```

Java8 分支还需先安装同版本线的 yudao-spring-ai HTTP 桥接器。
`agentscope-java8-compat` 小模块与 core 共用辅助类，无需引入整个 core。
继承 Boot2 parent 时，按 [Java8 branch guide](https://github.com/yudaocode/yudao-agentscope-java/blob/main-jdk8/JAVA8-COMPATIBILITY.md)
覆盖依赖版本，包括 `httpclient5.version=5.6.1`、`httpcore5.version=5.4`。

导入匹配版本的 `cn.iocoder.agentscope:agentscope-bom` 后引入：

```xml
<dependency>
    <groupId>cn.iocoder.agentscope</groupId>
    <artifactId>agentscope-runtime-sandbox-core</artifactId>
</dependency>
```

远端模式连接的是支持上游 `/sandbox/*` 协议的 **SandboxService 管理服务**：

```java
ManagerConfig config = ManagerConfig.builder()
        .baseUrl(System.getenv("SANDBOX_MANAGER_URL"))
        .bearerToken(System.getenv("SANDBOX_MANAGER_TOKEN"))
        .remoteConnectTimeout(Duration.ofSeconds(30))
        .remoteRequestTimeout(Duration.ofMinutes(5))
        .build();
try (SandboxService service = new SandboxService(config)) {
    service.start();
    BaseSandbox sandbox = new BaseSandbox(service, "user-id", "session-id");
    String result = sandbox.runShellCommand("printf 'hello'");
}
```

导入 `io.agentscope.runtime.sandbox.manager.*`、
`io.agentscope.runtime.sandbox.box.BaseSandbox`、`java.time.Duration`。
管理服务地址与沙箱容器自己的 `/tools/*` 地址不同。远端模式不会初始化本机 Docker。
本地模式按上游 API 配置容器客户端、端口范围和文件系统，并准备 Docker 与所需沙箱镜像。

`SandboxService.close()` 清理其配置中记录的沙箱；独立拥有的服务使用独立管理状态。
直接创建的 `SandboxHttpClient`、`TrainingSandboxClient`、`RemoteHttpClient` 用
try-with-resources 关闭。RemoteHttpClient 借用共享客户端时仅取消自己的请求，不关闭共享客户端。
请求超时覆盖完整正文。原生 JDK17 没有 HttpClient.close：关闭时取消请求、释放引用；
JDK21+ 和 Java8 桥接器可确定性关闭底层客户端。

`sandbox-docker-tests` 为显式开启的 Maven profile，需要本机 `DOCKER_HOST` 和
已缓存 Alpine 镜像的 `SANDBOX_PROBE_IMAGE=sha256:...`。测试仅创建带独立所有权标签的
临时容器，不拉镜像；该夹具验证容器生命周期，与官方沙箱工具服务镜像分开。

## 源码来源

基于 runtime1.0.2，tag 提交 `08fdb10f321a99d73cd630d876003e5a92d56e9c`。
官方 sources JAR SHA256：
`e021a689309810014fcc931feb6a8d4f1c86671b3cbaac0a5a4e53e3b4b19e1a`。
保留原 Apache2.0 文件头、包名和作者，补充 Java8 转换、HTTP/容器资源所有权、取消与截止时间处理。
保留上游 SandboxProvider 的 SPI 描述文件。

显式配置 AgentBay 时，其初始化独立于本地 Docker。Docker 不可用时 AgentBay 仍可使用，再次调用 start() 会重试本地 Docker，并复用已初始化的 AgentBay。仅使用 Docker 时，连接失败仍会报错并允许重试。
