package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
   * registrati (sia come nome ruolo che come tipologia) oppure tra i ruoli canonici di sistema
   * ({@link UserRole} e {@link AppRoleType}). L'operazione e' consentita esclusivamente a un utente
   * tecnico autorizzato.
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
              // 1. Raccoglie tutti i ruoli ammessi dai client registrati (nome ruolo e tipologia
              // applicativa)
              Set<String> allowedRoles = new HashSet<>();
              if (clientCredentials != null) {
                clientCredentials.forEach(
                    c -> {
                      if (c.getAppRoles() != null) {
                        c.getAppRoles()
                            .forEach(
                                r -> {
                                  if (r != null) {
                                    if (r.getRole() != null)
                                      allowedRoles.add(r.getRole().toUpperCase());
                                    if (r.getType() != null)
                                      allowedRoles.add(r.getType().toUpperCase());
                                  }
                                });
                      }
                    });
              }

              // 2. Raccoglie i ruoli canonici/di sistema definiti a livello di piattaforma
              // (UserRole e AppRoleType)
              Arrays.stream(UserRole.values())
                  .forEach(ur -> allowedRoles.add(ur.name().toUpperCase()));
              Arrays.stream(AppRoleType.values())
                  .forEach(art -> allowedRoles.add(art.getValue().toUpperCase()));

              // 3. Valida e filtra i ruoli richiesti: rimuove duplicati, scarta non riconosciuti
              // con warning
              List<String> validRoles =
                  (roles != null && roles.getRoles() != null)
                      ? roles.getRoles().stream()
                          .filter(Objects::nonNull)
                          .map(String::trim)
                          .filter(r -> !r.isBlank())
                          .filter(
                              r -> {
                                boolean valid =
                                    allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(r));
                                if (!valid) {
                                  LOG.warn(
                                      "⚠️ [ROLES] Discarding unrecognized role [{}] for user {}",
                                      r,
                                      identifier);
                                }
                                return valid;
                              })
                          .map(String::toUpperCase)
                          .distinct()
                          .toList()
                      : List.of();

              LOG.info(
                  "👥 [ROLES] Validated roles for user {}: requested={}, accepted={}",
                  identifier,
                  roles != null ? roles.getRoles() : null,
                  validRoles);

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
