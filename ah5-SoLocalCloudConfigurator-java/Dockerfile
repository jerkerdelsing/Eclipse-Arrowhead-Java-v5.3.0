FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /source
COPY pom.xml .
COPY vendor ./vendor
COPY src ./src
# the deployment kit (with its documentation) and the policy catalog are packaged as classpath resources (pom.xml)
COPY kit ./kit
COPY catalogs ./catalogs
COPY config ./config
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
RUN apt-get update && apt-get -y upgrade && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /source/target/solocalcloudconfigurator.jar /app/solocalcloudconfigurator.jar
COPY config /app/config
COPY catalogs /app/catalogs
RUN mkdir -p /app/logs && chown -R 10001:10001 /app/logs
USER 10001:10001
# 8496: soLocalCloudConfiguration over mutual TLS. No Docker access: packages are rendered here, never built or run.
EXPOSE 8496
HEALTHCHECK --interval=15s --start-period=90s --timeout=4s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8496"]
ENTRYPOINT ["java","-Xms64m","-Xmx384m","-Dlog4j.configurationFile=/app/config/log4j2.xml","-jar","/app/solocalcloudconfigurator.jar","--spring.config.additional-location=file:/app/config/application.properties"]
