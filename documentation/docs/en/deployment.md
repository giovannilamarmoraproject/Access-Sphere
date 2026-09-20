# Running and deployment

## Maven development run

```powershell
.\mvnw.cmd spring-boot:run
```

Maven builds the documentation before starting Spring Boot. For documentation
hot reload only:

```powershell
cd documentation
npm.cmd run docs:dev
```

## Docker

The Dockerfile uses a Node stage for VitePress and a Maven stage for the
Spring Boot jar:

```bash
docker build -t access-sphere:local .
docker run --rm --name access-sphere -p 8081:8081 access-sphere:local
```

Pass database credentials and secrets through the environment or an
orchestrator, never through the image.

## CI/CD

The GitHub Actions workflows install Java 22 and Node 22, run `npm ci` in
`documentation`, execute tests and package the application.

## Post-deployment checks

```text
GET /docs/it/overview
GET /docs/en/overview
GET /swagger-ui.html
GET /api-docs
```

If documentation returns 401, include `/docs` and `/docs/**` in the public
filter exclusions. If it returns `No static resource`, verify that the jar
contains `static/docs/{language}/{page}.html`.
