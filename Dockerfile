# Fase di build
FROM maven:3.8.2-eclipse-temurin-22 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Fase di runtime
FROM eclipse-temurin:22-jre
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/target/access-sphere.jar access-sphere.jar

ENTRYPOINT ["java","-jar","access-sphere.jar"]
ENV TZ Europe/Rome
