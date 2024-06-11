package io.github.giovannilamarmora.accesssphere.data.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.IAccessTokenDAO;
import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
public class UserServiceTest {

  @Autowired private UserService userService;
  @MockBean private StrapiClient strapiClient;
  @MockBean private ServerHttpRequest request;
  @MockBean private AccessTokenData accessTokenData;
  @Autowired private TokenService tokenService;
  @MockBean private IAccessTokenDAO accessTokenDAO;
  private final ObjectMapper mapper = MapperUtils.mapper().failOnUnknownProprieties().build();

  @BeforeAll
  static void init() {
    Mockito.mockStatic(GoogleGrpcService.class);
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

    // Act
    Mono<ResponseEntity<Response>> result = userService.register(user, clientId, registrationToken);

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
    Mono<ResponseEntity<Response>> result = userService.register(user, clientId, registrationToken);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
            jwtData.getEmail(),
            jwtData.getIdentifier(),
            jwtData.getType(),
            jwtData.getIat(),
            jwtData.getExp(),
            jwtData.getExp(),
            Utils.mapper().writeValueAsString(jwtData),
            TokenStatus.ISSUED);

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
}
