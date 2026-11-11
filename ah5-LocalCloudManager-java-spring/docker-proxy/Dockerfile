FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# SOS-16 (2026-11-10): Ubuntu security updates on top of the pinned base image
RUN apt-get update && apt-get -y upgrade && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /source/target/localcloudmanager-docker-proxy.jar /app/docker-proxy.jar
EXPOSE 2375
# SOS-8 (2026-11-10): runs as an unprivileged user in group 0, which is all /var/run/docker.sock needs on Docker Desktop
# (srw-rw---- root:root); read-only root filesystem, no capabilities, on an internal network. Forwards only
# GET /containers/json, GET /containers/<id>/logs and POST /containers/<id>/(start|stop|restart) for its own Compose
# project (lifecycle only for MANAGED_SERVICES).
USER 10001:0
ENTRYPOINT ["java","-Xms16m","-Xmx96m","-XX:+UseSerialGC","-jar","/app/docker-proxy.jar"]
