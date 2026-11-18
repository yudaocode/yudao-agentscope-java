# AgentScope Training on Boot 2.7 and Java 8

Install the Java 8 reactor first, then run the independent fixture with a JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-training/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture runs the module's Trinity client tests against installed artifacts with Boot 2.7
dependency management. HTTP requests are handled by a loopback server.
