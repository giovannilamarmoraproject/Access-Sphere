package io.github.giovannilamarmora.accesssphere.data.roles;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private ClientService clientService;
  @Autowired private UserService userService;
  @Autowired private TechUserService techUserService;

  /**
   * Modifica i ruoli assegnati a un determinato utente, verificando che i ruoli esistano nei client
   * registrati. L'operazione e' consentita esclusivamente a un utente tecnico autorizzato.
   *
   * @param identifier Identificatore univoco dell'utente.
   * @param roles Oggetto contenente la lista dei ruoli da assegnare.
   * @return Mono contenente la risposta HTTP con l'utente aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changeRoles(String identifier, UserRoles roles) {
    LOG.info("👥 [ROLES] Changing user roles process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can change the user roles {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    Mono<List<ClientCredential>> getClients = clientService.getClientCredentials();

    return getClients
        .flatMap(
            clientCredentials -> {
              List<String> validRoles =
                  clientCredentials.stream()
                      .filter(c -> c.getAppRoles() != null)
                      .flatMap(clientCredential -> clientCredential.getAppRoles().stream())
                      .filter(java.util.Objects::nonNull)
                      .map(AppRole::getRole)
                      .filter(java.util.Objects::nonNull)
                      .filter(roleName -> roles.getRoles() != null && roles.getRoles().stream().anyMatch(roleName::equalsIgnoreCase))
                      .distinct()
                      .toList();
              return userService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setRoles(validRoles);
                        return userService
                            .updateUser(identifier, user)
                            .map(
                                updatedUser -> {
                                  Response response =
                                      new Response(
                                          HttpStatus.OK.value(),
                                          "User roles for "
                                              + updatedUser.getUsername()
                                              + " successfully changed!",
                                          TraceUtils.getSpanID(),
                                          updatedUser);
                                  return ResponseEntity.ok(response);
                                });
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [ROLES] Changing user roles process ended, identifier: {}", identifier));
  }
}
