package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TechUserService extends TechUserConfig {

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean checkIfTechUser(String username, String password, String clientId) {
    return username.equalsIgnoreCase(techUsername)
        && password.equalsIgnoreCase(techPassword)
        && clientId.equalsIgnoreCase(techClientId);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechUser() {
    return isTechUser(accessTokenData);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechUser(AccessTokenData tokenData) {
    if (ObjectToolkit.isNullOrEmpty(tokenData)
        || ObjectToolkit.isNullOrEmpty(tokenData.getSubjectType())
        || ObjectToolkit.isNullOrEmpty(tokenData.getClientId())
        || ObjectToolkit.fieldsNullOrEmpty(tokenData)) return false;
    return tokenData.getSubjectType().equals(SubjectType.TECHNICAL)
        || tokenData.getClientId().equalsIgnoreCase(techClientId);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Boolean> hasTechUserRoles(
      AccessTokenData accessTokenData, ClientService clientService) {
    if (accessTokenData.getSubjectType().equals(SubjectType.TECHNICAL)) {
      logTechRole(accessTokenData.getSubject());
      return Mono.just(true);
    }
    return clientService
        .getClientCredentialByClientID(techClientId)
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
