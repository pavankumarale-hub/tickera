# Build context is the repository root so the module can resolve the
# reactor parent pom and the common-events dependency.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-events/pom.xml common-events/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
# Warm the dependency cache before copying sources
RUN mvn -B -pl booking-service -am dependency:go-offline || true
COPY common-events/src common-events/src
COPY booking-service/src booking-service/src
RUN mvn -B -pl booking-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/booking-service/target/booking-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
