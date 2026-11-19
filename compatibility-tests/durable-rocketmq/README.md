# Durable RocketMQ 集成测试

这个独立 Maven 项目通过本地安装的 AgentScope BOM 消费 Durable 扩展。测试在宿主机启动 Spring Boot Brain 和独立 worker JVM，使用 Docker 中的 RocketMQ 5.3.2（走 gRPC Proxy）和 PostgreSQL 17。模型使用固定响应，工具用文件记录实际执行。

- 正常执行验证命令投递、结果回传和一次应用。
- 故障注入在工具写入副作用后强杀原 worker。接替进程使用同一份 PostgreSQL worker 账本，对同一 execution ID 查询恢复。测试断言工具只执行一次，且 Brain 只应用一次工具结果。

先用 JDK 17 在仓库根目录安装所需的 AgentScope 开发制品。Boot 2 分支另需按仓库说明安装对应的 Spring AI 兼容制品。

```bash
export JAVA_HOME=/path/to/jdk17
mvn -pl :agentscope-bom,:agentscope-extensions-durable-rocketmq,:agentscope-extensions-durable-jdbc,:agentscope-durable-adapter-harness-agentscope \
  -am -DskipTests install
```

然后进入本目录，启动测试容器。每次执行请使用新的短命名空间，避免读取上一次测试的消息。

```bash
cd compatibility-tests/durable-rocketmq
docker compose -p durable-mq-it up -d --wait
ns=durable-it-$(date +%s)
for mode in normal crash-after-effect; do
  for topic in commands controls updates; do
    docker compose -p durable-mq-it exec -T broker sh mqadmin updateTopic \
      -n nameserver:9876 -c DefaultCluster -t "$ns-$mode-hands-$topic" -r 2 -w 2
  done
  for group in brain worker worker-controls; do
    docker compose -p durable-mq-it exec -T broker sh mqadmin updateSubGroup \
      -n nameserver:9876 -c DefaultCluster -g "$ns-$mode-hands-$group"
  done
done

mq=$(docker compose -p durable-mq-it port proxy 18081)
pg=$(docker compose -p durable-mq-it port postgres 5432)
mvn clean test \
  -Dcompat.test.jvm=/path/to/jdk8/bin/java \
  -Ddurable.mq.boot=2.7.18 -Ddurable.mq.jdk=1.8 \
  -Ddurable.mq.endpoint="$mq" -Ddurable.mq.namespace="$ns" \
  "-Ddurable.mq.jdbc=jdbc:postgresql://$pg/durable?user=durable&password=local_fixture"

docker compose -p durable-mq-it down -v
```

测试在 Brain 和 worker 内分别校验实际的 Spring Boot 和 JVM 版本。Broker 使用隔离的临时存储，本测试的故障注入只针对 worker 进程。
