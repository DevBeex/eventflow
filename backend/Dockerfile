# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src

# Packaging only. Automated tests run in Compose service `tests` (and in CI via ./mvnw verify)
# before the app is considered ready. Do not re-enable -DskipTests silently in Compose.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/quarkus-app/ /app/
ENV QUARKUS_PROFILE=prod
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
