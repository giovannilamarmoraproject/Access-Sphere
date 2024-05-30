FROM maven:3.8.2-jdk-11 AS build
COPY . .
RUN mvn clean package

FROM amazoncorretto:22
EXPOSE 8080
WORKDIR /
COPY --from=build /target/access-sphere.jar access-sphere.jar

ENTRYPOINT ["java","-jar","access-sphere.jar"]
ENV TZ=Europe/Rome
