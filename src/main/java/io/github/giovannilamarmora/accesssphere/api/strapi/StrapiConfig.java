package io.github.giovannilamarmora.accesssphere.api.strapi;

import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.webClient.WebClientRest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

public class StrapiConfig {
  public final Logger LOG = LoggerFilter.getLogger(this.getClass());
  public final WebClientRest webClientRest = new WebClientRest();

  @Value(value = "${rest.client.strapi.baseUrl}")
  public String strapiUrl;

  @Value(value = "${rest.client.strapi.bearer}")
  public String strapiToken;

  @Value(value = "${rest.client.strapi.path.clientId}")
  public String clientIdUrl;

  @Value(value = "${rest.client.strapi.path.registerUser}")
  public String registerUserUrl;

  @Value(value = "${rest.client.strapi.path.getUserByEmail}")
  public String getUserByEmailUrl;

  @Value(value = "${rest.client.strapi.path.login}")
  public String loginUrl;

  @Value(value = "${rest.client.strapi.path.getRefreshToken}")
  public String getRefreshTokenUrl;

  @Value(value = "${rest.client.strapi.path.userInfo}")
  public String userInfoUrl;

  @Value(value = "${rest.client.strapi.path.refreshToken}")
  public String refreshTokenUrl;

  @Value(value = "${rest.client.strapi.path.updateUser}")
  public String updateUserUrl;

  @Value(value = "${rest.client.strapi.path.getTemplate}")
  public String templateUrl;

  @Value(value = "${rest.client.strapi.path.locales}")
  public String localesUrl;

  @Value(value = "${rest.client.strapi.path.logout}")
  public String logoutUrl;

  @Autowired public WebClient.Builder builder;

  @PostConstruct
  void init() {
    webClientRest.setBaseUrl(strapiUrl);
    webClientRest.init(builder);
  }
}
