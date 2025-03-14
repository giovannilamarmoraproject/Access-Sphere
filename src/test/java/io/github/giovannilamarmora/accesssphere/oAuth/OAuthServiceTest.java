package io.github.giovannilamarmora.accesssphere.oAuth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.IAccessTokenDAO;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.auth.TokenUtils;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
public class OAuthServiceTest {

  private final ObjectMapper mapper = MapperUtils.mapper().failOnUnknownProprieties().build();

  @Autowired private OAuthService oAuthService;
  @MockBean private StrapiClient strapiClient;
  @MockBean private AccessTokenService accessTokenService;
  private MockedStatic<GoogleGrpcService> mockStatic;
  @MockBean private IAccessTokenDAO accessTokenDAO;
  @MockBean private AccessTokenData accessTokenData;

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
  public void test_successfully_retrieves_client_credentials_by_client_id()
      throws IOException, GeneralSecurityException {
    // Arrange
    String responseType = "code";
    String accessType = "online";
    String clientId = "GOOGLE-OAUTH-01";
    String redirectUri = "http://localhost/callback";
    String scope = "openid email";
    String registrationToken = "some_registration_token";
    String state = "some_state";

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    Mockito.when(
            GoogleGrpcService.startGoogleAuthorization(
                any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(redirectUri);

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/authorize").build());

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.authorize(
            responseType,
            accessType,
            clientId,
            redirectUri,
            scope,
            registrationToken,
            null,
            state,
            exchange.getResponse());

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.TEMPORARY_REDIRECT, res.getStatusCode());
              assertNotNull(res.getHeaders().getLocation());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_google_oAuth_token_include_user_data_false()
      throws IOException, GeneralSecurityException {
    // Arrange
    String clientId = "test-client-id";
    String refresh_token = null;
    String grant_type = "authorization_code";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    String basic = "test-basic";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=false").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    TokenResponse tokenResponse = new TokenResponse();
    tokenResponse.set("id_token", "token");
    tokenResponse.set("access_token", "token");
    tokenResponse.set("token_type", "Bearer");
    Mockito.when(
            GoogleGrpcService.getTokenResponse(
                anyString(), anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(tokenResponse);

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    google.setExpirationTimeSeconds(3600000L);
    google.setIssuedAtTimeSeconds(360000L);

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    when(accessTokenService.save(
            any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any()))
        .thenReturn(new AccessTokenData());

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_google_oAuth_token_include_user_data_true()
      throws IOException, GeneralSecurityException {
    // Arrange
    String clientId = "test-client-id";
    String refresh_token = null;
    String grant_type = "authorization_code";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    String basic = "test-basic";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=true").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    TokenResponse tokenResponse = new TokenResponse();
    tokenResponse.set("id_token", "token");
    tokenResponse.set("access_token", "token");
    tokenResponse.set("token_type", "Bearer");
    Mockito.when(
            GoogleGrpcService.getTokenResponse(
                anyString(), anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(tokenResponse);

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    google.setExpirationTimeSeconds(3600000L);
    google.setIssuedAtTimeSeconds(360000L);

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(new AccessTokenData());

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_bearer_oAuth_token_include_user_data_true()
      throws IOException, GeneralSecurityException {
    String basic = "Basic " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "test-client-id";
    String refresh_token = null;
    String grant_type = "password";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=true").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    StrapiResponse responseUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);
    when(strapiClient.login(any())).thenReturn(Mono.just(ResponseEntity.ok(responseUser)));
    when(strapiClient.getRefreshToken(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(responseUser)));

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(new AccessTokenData());

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_bearer_oAuth_token_include_user_data_false()
      throws IOException, GeneralSecurityException {
    String basic = "Basic " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "test-client-id";
    String refresh_token = null;
    String grant_type = "password";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=false").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    StrapiResponse responseUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);
    when(strapiClient.login(any())).thenReturn(Mono.just(ResponseEntity.ok(responseUser)));
    when(strapiClient.getRefreshToken(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(responseUser)));

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(new AccessTokenData());

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_invalid_grant_type() {
    // Arrange
    OAuthService oAuthService = new OAuthService();
    String clientId = "test-client-id";
    String refresh_token = null;
    String grant_type = "invalid_grant_type";
    String scope = "test-scope";
    String code = "test-code";
    String prompt = "test-prompt";
    String redirect_uri = "http://localhost/redirect";
    String basic = "test-basic";
    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/tokenOAuth").build());

    // Act & Assert
    assertThrows(
        OAuthException.class,
        () -> {
          oAuthService
              .tokenOAuth(
                  clientId,
                  refresh_token,
                  grant_type,
                  scope,
                  code,
                  prompt,
                  redirect_uri,
                  basic,
                  exchange)
              .block();
        });
  }

  @Test
  public void test_successfully_bearer_refresh_token_include_user_data_false() throws IOException {
    String basic = "Basic " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "BEARER-OAUTH-01";
    String refresh_token = "token";
    String grant_type = "refresh_token";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=false").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    String payload = "{\"access_token\": \"token\", \"refresh_token\": \"token\"}";

    AccessTokenData accessToken =
        new AccessTokenData(
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            clientId,
            SessionID.builder().generate(),
            null,
            SubjectType.CUSTOMER,
            "email@emial.com",
            "identifier",
            OAuthType.BEARER,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            mapper.readTree(payload),
            TokenStatus.ISSUED,
            List.of("DEFAULT"));

    when(accessTokenService.getByRefreshToken(anyString())).thenReturn(accessToken);

    StrapiResponse responseUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);
    when(strapiClient.refreshToken(any())).thenReturn(Mono.just(ResponseEntity.ok(responseUser)));

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(accessToken);

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_google_refresh_token_include_user_data_false()
      throws IOException, GeneralSecurityException {
    String basic = "Basic " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "GOOGLE-OAUTH-01";
    String refresh_token = "token";
    String grant_type = "refresh_token";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=false").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    String payload = "{\"access_token\": \"token\", \"refresh_token\": \"token\"}";

    AccessTokenData accessToken =
        new AccessTokenData(
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            clientId,
            SessionID.builder().generate(),
            null,
            SubjectType.CUSTOMER,
            "email@emial.com",
            "identifier",
            OAuthType.GOOGLE,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            mapper.readTree(payload),
            TokenStatus.ISSUED,
            List.of("DEFAULT"));

    when(accessTokenService.getByRefreshToken(anyString())).thenReturn(accessToken);

    StrapiResponse responseUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);
    when(strapiClient.refreshToken(any())).thenReturn(Mono.just(ResponseEntity.ok(responseUser)));

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(accessToken);

    TokenResponse tokenResponse = new TokenResponse();
    tokenResponse.set("id_token", "token");
    tokenResponse.set("access_token", "token");
    tokenResponse.set("token_type", "Bearer");
    Mockito.when(GoogleGrpcService.refreshGoogleOAuthToken(anyString(), anyString(), anyString()))
        .thenReturn(tokenResponse);

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    google.setExpirationTimeSeconds(3600000L);
    google.setIssuedAtTimeSeconds(360000L);

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));
    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_google_refresh_token_include_user_data_true()
      throws IOException, GeneralSecurityException {
    String basic = "Basic " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "GOOGLE-OAUTH-01";
    String refresh_token = "token";
    String grant_type = "refresh_token";
    String scope = "openid";
    String code = "openid";
    String prompt = "prompt";
    String redirect_uri = "http://localhost/redirect";
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tokenOAuth?include_user_data=true").build());

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    String payload = "{\"access_token\": \"token\", \"refresh_token\": \"token\"}";

    AccessTokenData accessToken =
        new AccessTokenData(
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            clientId,
            SessionID.builder().generate(),
            null,
            SubjectType.CUSTOMER,
            "email@emial.com",
            "identifier",
            OAuthType.GOOGLE,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            mapper.readTree(payload),
            TokenStatus.ISSUED,
            List.of("DEFAULT"));

    when(accessTokenService.getByRefreshToken(anyString())).thenReturn(accessToken);

    StrapiResponse responseUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiResponseOfUser.json").getInputStream(),
            StrapiResponse.class);
    when(strapiClient.refreshToken(any())).thenReturn(Mono.just(ResponseEntity.ok(responseUser)));

    List<StrapiUser> strapiUser =
        mapper.readValue(
            new ClassPathResource("mock/StrapiUser.json").getInputStream(),
            new TypeReference<>() {});
    when(strapiClient.userInfo(any()))
        .thenReturn(Mono.just(ResponseEntity.ok(strapiUser.getFirst())));

    when(accessTokenService.save(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(accessToken);

    TokenResponse tokenResponse = new TokenResponse();
    tokenResponse.set("id_token", "token");
    tokenResponse.set("access_token", "token");
    tokenResponse.set("token_type", "Bearer");
    Mockito.when(GoogleGrpcService.refreshGoogleOAuthToken(anyString(), anyString(), anyString()))
        .thenReturn(tokenResponse);

    GoogleIdToken.Payload google =
        mapper.readValue(
            new ClassPathResource("mock/GooglePayload.json").getInputStream(),
            GoogleIdToken.Payload.class);
    google.setExpirationTimeSeconds(3600000L);
    google.setIssuedAtTimeSeconds(360000L);

    Mockito.when(GoogleGrpcService.getUserInfo(anyString(), anyString())).thenReturn(google);

    when(strapiClient.getUserByEmail(any())).thenReturn(Mono.just(ResponseEntity.ok(strapiUser)));
    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.tokenOAuth(
            clientId,
            refresh_token,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            basic,
            exchange);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_logout_google() throws IOException, GeneralSecurityException {
    String bearer = "Bearer " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "GOOGLE-OAUTH-01";
    String redirect_uri = "http://localhost/redirect";

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDGoogle.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    String payload = "{\"access_token\": \"token\", \"refresh_token\": \"token\"}";

    AccessTokenData accessToken =
        new AccessTokenData(
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            clientId,
            SessionID.builder().generate(),
            null,
            SubjectType.CUSTOMER,
            "email@emial.com",
            "identifier",
            OAuthType.GOOGLE,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            mapper.readTree(payload),
            TokenStatus.ISSUED,
            List.of("DEFAULT"));

    when(accessTokenData.getClientId()).thenReturn(accessToken.getClientId());
    when(accessTokenData.getIdentifier()).thenReturn(accessToken.getIdentifier());
    when(accessTokenData.getPayload()).thenReturn(accessToken.getPayload());
    when(accessTokenData.getType()).thenReturn(accessToken.getType());

    Mockito.doNothing().when(accessTokenService).revokeTokenByIdentifier(anyString());

    when(strapiClient.logout(any())).thenReturn(Mono.just(ResponseEntity.ok(null)));

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/logout").build());

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.logout(clientId, redirect_uri, bearer, exchange.getResponse());

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void test_successfully_logout_bearer() throws IOException, GeneralSecurityException {
    String bearer = "Bearer " + Base64.getEncoder().encodeToString("user:user".getBytes());
    // Arrange
    String clientId = "BEARER-OAUTH-01";
    String redirect_uri = "http://localhost/redirect";

    StrapiResponse response =
        mapper.readValue(
            new ClassPathResource("mock/ClientIDBearer.json").getInputStream(),
            StrapiResponse.class);

    when(strapiClient.getClientByClientID(clientId))
        .thenReturn(Mono.just(ResponseEntity.ok(response)));

    String payload = "{\"access_token\": \"token\", \"refresh_token\": \"token\"}";

    AccessTokenData accessToken =
        new AccessTokenData(
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            TokenUtils.hashingToken("token"),
            clientId,
            SessionID.builder().generate(),
            null,
            SubjectType.CUSTOMER,
            "email@emial.com",
            "identifier",
            OAuthType.BEARER,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() + 3600000,
            mapper.readTree(payload),
            TokenStatus.ISSUED,
            List.of("DEFAULT"));

    when(accessTokenData.getClientId()).thenReturn(accessToken.getClientId());
    when(accessTokenData.getIdentifier()).thenReturn(accessToken.getIdentifier());
    when(accessTokenData.getPayload()).thenReturn(accessToken.getPayload());
    when(accessTokenData.getType()).thenReturn(accessToken.getType());

    Mockito.doNothing().when(accessTokenService).revokeTokenByIdentifier(anyString());

    when(strapiClient.logout(any())).thenReturn(Mono.just(ResponseEntity.ok(null)));

    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/logout").build());

    // Act
    Mono<ResponseEntity<?>> result =
        oAuthService.logout(clientId, redirect_uri, bearer, exchange.getResponse());

    // Assert
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertEquals(HttpStatus.OK, res.getStatusCode());
            })
        .verifyComplete();
  }
}
