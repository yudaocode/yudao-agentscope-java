# Ollama starter 接入和本地复验

在应用中导入对应分支的 `cn.iocoder.agentscope:agentscope-bom`，引入 `cn.iocoder.agentscope:agentscope-ollama-spring-boot-starter`。用下面的配置让 Boot 创建 `io.agentscope.core.model.Model`。

```yaml
agentscope:
  model:
    provider: ollama
  ollama:
    base-url: http://127.0.0.1:11434
    model-name: smollm2-compat:135m
```

采用 Spring Boot 2.7 的 Maven parent 时，设置 `<httpclient5.version>5.6.5</httpclient5.version>` 和 `<httpcore5.version>5.4.4</httpcore5.version>`，与 AgentScope 的 Java 8 路线对齐。Boot 3.5.15 和 4.1.2 使用各自默认 BOM。

准备 Ollama 服务和一个可用于聊天的本地模型。复验使用 SmolLM2 135M Q4_K_M GGUF，可用以下 Modelfile 导入。Docker 环境把模型文件挂载到容器，再从容器内执行 `ollama create smollm2-compat:135m -f /path/to/Modelfile`。

```text
FROM /path/to/smollm2.gguf
PARAMETER temperature 0
PARAMETER num_thread 2
```

从仓库根目录用 JDK 17 启动 Maven。先安装当前分支的 BOM 和 starter，再运行独立工程。用例检查实际 Spring Boot 和 JVM 版本。设置 `BOOT_VERSION` 为 2.7.18、3.5.15 或 4.1.2，`TEST_JAVA_SPEC` 为 `1.8`、`17` 或 `25`，`TEST_JAVA_HOME` 指向目标 JDK。

```bash
mvn -pl :agentscope-bom,:agentscope-ollama-spring-boot-starter -am \
  -DskipTests -Dmaven.javadoc.skip=true -Dmaven.deploy.skip=true install

mvn -f compatibility-tests/ollama-starter-chat/pom.xml clean test \
  -Dboot.version="$BOOT_VERSION" \
  -Dcompat.test.jvm="$TEST_JAVA_HOME/bin/java" \
  -Dcompat.java.version="$TEST_JAVA_SPEC" \
  -Dollama.base-url=http://127.0.0.1:11434 \
  -Dollama.model=smollm2-compat:135m
```

`main-jdk8` 固定使用 Boot 2.7.18。`main` 同一份已安装 starter JAR 可在 Boot 3.5.15 和 4.1.2 下复验。
