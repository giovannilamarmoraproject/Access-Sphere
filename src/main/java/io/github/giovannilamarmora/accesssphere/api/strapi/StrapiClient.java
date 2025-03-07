package io.github.giovannilamarmora.accesssphere.api.strapi;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiLogin;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.webClient.UtilsUriBuilder;
import io.github.giovannilamarmora.utils.webClient.WebClientRest;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Logged
public class StrapiClient {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final WebClientRest webClientRest = new WebClientRest();

  @Value(value = "${rest.client.strapi.baseUrl}")
  private String strapiUrl;

  @Value(value = "${rest.client.strapi.bearer}")
  private String strapiToken;

  @Value(value = "${rest.client.strapi.path.clientId}")
  private String clientIdUrl;

  @Value(value = "${rest.client.strapi.path.registerUser}")
  private String registerUserUrl;

  @Value(value = "${rest.client.strapi.path.getUserByEmail}")
  private String getUserByEmailUrl;

  @Value(value = "${rest.client.strapi.path.login}")
  private String loginUrl;

  @Value(value = "${rest.client.strapi.path.getRefreshToken}")
  private String getRefreshTokenUrl;

  @Value(value = "${rest.client.strapi.path.userInfo}")
  private String userInfoUrl;

  @Value(value = "${rest.client.strapi.path.refreshToken}")
  private String refreshTokenUrl;

  @Value(value = "${rest.client.strapi.path.updateUser}")
  private String updateUserUrl;

  @Value(value = "${rest.client.strapi.path.getTemplate}")
  private String templateUrl;

  @Value(value = "${rest.client.strapi.path.logout}")
  private String logoutUrl;

  @Autowired private WebClient.Builder builder;

  @PostConstruct
  void init() {
    webClientRest.setBaseUrl(strapiUrl);
    webClientRest.init(builder);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> getClientByClientID(String clientID) {
    Map<String, Object> params = new HashMap<>();
    params.put("filters[clientId][$eq]", clientID);
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.perform(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(clientIdUrl, params),
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> getClients() {
    Map<String, Object> params = new HashMap<>();
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.perform(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(clientIdUrl, params),
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> saveUser(StrapiUser user) {
    Map<String, Object> params = new HashMap<>();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    // headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.perform(
        HttpMethod.POST,
        UtilsUriBuilder.buildUri(registerUserUrl, params),
        user,
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<List<StrapiUser>>> getUserByEmail(String email) {
    Map<String, Object> params = new HashMap<>();
    params.put("filters[email][$eq]", email);
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.performList(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(getUserByEmailUrl, params),
        headers,
        StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<List<StrapiUser>>> getUsers() {
    Map<String, Object> params = new HashMap<>();
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.performList(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(getUserByEmailUrl, params),
        headers,
        StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<List<StrapiUser>>> getUserByIdentifier(String identifier) {
    Map<String, Object> params = new HashMap<>();
    params.put("filters[identifier][$eq]", identifier);
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.performList(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(getUserByEmailUrl, params),
        headers,
        StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> login(StrapiLogin login) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return webClientRest.perform(
        HttpMethod.POST,
        UtilsUriBuilder.buildUri(loginUrl, null),
        login,
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> getRefreshToken(StrapiLogin login) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return webClientRest.perform(
        HttpMethod.POST,
        UtilsUriBuilder.buildUri(getRefreshTokenUrl, null),
        login,
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiUser>> userInfo(String bearer) {
    Map<String, Object> params = new HashMap<>();
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);

    return webClientRest.perform(
        HttpMethod.GET, UtilsUriBuilder.buildUri(userInfoUrl, params), headers, StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> refreshToken(String refresh_token) {
    Map<String, Object> body = new HashMap<>();
    body.put("token", refresh_token);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return webClientRest.perform(
        HttpMethod.POST,
        UtilsUriBuilder.buildUri(refreshTokenUrl, null),
        body,
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiUser>> updateUser(StrapiUser user) {
    Map<String, Object> query = new HashMap<>();
    query.put("userId", user.getId());

    String updateUser = UtilsUriBuilder.toBuild().set(updateUserUrl, query).getStringUri();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.perform(
        HttpMethod.PUT,
        UtilsUriBuilder.buildUri(updateUser, null),
        user,
        headers,
        StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<StrapiResponse>> getTemplateById(String templateId, String locale) {
    Map<String, Object> params = new HashMap<>();
    params.put("filters[identifier][$eq]", templateId);
    params.put("locale", locale);
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.perform(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(templateUrl, params),
        headers,
        StrapiResponse.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<List<StrapiUser>>> getUserByTokenReset(String tokenReset) {
    Map<String, Object> params = new HashMap<>();
    params.put("filters[tokenReset][$eq]", tokenReset);
    params.put("populate", "*");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + strapiToken);

    return webClientRest.performList(
        HttpMethod.GET,
        UtilsUriBuilder.buildUri(getUserByEmailUrl, params),
        headers,
        StrapiUser.class);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<Void>> logout(String refresh_token) {
    Map<String, Object> body = new HashMap<>();
    body.put("token", refresh_token);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return webClientRest.perform(
        HttpMethod.POST, UtilsUriBuilder.buildUri(logoutUrl, null), body, headers, Void.class);
  }
}
