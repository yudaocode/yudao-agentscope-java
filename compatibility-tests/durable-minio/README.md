# Durable MinIO 集成测试

测试通过已安装的 AgentScope BOM 使用 `agentscope-extensions-durable-objects`。它启动 Spring Boot 上下文，验证对 MinIO 的条件写入、重新创建客户端后的读取，以及损坏对象检测。测试会创建和删除自己的桶。

在仓库根目录用 JDK 17 安装所需制品，然后启动隔离的 Docker MinIO。

```bash
export JAVA_HOME=/path/to/jdk17
mvn -pl :agentscope-bom,:agentscope-extensions-durable-objects -am -DskipTests install
cd compatibility-tests/durable-minio

docker run -d --name durable-minio-it -p 127.0.0.1::9000 \
  --tmpfs /data:rw,size=3g \
  -e MINIO_ROOT_USER=durable_fixture -e MINIO_ROOT_PASSWORD=durable_local_only \
  minio/minio:RELEASE.2024-05-28T17-19-04Z server /data
endpoint="http://$(docker port durable-minio-it 9000/tcp)"
curl -fsS --retry 10 --retry-all-errors "$endpoint/minio/health/ready"
mvn clean test \
  -Dcompat.test.jvm=/path/to/jdk8/bin/java \
  -Ddurable.minio.boot=2.7.18 -Ddurable.minio.jdk=1.8 \
  -Ddurable.minio.endpoint="$endpoint" \
  -Ddurable.minio.access-key=durable_fixture -Ddurable.minio.secret-key=durable_local_only
docker rm -fv durable-minio-it
```
