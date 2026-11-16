# Runtime sandbox SDK

This module keeps the `io.agentscope.runtime.sandbox.*` API from
[agentscope-runtime-java](https://github.com/agentscope-ai/agentscope-runtime-java).
It is independent of AgentScope core and does not require a Spring application context.

| Branch | Artifact version | Runtime |
| --- | --- | --- |
| main | 3.0.0-SNAPSHOT | JDK17+, shared by Boot3/4 |
| main-jdk8 | 2.0.0-SNAPSHOT | JDK8+, for Boot2 |

Coordinates are `cn.iocoder.agentscope:agentscope-runtime-sandbox-core`.
Development artifacts must first be built from source; these coordinates have not
been published to Maven Central or a remote SNAPSHOT repository.

Build from the repository root with JDK17 and installed Maven3.9+:

```bash
mvn -pl agentscope-dependencies-bom,agentscope-distribution/agentscope-bom,agentscope-runtime-sandbox-core -am -DskipTests install
```

The Java8 branch also needs the matching `yudao-spring-ai` HTTP bridge installed
locally. Its small `agentscope-java8-compat` helper dependency shares utility
classes with core without depending on the entire core SDK. When inheriting a
Boot2 parent, follow the dependency overrides in
[JAVA8-COMPATIBILITY.md](../JAVA8-COMPATIBILITY.md), including
`httpclient5.version=5.6.1` and `httpcore5.version=5.4`.

Import the matching `cn.iocoder.agentscope:agentscope-bom`, then add:

```xml
<dependency>
    <groupId>cn.iocoder.agentscope</groupId>
    <artifactId>agentscope-runtime-sandbox-core</artifactId>
</dependency>
```

For a remote **SandboxService manager** supporting the upstream `/sandbox/*`
protocol, configure its URL and bearer token:

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

Imports remain `io.agentscope.runtime.sandbox.manager.*`,
`io.agentscope.runtime.sandbox.box.BaseSandbox` and `java.time.Duration`.
The manager URL is distinct from a sandbox container's `/tools/*` HTTP endpoint.
Remote mode does not initialize a local Docker client. For local container mode,
configure `ManagerConfig`'s container client, port range and filesystem as in the
upstream API; Docker and the requested sandbox image must be available.

`SandboxService.close()` cleans up sandboxes tracked by its configuration. Use
independent manager state for independently owned services. Close direct
`SandboxHttpClient`, `TrainingSandboxClient` and `RemoteHttpClient` instances with
try-with-resources. The latter's borrowed-client constructor cancels only its own
requests and preserves the shared HTTP client. Request timeouts include the whole
response body. Native JDK17 has no `HttpClient.close()` API: closing cancels tracked
requests and releases references; deterministic native-client close is available
on JDK21+ and through the Java8 bridge.

The opt-in `sandbox-docker-tests` Maven profile requires an explicit local
`DOCKER_HOST` and `SANDBOX_PROBE_IMAGE=sha256:...` for an already cached Alpine
fixture. Its temporary containers carry a per-test ownership label; the tests do
not pull images. This fixture tests the container lifecycle and is separate from
the official sandbox tool-service image.

## Source provenance

The vendored baseline is runtime1.0.2, tag commit
`08fdb10f321a99d73cd630d876003e5a92d56e9c`. The official sources JAR SHA256 is
`e021a689309810014fcc931feb6a8d4f1c86671b3cbaac0a5a4e53e3b4b19e1a`.
Original Apache2.0 headers, packages and author declarations are retained.
The fork adds Java8 lowering and HTTP/container ownership, cancellation and
deadline handling. The upstream `SandboxProvider` SPI descriptor is preserved.

Explicit AgentBay configuration is independent of local Docker availability. If Docker is unavailable, AgentBay remains usable and another start() retries local Docker without recreating AgentBay. Docker-only startup still fails and remains retryable.
