# Daytona on Spring Boot 2 and Java 8

Install the Java 8 reactor first, then run the standalone project with a JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-daytona/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The project reuses the module's lifecycle test with the installed artifacts and Boot 2.7 dependency management. It needs no Daytona account.
