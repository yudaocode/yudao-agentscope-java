# AgentScope RAG Simple on Boot 2.7 and Java 8

Install the Java 8 reactor first, then run the independent fixture with a JDK 8 test JVM.

```bash
mvn -f compatibility-tests/boot2-rag-simple/pom.xml test -Dcompat.test.jvm=/path/to/jdk8/bin/java
```

The fixture creates a small PDF, then loads it through both reader entry points with the
installed artifacts and Boot 2.7 dependency management.

For the Elasticsearch backend with the Boot 2.7 parent, align `elasticsearch.version` to 8.19.11 and `jakarta-json.version` to 2.0.1. The fixture also runs a vector search through a local Elasticsearch HTTP server.
