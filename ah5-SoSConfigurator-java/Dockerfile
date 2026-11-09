FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/sosconfigurator.jar /app/sosconfigurator.jar
COPY catalogs /app/catalogs
USER 10001:10001
EXPOSE 8096
HEALTHCHECK --interval=15s --start-period=20s --timeout=4s CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8096"]
ENTRYPOINT ["java","-jar","/app/sosconfigurator.jar"]
CMD ["serve","-listen",":8096","-catalog","/app/catalogs/sos.json"]
