# Kubernetes sandbox on Boot 2.7 and Java 8

Install the matching Spring AI Java 8 HTTP compatibility module and AgentScope reactor first.

```bash
mvn -f compatibility-tests/boot2-kubernetes/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture checks Kubernetes configuration parsing and a bodyless download redirect over
loopback HTTP. It needs no cluster credentials.
