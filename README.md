# 🎡 **AccessSphere API**

## 📝 **Overview**

AccessSphere API provides endpoints for user management and OAuth 2.0 authentication and authorization. This
microservice is designed to handle user registration, login, session management, and token-based authentication.

## 🚀 **Endpoints**

### 👤 **UserController**

#### 📄 **Get User Info**

Retrieve the information of the current user.

- **URL**: `/userInfo`
- **Method**: `GET`
- **Headers**:
    - `Authorization`: Bearer token (optional)
- **Responses**:
    - `200 OK`: User info retrieved successfully.
    - `401 Unauthorized`: Invalid or missing authorization token.
    - `403 Forbidden`: Access to the resource is forbidden.

#### 📝 **Register User**

Register a new user in the system.

- **URL**: `/users/register`
- **Method**: `POST`
- **Request Body**:
    - `User`: User object (required)
- **Request Params**:
    - `client_id`: Client identifier (required)
    - `registration_token`: Registration token (optional)
- **Responses**:
    - `201 Created`: User registered successfully.
    - `400 Bad Request`: Invalid input data.
    - `409 Conflict`: User already exists.

#### ✏️ **Update User**

Update the details of an existing user.

- **URL**: `/users/update`
- **Method**: `PUT`
- **Request Body**:
    - `User`: User object (required)
- **Headers**:
    - `Authorization`: Bearer token (optional)
- **Responses**:
    - `200 OK`: User updated successfully.
    - `400 Bad Request`: Invalid input data.

### 🔐 **OAuthController**

#### 🔄 **Start OAuth 2.0 Authorization**

Initiate the OAuth 2.0 authorization flow.

- **URL**: `/authorize`
- **Method**: `GET`
- **Request Params**:
    - `response_type`: Type of response (required)
    - `access_type`: Type of access (required)
    - `client_id`: Client identifier (required)
    - `redirect_uri`: Redirect URI (required)
    - `scope`: Scopes (required)
    - `registration_token`: Registration token (optional)
    - `state`: State (optional)
- **Responses**:
    - `200 OK`: Successful operation.
    - `400 Bad Request`: Invalid input data.

#### 🔐 **Perform OAuth 2.0 Login**

Perform the login operation for OAuth 2.0.

- **URL**: `/login/{client_id}`
- **Method**: `GET`
- **Request Params**:
    - `scope`: Scopes (optional)
    - `code`: Authorization code (optional)
    - `prompt`: Prompt behavior (optional)
- **Responses**:
    - `400 Bad Request`: Invalid input data.

#### 🔑 **Get OAuth 2.0 Token**

Retrieve the OAuth 2.0 token.

- **URL**: `/token`
- **Method**: `POST`
- **Request Params**:
    - `client_id`: Client identifier (required)
    - `refresh_token`: Refresh token (optional)
    - `grant_type`: Grant type (required)
    - `scope`: Scopes (optional)
    - `code`: Authorization code (optional)
    - `redirect_uri`: Redirect URI (optional)
    - `prompt`: Prompt behavior (optional)
- **Headers**:
    - `Authorization`: Basic auth (optional)
- **Responses**:
    - `200 OK`: Token retrieved successfully.
    - `400 Bad Request`: Invalid input data.

## 🛠 **Development**

### 📋 **Prerequisites**

- Java 11+
- Spring Boot
- Maven or Gradle

### ▶️ **Running the Application**

To run the application, use one of the following commands:

```bash
./mvnw spring-boot:run
# or
./gradlew bootRun
```

### 🛠 **Building the Application**

To build the application, you can use Maven or Gradle:

**With Maven:**

```bash
./mvnw clean install
```

### ⚙️ **Configuration**

#### `application.properties`

The application's configuration is managed primarily through Spring Boot's properties and YAML files. Below are some key
configuration points:

```properties
# Configurazione del server
server.port=8080
# Configurazione del filtro Session ID
filter.session-id.shouldNotFilter=/public/**
filter.session-id.generateSessionURI=/auth/**
filter.session-id.logoutURI=/logout
filter.session-id.bearerNotFilter=/public/**
```

### 📝  **Logging**

📝 Logging
The application utilizes SLF4J for logging. Configure logging using
`logback-spring.xml` o `application.yml`.

```properties
logging.level.root=INFO
logging.level.io.github.giovannilamarmora.accesssphere=DEBUG
logging.file.name=logs/accesssphere.log

```