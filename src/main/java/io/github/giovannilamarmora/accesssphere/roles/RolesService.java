package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.ClientMapper;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserConfig;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Servizio di dominio per l'amministrazione dei ruoli e permessi utente (RBAC).
 *
 * <p>Interconnessioni logiche:
 * <ul>
 *   <li>Persiste le relazioni utente-client-ruolo su tabella relazionale {@code USER_CLIENT_ROLE} via {@link IUserClientRoleDAO}.
 *   <li>Mantiene la retrocompatibilita al 100% sincronizzando la colonna denormalizzata legacy {@code USERS.ROLES} via {@link UserService}.
 *   <li>Risolve la validita dei ruoli dai client OAuth2 registrati tramite {@link ClientService} e {@link IClientDAO}.
 *   <li>Consente l'esecuzione delle operazioni esclusivamente a operatori autenticati con ruolo tecnico ({@link TechUserService}).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private ClientService clientService;
  @Autowired private UserService userService;
  @Autowired private TechUserService techUserService;
  @Autowired private TechUserConfig techUserConfig;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IClientDAO clientDAO;

  /**
   * Modifica i ruoli assegnati a un determinato utente, persistendo le associazioni nella tabella relazionale
   * {@code USER_CLIENT_ROLE} e mantenendo allineata per retrocompatibilita la lista flat legacy in {@code USERS.ROLES}.
   * Supporta sia il formato moderno basato su {@code clientRoles} (client-scoped con permessi aggregati), sia
   * il formato legacy basato su array flat di stringhe {@code roles}.
   * L'operazione e consentita esclusivamente a un utente tecnico autorizzato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param roles Oggetto contenente la lista dei ruoli da assegnare (clientRoles e/o roles legacy).
   * @return Mono contenente la risposta HTTP con l'utente aggiornato e arricchito.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changeRoles(String identifier, UserRoles roles) {
    LOG.info("👥 [ROLES] Changing user roles process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can change the user roles {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String techClientId = techUserConfig.getTechClientId();
    Mono<List<ClientCredential>> getClients = clientService.getClientCredentials();

    return getClients
        .flatMap(
            clientCredentials -> {
              // 1. Raccoglie tutti i ruoli ammessi dai client registrati (nome ruolo e tipologia applicativa)
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

              // 2. Raccoglie i ruoli canonici/di sistema definiti a livello di piattaforma (UserRole e AppRoleType)
              Arrays.stream(UserRole.values())
                  .forEach(
                      ur -> {
                        allowedRoles.add(ur.name().toUpperCase());
                        allowedRoles.add("ROLE_" + ur.name().toUpperCase());
                      });
              Arrays.stream(AppRoleType.values())
                  .forEach(art -> allowedRoles.add(art.getValue().toUpperCase()));
              allowedRoles.add("ROLE_TECH");

              List<UserClientRoleEntity> entitiesToSave = new ArrayList<>();
              List<String> distinctRoleNames = new ArrayList<>();

              // CASO A: Ricevuto formato moderno 'clientRoles'
              if (roles != null && roles.getClientRoles() != null && !roles.getClientRoles().isEmpty()) {
                LOG.info("👥 [ROLES] Processing modern clientRoles payload with {} entries", roles.getClientRoles().size());
                for (UserClientRoleDTO cr : roles.getClientRoles()) {
                  if (cr == null || cr.getRole() == null || cr.getRole().isBlank()) continue;
                  String targetClientId = (cr.getClientId() != null && !cr.getClientId().isBlank())
                      ? cr.getClientId().trim()
                      : techClientId;
                  String roleName = cr.getRole().trim().toUpperCase();

                  boolean valid = allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(roleName));
                  if (!valid) {
                    LOG.warn("⚠️ [ROLES] Discarding unrecognized client-scoped role [{}] for client [{}]", roleName, targetClientId);
                    continue;
                  }

                  boolean alreadyAdded = entitiesToSave.stream()
                      .anyMatch(e -> e.getClientId().equalsIgnoreCase(targetClientId) && e.getRole().equalsIgnoreCase(roleName));
                  if (!alreadyAdded) {
                    entitiesToSave.add(new UserClientRoleEntity(identifier, targetClientId, roleName));
                  }
                  if (!distinctRoleNames.contains(roleName)) {
                    distinctRoleNames.add(roleName);
                  }
                }
              }
              // CASO B: Ricevuto formato legacy 'roles' (array flat di stringhe)
              else if (roles != null && roles.getRoles() != null && !roles.getRoles().isEmpty()) {
                LOG.info("👥 [ROLES] Processing legacy roles payload with {} entries", roles.getRoles().size());
                List<String> validLegacyRoles = roles.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(r -> !r.isBlank())
                    .filter(r -> {
                      boolean valid = allowedRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(r));
                      if (!valid) {
                        LOG.warn("⚠️ [ROLES] Discarding unrecognized role [{}] for user {}", r, identifier);
                      }
                      return valid;
                    })
                    .map(String::toUpperCase)
                    .distinct()
                    .toList();

                for (String r : validLegacyRoles) {
                  // Determina a quale client appartiene il ruolo
                  String targetClientId = techClientId;
                  if (clientCredentials != null) {
                    for (ClientCredential cc : clientCredentials) {
                      if (cc.getAppRoles() != null) {
                        boolean match = cc.getAppRoles().stream().anyMatch(ar -> ar != null && ar.matches(r));
                        if (match) {
                          targetClientId = cc.getClientId();
                          break;
                        }
                      }
                    }
                  }

                  entitiesToSave.add(new UserClientRoleEntity(identifier, targetClientId, r));
                  distinctRoleNames.add(r);
                }
              }

              // 3. Sostituzione atomica nella tabella relazionale USER_CLIENT_ROLE
              userClientRoleDAO.deleteByUserIdentifier(identifier);
              if (!entitiesToSave.isEmpty()) {
                userClientRoleDAO.saveAll(entitiesToSave);
                LOG.info("💾 [ROLES] Persisted {} entries in USER_CLIENT_ROLE for user {}", entitiesToSave.size(), identifier);
              }

              // 4. Sincronizzazione per retrocompatibilita su USERS.ROLES
              return userService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setRoles(distinctRoleNames);
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

  /**
   * Assegna un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da assegnare.
   * @return Mono contenente la risposta HTTP con l'utente aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> assignClientRole(
      String identifier, String clientId, String role) {
    LOG.info("👥 [ROLES] Assigning role [{}] for client [{}] to user [{}]", role, clientId, identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can assign client roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedRole = role.trim().toUpperCase();
    String normalizedClientId = clientId.trim();

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            user -> {
              boolean alreadyExists =
                  userClientRoleDAO.existsByUserIdentifierAndClientIdAndRole(
                      identifier, normalizedClientId, normalizedRole);
              if (!alreadyExists) {
                userClientRoleDAO.save(
                    new UserClientRoleEntity(identifier, normalizedClientId, normalizedRole));
                LOG.info(
                    "💾 [ROLES] Successfully created USER_CLIENT_ROLE mapping ({}, {}, {})",
                    identifier,
                    normalizedClientId,
                    normalizedRole);
              }

              // Sincronizza campo flat legacy
              List<String> currentRoles = new ArrayList<>(user.getRoles() != null ? user.getRoles() : List.of());
              if (!currentRoles.contains(normalizedRole)) {
                currentRoles.add(normalizedRole);
                user.setRoles(currentRoles);
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role " + normalizedRole + " successfully assigned for client " + normalizedClientId,
                                TraceUtils.getSpanID(),
                                updatedUser);
                        return ResponseEntity.ok(response);
                      });
            });
  }

  /**
   * Revoca un singolo ruolo contestualizzato a uno specifico Client OAuth2 per l'utente indicato.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param clientId Identificativo univoco del client OAuth2 target.
   * @param role Nome del ruolo da revocare.
   * @return Mono contenente la risposta HTTP con l'utente aggiornato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> revokeClientRole(
      String identifier, String clientId, String role) {
    LOG.info("👥 [ROLES] Revoking role [{}] for client [{}] from user [{}]", role, clientId, identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can revoke client roles");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    String normalizedRole = role.trim().toUpperCase();
    String normalizedClientId = clientId.trim();

    userClientRoleDAO.deleteByUserIdentifierAndClientIdAndRole(
        identifier, normalizedClientId, normalizedRole);
    LOG.info(
        "🗑️ [ROLES] Deleted USER_CLIENT_ROLE mapping ({}, {}, {})",
        identifier,
        normalizedClientId,
        normalizedRole);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            user -> {
              // Verifica se il ruolo rimosso e ancora assegnato a un altro client per questo utente
              List<UserClientRoleEntity> remainingUserRoles =
                  userClientRoleDAO.findByUserIdentifier(identifier);
              boolean roleStillPresentInOtherClient =
                  remainingUserRoles.stream()
                      .anyMatch(r -> r.getRole().equalsIgnoreCase(normalizedRole));

              if (!roleStillPresentInOtherClient && user.getRoles() != null) {
                List<String> updatedLegacyRoles =
                    user.getRoles().stream()
                        .filter(r -> !r.equalsIgnoreCase(normalizedRole))
                        .toList();
                user.setRoles(updatedLegacyRoles);
              }

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Role " + normalizedRole + " successfully revoked for client " + normalizedClientId,
                                TraceUtils.getSpanID(),
                                updatedUser);
                        return ResponseEntity.ok(response);
                      });
            });
  }
}
