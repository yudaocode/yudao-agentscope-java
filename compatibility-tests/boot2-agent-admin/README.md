# AgentScope Admin starter on Boot 2.7 and Java 8

Install the Java 8 reactor first, then run the independent fixture with an actual JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-agent-admin/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture starts a real Boot 2.7 servlet host, then requests Actuator health and the
SpringDoc group for the installed Admin starter.
