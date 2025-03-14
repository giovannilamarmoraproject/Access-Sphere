package io.github.giovannilamarmora.accesssphere.data.tech;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TechUserService {

  @Value(value = "${app.tech-user.username}")
  private String tech_username;

  @Value(value = "${app.tech-user.password}")
  private String tech_password;

  @Getter
  @Value(value = "${app.tech-user.client-id}")
  private String tech_client_id;

  @Getter
  @Value(value = "${app.tech-user.strapi}")
  private String tech_token;

  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenData accessTokenData;
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String TECH_ROLE_LOG = "The subject {} has Technical Roles";

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean checkIfTechUser(String username, String password, String client_id) {
    return username.equalsIgnoreCase(tech_username)
        && password.equalsIgnoreCase(tech_password)
        && client_id.equalsIgnoreCase(tech_client_id);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechUser() {
    return accessTokenData.getSubjectType().equals(SubjectType.TECHNICAL);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Boolean> hasTechUserRoles(
      AccessTokenData accessTokenData, ClientService clientService) {
    if (accessTokenData.getSubjectType().equals(SubjectType.TECHNICAL)) {
      logTechRole(accessTokenData.getSubject());
      return Mono.just(true);
    }
    return clientService
        .getClientCredentialByClientID(tech_client_id)
        .map(
            clientCredential ->
                clientCredential.getAppRoles().stream()
                    .map(AppRole::getRole)
                    .anyMatch(accessTokenData.getRoles()::contains))
        .doOnSuccess(
            aBoolean -> {
              if (aBoolean) logTechRole(accessTokenData.getSubject());
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Boolean hasTechUserRoles(
      AccessTokenData accessTokenData, ClientCredential clientCredential) {
    if (accessTokenData.getSubjectType().equals(SubjectType.TECHNICAL)) {
      logTechRole(accessTokenData.getSubject());
      return true;
    }
    boolean hasTechRole =
        clientCredential.getAppRoles().stream()
            .map(AppRole::getRole)
            .anyMatch(accessTokenData.getRoles()::contains);
    if (hasTechRole) logTechRole(accessTokenData.getSubject());
    return hasTechRole;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> loginTechUser(
      String username, ClientCredential clientCredential, ServerHttpRequest request) {
    LOG.info("Performing Tech User login for client {}", clientCredential.getClientId());
    User user = UserMapper.getTechUser(username, clientCredential);
    JWTData jwtData =
        JWTData.generateJWTData(user, clientCredential, SubjectType.TECHNICAL, request);
    Map<String, Object> strapiToken = new HashMap<>();
    strapiToken.put(TokenData.STRAPI_ACCESS_TOKEN.getToken(), tech_token);
    strapiToken.put("token_type", "Bearer");
    AuthToken token = tokenService.generateToken(jwtData, clientCredential, strapiToken);
    return Mono.just(
        new OAuthTokenResponse(
            token, Utils.mapper().convertValue(strapiToken, JsonNode.class), jwtData, user));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void validateTechClient(List<ClientCredential> clients) {
    String client_id = tech_client_id;
    ClientCredential clientCredentialToCheck =
        clients.stream()
            .filter(c -> c.getClientId().equalsIgnoreCase(client_id))
            .toList()
            .getFirst();
    OAuthValidator.validateUserRoles(clientCredentialToCheck, accessTokenData.getRoles());
    String strapi_token = OAuthMapper.getStrapiAccessToken(accessTokenData);
    if (ObjectToolkit.isNullOrEmpty(strapi_token)) {
      LOG.error("Strapi token not found for user {}", accessTokenData.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403);
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> userInfo(JWTData jwtData) {
    LOG.info("Performing Tech UserInfo for subject {}", jwtData.getSub());
    User user = UserMapper.getTechUser(jwtData);
    return Mono.just(user);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private void logTechRole(String subject) {
    LOG.info(TECH_ROLE_LOG, subject);
  }
}
