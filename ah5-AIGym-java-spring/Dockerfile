FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/arrowhead-ai-gym-5.2.2.jar /app/app.jar
COPY config /app/config
RUN chown -R 10001:10001 /app
USER 10001:10001
EXPOSE 8478
HEALTHCHECK --interval=15s --start-period=60s --timeout=4s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8478"]
ENTRYPOINT ["java","-Xms32m","-Xmx256m","-jar","/app/app.jar","--spring.config.additional-location=file:/app/config/application.properties"]
