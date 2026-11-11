FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /source
COPY pom.xml .
COPY vendor ./vendor
COPY src ./src
COPY catalogs ./catalogs
COPY config ./config
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# SOS-16 (2026-11-10): Ubuntu security updates on top of the pinned base image
RUN apt-get update && apt-get -y upgrade && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /source/target/localcloudconfigurator.jar /app/localcloudconfigurator.jar
COPY config /app/config
COPY catalogs /app/catalogs
RUN mkdir -p /app/logs && chown -R 10001:10001 /app/logs
USER 10001:10001
# 8492: localCloudConfiguration over mutual TLS. The host worker (8493) runs outside this container, on the host's loopback.
EXPOSE 8492
HEALTHCHECK --interval=15s --start-period=90s --timeout=4s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8492"]
ENTRYPOINT ["java","-Xms64m","-Xmx256m","-Dlog4j.configurationFile=/app/config/log4j2.xml","-jar","/app/localcloudconfigurator.jar","serve","--spring.config.additional-location=file:/app/config/application.properties"]
