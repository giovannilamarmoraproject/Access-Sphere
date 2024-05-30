FROM eclipse-temurin:22-jdk
EXPOSE 8080
WORKDIR /
COPY --from=build /target/access-sphere.jar access-sphere.jar

ARG MAVEN_VERSION=3.9.7

ENTRYPOINT ["java","-jar","access-sphere.jar"]
ENV TZ=Europe/Rome
