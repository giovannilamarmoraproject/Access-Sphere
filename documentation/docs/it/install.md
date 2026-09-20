# Installazione locale

## Clonazione

```powershell
git clone https://github.com/giovannilamarmora/Access-Sphere.git
cd Access-Sphere
```

## Dipendenze VitePress

La documentazione è un progetto Node separato:

```powershell
cd documentation
npm ci
cd ..
```

Non è necessario copiare manualmente `dist`: Maven esegue la build durante il
packaging.

## Configurazione database

Imposta MySQL/MariaDB e verifica i valori:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/access_sphere
    username: root
    password: root
  r2dbc:
    url: r2dbc:mysql://localhost:3306/access_sphere
    username: root
    password: root
```

Per un ambiente reale sostituisci ogni credenziale con una variabile
d'ambiente o un secret gestito.

## Build

```powershell
$env:JAVA_HOME = "C:\percorso\jdk-22"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean package -DskipTests
```

La build:

1. esegue `npm run docs:build`;
2. genera le pagine VitePress;
3. copia `documentation/docs/.vitepress/dist` in `target/classes/static/docs`;
4. compila le classi Java;
5. crea `target/access-sphere.jar`.

## Avvio

```powershell
java -jar target\access-sphere.jar
```

Apri `http://localhost:8081/docs/it/overview`.
