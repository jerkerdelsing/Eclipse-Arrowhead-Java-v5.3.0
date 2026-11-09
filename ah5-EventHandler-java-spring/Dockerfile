FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY vendor ./vendor
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/arrowhead-eventhandler-5.2.2.jar /app/app.jar
COPY config /app/config
RUN mkdir -p /app/logs && chown -R 10001:10001 /app
USER 10001:10001
EXPOSE 8455
HEALTHCHECK --interval=15s --start-period=90s --timeout=4s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8455"]
ENTRYPOINT ["java","-Xms64m","-Xmx256m","-Dlog4j.configurationFile=/app/config/log4j2.xml","-jar","/app/app.jar","--spring.config.additional-location=file:/app/config/application.properties"]
