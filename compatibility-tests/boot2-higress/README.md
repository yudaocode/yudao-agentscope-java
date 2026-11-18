# Higress on Spring Boot 2 and Java 8

Install the Java 8 reactor first, then run this independent project with a JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-higress/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The test uses the installed extension and a local MCP HTTP fixture. It exercises initialization, Higress tool search and tool execution using the Boot 2.7 dependency set.
