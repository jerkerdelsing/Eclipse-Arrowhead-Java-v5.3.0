FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/localcloudconfigurator.jar /app/localcloudconfigurator.jar
COPY catalogs /app/catalogs
RUN mkdir /state && chown 10001:10001 /state
USER 10001:10001
EXPOSE 8097
HEALTHCHECK --interval=10s --start-period=15s --timeout=3s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8097"]
ENTRYPOINT ["java","-Xmx160m","-XX:+UseSerialGC","-jar","/app/localcloudconfigurator.jar"]
CMD ["serve","-listen","0.0.0.0:8097","-catalog","/app/catalogs/javacloud.json"]
