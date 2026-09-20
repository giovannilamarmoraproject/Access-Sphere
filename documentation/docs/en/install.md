# Local installation

## Clone the repository

```powershell
git clone https://github.com/giovannilamarmora/Access-Sphere.git
cd Access-Sphere
```

## Install VitePress dependencies

```powershell
cd documentation
npm ci
cd ..
```

VitePress is a separate Node project. Maven builds it automatically; manually
copying `dist` is not required.

## Configure the database

For local development, configure:

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

Use environment variables or a managed secret for non-local environments.

## Build and run

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-22"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean package -DskipTests
java -jar target\access-sphere.jar
```

The build runs `npm run docs:build`, copies the generated site into the
Spring classpath, compiles Java and creates `target/access-sphere.jar`.

Open `http://localhost:8081/docs/en/overview`.
