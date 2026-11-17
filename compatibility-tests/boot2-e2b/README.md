# Boot 2 E2B consumer

An independent official Boot2.7.18 parent consumes installed Java8 compatibility jars, with explicit Harness dependency because the E2B backend declares it provided. This fixture is outside the default reactor and is not deployed.

The complete POM demonstrates Jackson/HTTP/Logback overrides plus OkHttp4.12.0, Reactor BOM2024.0.18 and Commons Lang3.20.0, preserving the Boot2/Spring5/SLF4J1.7 route. Run after installing the default SDK:

```sh
./mvnw -f compatibility-tests/boot2-e2b/pom.xml verify \
  -Dcompat.test.jvm="$JAVA8_HOME/bin/java"
```

JDK17 is used for Maven/build tools; the test VM must be actual Java8. All3 cases passed without cloud credentials: installed unique E2B jar plus Boot context/JSON setup, actual local platform create request and bounded snapshot-reference input. Runtime cloud start/execute/snapshot persistence still requires separate account acceptance. Full [usage and backend limits](../../JAVA8-COMPATIBILITY.md#e2b-backend-on-java-8).
