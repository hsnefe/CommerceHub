FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-security/pom.xml common-security/
COPY auth-service/pom.xml auth-service/
COPY product-service/pom.xml product-service/
RUN mvn -pl product-service -am dependency:go-offline -B
COPY common-security/src common-security/src
COPY product-service/src product-service/src
RUN mvn -pl product-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/product-service/target/product-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
