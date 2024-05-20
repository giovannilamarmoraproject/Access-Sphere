package io.github.giovannilamarmora.accesssphere.api.strapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiLogin;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.webClient.UtilsUriBuilder;
import io.github.giovannilamarmora.utils.webClient.WebClientRest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Logged
public class StrapiClient {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
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
}
