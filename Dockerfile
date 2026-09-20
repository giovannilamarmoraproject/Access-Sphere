FROM node:22-alpine AS documentation
WORKDIR /documentation
COPY documentation/package*.json ./
RUN npm ci
COPY documentation/docs ./docs
RUN npm run docs:build

FROM maven:3.9.7-eclipse-temurin-22 AS build
COPY . .
COPY --from=documentation /documentation/docs/.vitepress/dist ./documentation/docs/.vitepress/dist
RUN mvn clean package -Ddocs.build.skip=true

FROM eclipse-temurin:22-jdk
EXPOSE 8080
WORKDIR /
COPY src/main/resources /app/resources
COPY --from=build /target/access-sphere.jar access-sphere.jar

ENTRYPOINT ["java","-jar","access-sphere.jar"]
ENV TZ=Europe/Rome
