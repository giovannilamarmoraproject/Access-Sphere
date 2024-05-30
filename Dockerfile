FROM maven:3.9.7-eclipse-temurin-11 AS build
COPY . .
RUN mvn clean package

FROM eclipse-temurin:22-jdk
EXPOSE 8080
WORKDIR /
COPY --from=build /target/access-sphere.jar access-sphere.jar

ENTRYPOINT ["java","-jar","access-sphere.jar"]
ENV TZ=Europe/Rome
