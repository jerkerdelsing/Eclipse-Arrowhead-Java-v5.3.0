# Local context only: no sibling repository or shared runtime is used.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
RUN mkdir -p /native && cd /native && jar xf /source/target/arrowhead-gatetunnel-5.2.2.jar org/sqlite/native/Linux/$(uname -m)/libsqlitejdbc.so && cp org/sqlite/native/Linux/$(uname -m)/libsqlitejdbc.so /native/libsqlitejdbc.so
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/arrowhead-gatetunnel-5.2.2.jar /app/gatetunnel.jar
COPY --from=build /native/libsqlitejdbc.so /app/lib/libsqlitejdbc.so
RUN mkdir -p /data && chown 10001:10001 /data /app
USER 10001:10001
RUN java -Dorg.sqlite.lib.path=/app/lib -jar /app/gatetunnel.jar storage-selftest
EXPOSE 8453
HEALTHCHECK --interval=30s --timeout=6s --start-period=20s CMD ["java","-jar","/app/gatetunnel.jar","healthcheck"]
ENTRYPOINT ["java","-Dorg.sqlite.lib.path=/app/lib","-Xms32m","-Xmx256m","-jar","/app/gatetunnel.jar"]
