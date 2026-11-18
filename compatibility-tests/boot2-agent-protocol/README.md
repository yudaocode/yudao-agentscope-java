# Agent Protocol on Boot 2.7 and Java 8

Install the Java 8 reactor first, then run the independent fixture with an actual JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-agent-protocol/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture starts a Boot 2.7 servlet host with the installed Agent Protocol library and
checks the response of its task lookup endpoint over loopback HTTP.
