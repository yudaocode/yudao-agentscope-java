# Native Boot2 Durable consumer

Install the Java8 Spring AI compatibility artifacts and matching AgentScope source reactor first.
The native Boot2.7.18 parent imports `cn.iocoder.agentscope:agentscope-bom:2.1.0-jdk8-SNAPSHOT`.
It selects the Durable JDBC and HTTP extensions; transitive runtime and Java8 helpers come from installed jars.
The POM explicitly aligns Jackson2.21.4 and HTTP5/Core5 versions.
With Maven/compiler on JDK17:

```sh
mvn -f compatibility-tests/boot2-durable/pom.xml -Dcompat.test.jvm=/absolute/path/to/jdk8/bin/java test
```

Two actual Java8 cases verify SQLite sessions and immutable files across Boot2 context restarts, unique installed runtime provisioning, and a loopback HTTP backend round trip.
Immutable object scopes use `DurableJson.hash(key)` as required by the protocol; arbitrary short strings are invalid.
The HTTP listener binds to127.0.0.1 with a test-only namespace authorizer and closes its executor.
The test uses temporary directories and no LLM, cloud account or remote database.
