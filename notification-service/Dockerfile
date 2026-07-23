FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-events/pom.xml common-events/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -B -pl notification-service -am dependency:go-offline || true
COPY common-events/src common-events/src
COPY notification-service/src notification-service/src
RUN mvn -B -pl notification-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
