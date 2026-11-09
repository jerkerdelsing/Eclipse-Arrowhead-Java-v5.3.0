FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean verify
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/localcloudmanager-docker-proxy.jar /app/docker-proxy.jar
EXPOSE 2375
# Runs as root only to open /var/run/docker.sock (Docker Desktop exposes it to root); read-only root filesystem, no capabilities
# beyond that, on an internal network. Forwards only GET /containers/json, GET /containers/<id>/logs and
# POST /containers/<id>/(start|stop|restart) for its own Compose project (lifecycle only for MANAGED_SERVICES).
ENTRYPOINT ["java","-Xms16m","-Xmx96m","-XX:+UseSerialGC","-jar","/app/docker-proxy.jar"]
