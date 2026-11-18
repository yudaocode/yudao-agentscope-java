# AgentScope Studio on Boot 2.7 and Java 8

Install the Java 8 reactor first, then run the independent fixture with a JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-studio/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture reuses the module's HTTP client tests against the installed artifacts and Boot
2.7 dependency management. It uses a loopback server for registration and messaging.
