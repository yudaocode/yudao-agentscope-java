# AgentRun on Spring Boot 2 and Java 8

Install the Java 8 reactor first, then run the independent fixture with an actual JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-agentrun/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture uses the installed extension and a local HTTP endpoint to validate state restoration and the authenticated data-plane shutdown request. It does not need a cloud account.
