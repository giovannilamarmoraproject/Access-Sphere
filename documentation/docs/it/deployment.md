# Avvio e deploy

## Avvio Maven

```powershell
.\mvnw.cmd spring-boot:run
```

Maven ricompila la documentazione prima di avviare Spring Boot. Per modificare
solo il testo con hot reload:

```powershell
cd documentation
npm.cmd run docs:dev
```

## Docker

Il Dockerfile usa uno stage Node per VitePress e uno stage Maven per il jar:

```bash
docker build -t access-sphere:local .
docker run --rm --name access-sphere -p 8081:8081 access-sphere:local
```

Passa database e secret con `-e` o con un orchestratore; non inserirli
nell'immagine.

## CI/CD

I workflow GitHub Actions installano Java 22 e Node 22, eseguono `npm ci` nella
cartella `documentation`, quindi eseguono i test e il packaging Maven.

## Verifica post-deploy

Controlla:

```text
GET /docs/it/overview
GET /docs/en/overview
GET /swagger-ui.html
GET /api-docs
```

Se `/docs` restituisce 401, aggiorna l'elenco `shouldNotFilter` dei filtri
request/response e session ID. Se restituisce `No static resource`, verifica
che il jar contenga `static/docs/{language}/{page}.html`.
