package io.github.giovannilamarmora.accesssphere.data.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderClient;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.IAccessTokenDAO;
import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {

  @Autowired private UserService userService;
  @MockBean private StrapiClient strapiClient;
  @MockBean private ServerHttpRequest request;
  @MockBean private AccessTokenData accessTokenData;
  @Autowired private TokenService tokenService;
  @MockBean private IAccessTokenDAO accessTokenDAO;
  @MockBean private EmailSenderClient emailSenderClient;
  @MockBean private IUserDAO userDAO;
  private MockedStatic<GoogleGrpcService> mockStatic;
  private final ObjectMapper mapper = MapperUtils.mapper().failOnUnknownProprieties().build();

  @BeforeEach
  public void setUp() {
    // Registering a static mock for UserService before each test
    mockStatic = mockStatic(GoogleGrpcService.class);
  }

  @AfterEach
  public void tearDown() {
    // Closing the mockStatic after each test
    mockStatic.close();
  }

  @Test
  public void test_registers_new_user_with_valid_data() throws IOException {
    // Arrange
    User user = new User();
    user.setUsername("test2");
    user.setEmail("pippo@gmail.com");
    user.setPassword("Ciccio.2025");
    String clientId = "GOOGLE-OAUTH-01";
    String registrationToken = "token";

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    StrapiResponse userRes =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(strapiClient.saveUser(any())).thenReturn(Mono.just(ResponseEntity.ok(userRes)));
    UserEntity userFind = new UserEntity();
    userFind.setIdentifier(user.getIdentifier());
    userFind.setUsername(user.getName());
    userFind.setEmail(user.getEmail());
    userFind.setPassword("Ciccio.2025");
    when(userDAO.saveAndFlush(any())).thenReturn(userFind);
    // Act
    Mono<ResponseEntity<Response>> result =
        userService.register(null, user, clientId, registrationToken, false);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.CREATED, res.getStatusCode());
              assertEquals("User test2 successfully registered!", res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_registration_with_invalid_client_id() throws IOException {
    // Arrange
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("testuser@example.com");
    String clientId = "invalidClientId";
    String registrationToken = "validRegistrationToken";
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.register(null, user, clientId, registrationToken, false);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(throwable -> throwable instanceof UtilsException)
        .verify();
  }

  @Test
  public void test_successfully_retrieves_profile_data_for_valid_google_token()
      throws IOException, GeneralSecurityException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "GOOGLE-OAUTH-01";

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.GOOGLE);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);

    // try (MockedStatic<GoogleGrpcService> utilities = Mockito.mockStatic(GoogleGrpcService.class))
    // {
    //  utilities
    //      .when(() -> GoogleGrpcService.getUserInfo(anyString(), anyString()))
    //      .thenReturn(google);
    //  assertEquals(GoogleGrpcService.getUserInfo("test", "test"), google);
    // }

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.profile("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("Profile Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_retrieves_profile_data_for_valid_bearer_token()
      throws IOException, GeneralSecurityException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "GOOGLE-OAUTH-01";

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);
    jwtData.setType(OAuthType.BEARER);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.profile("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("Profile Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_invalid_or_malformed_bearer_token() throws IOException {
    // Arrange
    String bearer = "invalidBearerToken";
    String clientId = "GOOGLE-OAUTH-01";

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    // Act
    Mono<ResponseEntity<Response>> result = userService.profile("Bearer " + bearer, request);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(throwable -> throwable instanceof UtilsException)
        .verify();
  }

  @Test
  public void test_successfully_retrieve_user_info_with_bearer_token_and_include_user_data_true()
      throws IOException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "BEARER-OAUTH-01";

    ServerHttpRequest request =
        MockServerHttpRequest.get("/userinfo?include_user_data=true").build();

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);
    jwtData.setType(OAuthType.BEARER);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.userInfo("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("UserInfo Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_retrieve_user_info_with_bearer_token_and_include_user_data_false()
      throws IOException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "BEARER-OAUTH-01";

    ServerHttpRequest request =
        MockServerHttpRequest.get("/userinfo?include_user_data=false").build();

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);
    jwtData.setType(OAuthType.BEARER);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.userInfo("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("UserInfo Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_retrieve_user_info_with_google_token_and_include_user_data_true()
      throws IOException, GeneralSecurityException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "GOOGLE-OAUTH-01";

    ServerHttpRequest request =
        MockServerHttpRequest.get("/userinfo?include_user_data=true").build();

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.GOOGLE);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);
    jwtData.setType(OAuthType.GOOGLE);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    // try (MockedStatic<GoogleGrpcService> utilities = Mockito.mockStatic(GoogleGrpcService.class))
    // {
    //  utilities
    //      .when(() -> GoogleGrpcService.getUserInfo(anyString(), anyString()))
    //      .thenReturn(google);
    //  assertEquals(GoogleGrpcService.getUserInfo("test", "test"), google);
    // }

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.userInfo("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("UserInfo Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_retrieve_user_info_with_google_token_and_include_user_data_false()
      throws IOException, GeneralSecurityException {
    // Arrange
    User user = new User();
    user.setUsername("test22");
    user.setEmail("pippo@gmail.com");
    String clientId = "GOOGLE-OAUTH-01";

    ServerHttpRequest request =
        MockServerHttpRequest.get("/userinfo?include_user_data=false").build();

    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.GOOGLE);

    JsonNode payload =
        mapper.readValue(
            new ClassPathResource("mock/Payload.json").getInputStream(), JsonNode.class);
    when(accessTokenData.getPayload()).thenReturn(payload);

    ClientCredential clientCredential =
        StrapiMapper.mapFromStrapiResponseToClientCredential(response);

    // Generate Token
    JWTData jwtData =
        mapper.readValue(
            new ClassPathResource("mock/JTWData.json").getInputStream(), JWTData.class);
    jwtData.setType(OAuthType.GOOGLE);

    AccessTokenEntity accessTokenToBeSaved =
        new AccessTokenEntity(
            null,
            "refreshToken",
            "accessToken",
            "idToken",
            "session_id",
            "client_id",
            jwtData.getSub(),
            SubjectType.CUSTOMER,
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED,
            "[\"TEST_ROLES\"]");

    when(accessTokenDAO.save(any())).thenReturn(accessTokenToBeSaved);

    AuthToken token = tokenService.generateToken(jwtData, clientCredential, "{}");

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    // try (MockedStatic<GoogleGrpcService> utilities = Mockito.mockStatic(GoogleGrpcService.class))
    // {
    //  utilities
    //      .when(() -> GoogleGrpcService.getUserInfo(anyString(), anyString()))
    //      .thenReturn(google);
    //  assertEquals(GoogleGrpcService.getUserInfo("test", "test"), google);
    // }

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    // Act
    Mono<ResponseEntity<Response>> result =
        userService.userInfo("Bearer " + token.getAccess_token(), request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("UserInfo Data for " + user.getUsername(), res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_invalid_or_expired_bearer_token() throws IOException {
    // Mock request without include_user_data parameter
    ServerHttpRequest request = MockServerHttpRequest.get("/userinfo").build();
    String clientId = "GOOGLE-OAUTH-01";
    // Client ID
    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    // Invalid or expired Bearer token
    String bearer = "invalidOrExpiredBearerToken";

    // Call userInfo method
    Mono<ResponseEntity<Response>> responseMono = userService.userInfo("Bearer " + bearer, request);

    // Assert
    StepVerifier.create(responseMono)
        .expectErrorMatches(throwable -> throwable instanceof UtilsException)
        .verify();
  }

  @Test
  public void test_successfully_updates_user_information() throws IOException {
    // Arrange
    String bearer = "validBearerToken";
    String clientId = "BEARER-OAUTH-01";
    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});

    User userToUpdate = new User();
    userToUpdate.setIdentifier(strapiUser.getFirst().getIdentifier());
    userToUpdate.setUsername(strapiUser.getFirst().getUsername());
    userToUpdate.setEmail(strapiUser.getFirst().getEmail());
    userToUpdate.setPassword("Ciccio.2025");

    UserEntity userFind = new UserEntity();
    userFind.setIdentifier(strapiUser.getFirst().getIdentifier());
    userFind.setUsername(strapiUser.getFirst().getUsername());
    userFind.setEmail(strapiUser.getFirst().getEmail());
    userFind.setPassword("Ciccio.2025");

    ServerHttpRequest request = MockServerHttpRequest.get("/updateUser").build();

    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    when(accessTokenData.getIdentifier()).thenReturn(userToUpdate.getIdentifier());
    when(strapiClient.getUserByIdentifier(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));
    when(strapiClient.updateUser(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));
    when(userDAO.findUserEntityByIdentifier(any())).thenReturn(userFind);
    when(userDAO.saveAndFlush(any())).thenReturn(userFind);

    // Act
    Mono<ResponseEntity<Response>> resultUpdate =
        userService.updateUser(userToUpdate, bearer, request);

    // Assert
    StepVerifier.create(resultUpdate)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals(
                  "User " + userToUpdate.getUsername() + " successfully updated!",
                  res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_change_password_request() throws IOException {
    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});

    String clientId = "BEARER-OAUTH-01";
    String registrationToken = "token";

    User userToUpdate = new User();
    userToUpdate.setIdentifier(strapiUser.getFirst().getIdentifier());
    userToUpdate.setUsername(strapiUser.getFirst().getName());
    userToUpdate.setEmail(strapiUser.getFirst().getEmail());
    userToUpdate.setPassword("Ciccio.2025");

    UserEntity userFind = new UserEntity();
    userFind.setIdentifier(strapiUser.getFirst().getIdentifier());
    userFind.setUsername(strapiUser.getFirst().getName());
    userFind.setEmail(strapiUser.getFirst().getEmail());
    userFind.setPassword("Ciccio.2025");

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    StrapiResponse userRes =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));
    when(strapiClient.saveUser(any())).thenReturn(Mono.just(ResponseEntity.ok(userRes)));
    when(userDAO.saveAndFlush(any())).thenReturn(userFind);

    Mono<ResponseEntity<Response>> result =
        userService.register(null, userToUpdate, clientId, registrationToken, false);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.CREATED, res.getStatusCode());
              assertEquals(
                  "User " + userToUpdate.getUsername() + " successfully registered!",
                  res.getBody().getMessage());
            })
        .verifyComplete();

    when(accessTokenData.getClientId()).thenReturn(clientId);
    when(accessTokenData.getType()).thenReturn(OAuthType.BEARER);

    when(accessTokenData.getIdentifier()).thenReturn(userToUpdate.getIdentifier());
    when(strapiClient.getUserByIdentifier(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    // Arrange
    ChangePassword changePassword =
        new ChangePassword("TEST", userToUpdate.getEmail(), null, null, null);

    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));
    StrapiResponse templates =
        mapper.readValue(
            new ClassPathResource("mock/Template.json").getInputStream(), new TypeReference<>() {});
    when(strapiClient.getTemplateById(any(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(templates)));

    when(strapiClient.updateUser(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(userDAO.findUserEntityByIdentifier(any())).thenReturn(userFind);
    when(userDAO.saveAndFlush(any())).thenReturn(userFind);

    EmailResponse expected =
        new EmailResponse(LocalDateTime.now(), "Email SENT", strapiUser.getFirst().getTokenReset());

    when(emailSenderClient.sendEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(expected)));

    Mono<ResponseEntity<Response>> resultFinal =
        userService.changePasswordRequest(changePassword, "en-GB", true);
    // Assert
    StepVerifier.create(resultFinal)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("Email Sent! Check your email address!", res.getBody().getMessage());
            })
        .verifyComplete();

    Mono<ResponseEntity<Response>> resultFinal2 =
        userService.changePasswordRequest(changePassword, "en-GB", false);
    // Assert
    StepVerifier.create(resultFinal2)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("Email Sent! Check your email address!", res.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_change_password() throws IOException {
    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    strapiUser.getFirst().setUpdatedAt(LocalDateTime.now());

    when(strapiClient.getUserByTokenReset(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    when(strapiClient.updateUser(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(strapiClient.getUserByIdentifier(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    UserEntity userFind = new UserEntity();
    userFind.setIdentifier(strapiUser.getFirst().getIdentifier());
    userFind.setUsername(strapiUser.getFirst().getName());
    userFind.setEmail(strapiUser.getFirst().getEmail());
    userFind.setPassword("Ciccio.2025");
    when(userDAO.findUserEntityByIdentifier(any())).thenReturn(userFind);

    when(userDAO.saveAndFlush(any())).thenReturn(userFind);

    User userToUpdate = new User();
    userToUpdate.setIdentifier(strapiUser.getFirst().getIdentifier());
    userToUpdate.setUsername(strapiUser.getFirst().getName());
    userToUpdate.setEmail(strapiUser.getFirst().getEmail());
    userToUpdate.setPassword("Ciccio.2025");

    ChangePassword changePassword =
        new ChangePassword(
            "TEST",
            userToUpdate.getEmail(),
            Base64.getEncoder().encodeToString("Ciccio.2025".getBytes()),
            "token",
            null);

    Mono<ResponseEntity<Response>> result = userService.changePassword(changePassword);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
              assertEquals("Password Updated!", res.getBody().getMessage());
            })
        .verifyComplete();
  }
}
