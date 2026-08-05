FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-security/pom.xml common-security/
COPY common-messaging/pom.xml common-messaging/
COPY eureka-server/pom.xml eureka-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY product-service/pom.xml product-service/
COPY inventory-service/pom.xml inventory-service/
COPY order-service/pom.xml order-service/
COPY notification-service/pom.xml notification-service/
COPY analytics-service/pom.xml analytics-service/
RUN mvn -pl inventory-service -am dependency:go-offline -B
COPY common-security/src common-security/src
COPY common-messaging/src common-messaging/src
COPY inventory-service/src inventory-service/src
RUN mvn -pl inventory-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
