package io.github.giovannilamarmora.accesssphere.user.admin;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.OAuth2AuthorizationService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserRequest;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Servizio applicativo reattivo per la gestione amministrativa e il provisioning degli utenti IAM.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Verifica l'autorizzazione tecnica degli operatori tramite {@link AdminUserService} e {@link
 *       TechUserValidator}.
 *   <li>Risolve la configurazione dei client OAuth tramite {@link OAuth2ClientService}.
 *   <li>Esegue le operazioni di lettura, creazione e cancellazione delegando al layer di
 *       persistenza puro {@link UserService}.
 *   <li>Interagisce con {@link OAuth2AuthorizationService} per l'analisi dei privilegi di token
 *       Bearer in fase di registrazione.
 *   <li>Scatena eventi di notifica asincrona e webhook tramite {@link WebhookService} in caso di
 *       cancellazione utente.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

  private static final Logger LOG = LoggerFilter.getLogger(UserAdminService.class);

  private static final String USER_ADMIN_LOG = "User Admin";

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private UserService userService;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired private WebhookService webhookService;

  /**
   * Recupera la lista degli utenti registrati nel sistema IAM.
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore per recupero contesto.
   * @return Mono contenente la lista dei DTO {@link User} scoped in base all'identita'
   *     dell'operatore.
   */
  public Mono<List<UserDataResponse>> getUsers(String bearer) {
    SystemPermission readUserPermission = SystemPermission.USERS_READ;
    List<RoleDefinition> hasCorrectRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(readUserPermission);

    LOG.info("👥 {} Starting retrieval of users list", USER_ADMIN_LOG);

    return userService
        .getUsers()
        .map(
            users -> {
              if (permissionRoleManager.hasAdminPermission(readUserPermission)) {
                return users;
              }
              return users.stream().filter(user -> hasMatchingRole(user, hasCorrectRoles)).toList();
            })
        .map(UserMapper::toResponseList)
        .doOnSuccess(
            filteredUsers ->
                LOG.info(
                    "✅ {} Successfully retrieved {} users", USER_ADMIN_LOG, filteredUsers.size()));
  }

  /**
   * Registra un nuovo utente nel sistema IAM validando anagrafica, regex, password e ruoli
   * applicativi.
   *
   * <p>Interconnessioni logiche e controlli di sicurezza:
   *
   * <ul>
   *   <li>Se il token {@code bearer} e' fornito e l'istanza {@link OAuth2Authorization} non e'
   *       inizializzata, sincronizza il contesto di autorizzazione tramite {@link
   *       OAuth2AuthorizationService#getByAccessTokenOrIdToken(String)}.
   *   <li><b>Operatore Autenticato</b>: se l'operatore e' loggato (token valido presente), verifica
   *       il possesso del ruolo/permesso di registrazione delegata tramite {@link
   *       PermissionRoleManager#checkPermissionAndReturnUserRoles(SystemPermission)} con {@link
   *       SystemPermission#USERS_WRITE}. Gli operatori con tale permesso possono creare utenti
   *       bypassando il blocco preventivo di registrazione dei client.
   *   <li><b>Registrazione Pubblica (non autenticata)</b>: verifica che ciascun Client OAuth2
   *       associato ai ruoli richiesti sia censito e possieda la registrazione autonoma abilitata
   *       ({@link UserValidator#validateClientRegistrationEnabled(OAuth2Client)}), sollevando
   *       altrimenti {@link OAuthException} con codice {@code ERR_OAUTH_403}.
   *   <li><b>Salvataggio ed Arricchimento</b>: delega interamente la creazione dell'utente a {@link
   *       UserService#saveUser(User)}, che gestisce in modo transazionale e sicuro la validazione
   *       dell'unicita di username ed email, l'hashing BCrypt della password, la generazione
   *       dell'identificatore UUID e l'arricchimento con i permessi definiti in {@code
   *       ROLE_DEFINITIONS}.
   * </ul>
   *
   * @param bearer Token Bearer opzionale per operazioni privilegiate da operatore tecnico o
   *     amministratore.
   * @param userRequest DTO contenente i dati anagrafici, credenziali e ruoli del nuovo utente.
   * @return Mono contenente il DTO {@link UserDataResponse} registrato e salvato.
   */
  public Mono<UserDataResponse> saveUser(String bearer, UserRequest userRequest) {
    boolean isAuthenticated =
        !ObjectToolkit.isNullOrEmpty(bearer)
            && !(ObjectToolkit.isNullOrEmpty(oAuth2Authorization)
                || ObjectToolkit.fieldsNullOrEmpty(oAuth2Authorization));

    if (isAuthenticated) {
      permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.USERS_WRITE);
    }

    LOG.info(
        "👤 {} Starting user registration for username: {}, email: {}",
        USER_ADMIN_LOG,
        userRequest.getUsername(),
        userRequest.getEmail());

    UserValidator.validateUserRequest(userRequest);
    User user = UserMapper.toModel(userRequest);

    Mono<Void> registrationValidationMono = Mono.empty();
    if (!isAuthenticated) {
      List<String> clientIds =
          user.getRoles().stream()
              .map(UserClientRole::getClientId)
              .filter(StringUtils::hasText)
              .distinct()
              .toList();

      registrationValidationMono =
          oAuth2ClientDomainService
              .getOAuth2Clients()
              .flatMap(
                  oAuth2Clients -> {
                    Map<String, OAuth2Client> clientMap =
                        oAuth2Clients.stream()
                            .filter(c -> !ObjectToolkit.isNullOrEmpty(c.getClientId()))
                            .collect(
                                Collectors.toMap(
                                    c -> c.getClientId().trim().toUpperCase(),
                                    c -> c,
                                    (existing, replacement) -> existing));

                    List<RoleDefinition> enforcedRoleDefinitions = new ArrayList<>();
                    clientIds.forEach(
                        clientId -> {
                          OAuth2Client client = clientMap.get(clientId.trim().toUpperCase());
                          if (ObjectToolkit.isNullOrEmpty(client)) {
                            LOG.error("❌ Client {} not found", clientId);
                            throw new OAuthException(
                                ExceptionMap.ERR_OAUTH_400, "Invalid client_id: " + clientId);
                          }
                          UserValidator.validateClientRegistrationEnabled(client);

                          // Ricerca della definizione di ruolo applicativo con tipologia USER sul
                          // client target
                          List<RoleDefinition> clientAppRoles =
                              !ObjectToolkit.isNullOrEmpty(client.getAppRoles())
                                  ? client.getAppRoles()
                                  : List.of();
                          RoleDefinition userRoleDef =
                              clientAppRoles.stream()
                                  .filter(ar -> !ObjectToolkit.isNullOrEmpty(ar) && ar.isUser())
                                  .findFirst()
                                  .orElse(null);

                          // Se il client non ha alcun ruolo censito di tipo USER, blocca la
                          // registrazione anonima
                          if (ObjectToolkit.isNullOrEmpty(userRoleDef)) {
                            LOG.error(
                                "❌ Registration rejected: Client {} does not have a role configured with type USER",
                                client.getClientId());
                            throw new OAuthException(
                                ExceptionMap.ERR_OAUTH_403,
                                "Registration blocked: Client %s does not have a role configured with type USER"
                                    .formatted(client.getClientId()));
                          }

                          // Garantisce la presenza del clientId nella definizione del ruolo
                          if (ObjectToolkit.isNullOrEmpty(userRoleDef.getClientId())) {
                            userRoleDef.setClientId(client.getClientId());
                          }

                          enforcedRoleDefinitions.add(userRoleDef);
                        });

                    // Conversione tramite mapper e sovrascrittura con ruoli verificati e blindati
                    // con tipologia USER
                    user.setRoles(
                        UserClientRoleMapper.toUserClientModels(
                            enforcedRoleDefinitions, user.getIdentifier()));
                    return Mono.empty();
                  });
    }

    return registrationValidationMono
        .then(Mono.defer(() -> userService.saveUser(user)).subscribeOn(Schedulers.boundedElastic()))
        .map(UserMapper::toResponse)
        .doOnSuccess(
            registeredUser ->
                LOG.info(
                    "✅ {} Registration successfully completed for username: {}, email: {}",
                    USER_ADMIN_LOG,
                    registeredUser.getUsername(),
                    registeredUser.getEmail()));
  }

  /**
   * Blocca o sblocca lo stato di accesso di un utente.
   *
   * @param identifier UUID univoco dell'utente da bloccare o sbloccare.
   * @param block True per bloccare l'accesso, false per sbloccarlo.
   * @return Mono contenente il DTO {@link User} aggiornato con il nuovo stato.
   */
  public Mono<UserDataResponse> unlockUser(String identifier, Boolean block) {
    SystemPermission lockUserPermission = SystemPermission.USERS_LOCK;
    permissionRoleManager.checkPermissionAndReturnUserRoles(lockUserPermission);

    LOG.info(
        "🛡️ {} Starting user {} process for identifier: {}",
        USER_ADMIN_LOG,
        block ? "lock" : "unlock",
        identifier);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            targetUser -> {
              if (permissionRoleManager.isAdminUser(targetUser.getIdentifier())) {
                LOG.error(
                    "❌ Forbidden: User cannot change lock status of admin user {}", identifier);
                return Mono.error(
                    new OAuthException(
                        ExceptionMap.ERR_OAUTH_403, "Cannot alter admin user status"));
              }
              if (permissionRoleManager.isCurrentUser(targetUser.getIdentifier())) {
                LOG.error(
                    "❌ Forbidden: User cannot change lock status of the current user {}",
                    identifier);
                return Mono.error(
                    new OAuthException(
                        ExceptionMap.ERR_OAUTH_403, "Cannot alter current user status"));
              }

              targetUser.setPassword(null);
              targetUser.setBlocked(block);
              WebhookAction action =
                  Boolean.TRUE.equals(block) ? WebhookAction.LOCK_USER : WebhookAction.UNLOCK_USER;
              return userService
                  .updateUser(identifier, targetUser)
                  .map(UserMapper::toResponse)
                  .flatMap(
                      resp ->
                          webhookService
                              .checkAndExecuteWebhook(action, identifier)
                              .thenReturn(resp));
            })
        .doOnSuccess(
            updatedUser ->
                LOG.info(
                    "✅ {} User {} process completed successfully for identifier: {}",
                    USER_ADMIN_LOG,
                    block ? "lock" : "unlock",
                    identifier));
  }

  /**
   * Cancella definitivamente un utente dal sistema IAM ed esegue il webhook associato.
   *
   * @param identifier UUID univoco dell'utente da cancellare.
   * @param bearer Token di autorizzazione del chiamante.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono vuoto indicante il completamento della cancellazione.
   */
  public Mono<Void> deleteUser(String identifier, String bearer, ServerWebExchange exchange) {
    SystemPermission deleteUserPermission = SystemPermission.USERS_DELETE;
    permissionRoleManager.checkPermissionAndReturnUserRoles(deleteUserPermission);

    LOG.info(
        "🗑️ {} Starting user deletion process for identifier: {}", USER_ADMIN_LOG, identifier);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            targetUser -> {
              if (permissionRoleManager.isAdminUser(targetUser.getUsername())) {
                LOG.error(
                    "❌ Forbidden: User cannot delete the admin user {}", identifier);
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_403, "Cannot alter admin user");
              }
              if (permissionRoleManager.isCurrentUser(targetUser.getIdentifier())) {
                LOG.error(
                    "❌ Forbidden: User cannot delete the current user {}", identifier);
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_403, "Cannot alter current user status");
              }

              return userService
                  .deleteUserByIdentifier(targetUser.getIdentifier(), bearer, exchange)
                  .subscribeOn(Schedulers.boundedElastic());
            })
        .doOnSuccess(
            unused ->
                LOG.info(
                    "✅ {} User deletion successfully completed for identifier: {}",
                    USER_ADMIN_LOG,
                    identifier))
        .then();
  }

  /**
   * Verifica se l'utente specificato appartiene ad almeno uno dei Client OAuth2 autorizzati per
   * l'operatore. Il filtraggio agisce a livello di perimetro del client (clientId), includendo
   * tutti gli utenti registrati sul client a prescindere dallo specifico ruolo assegnato.
   *
   * @param user Utente da verificare con i rispettivi ruoli assegnati.
   * @param authorizedRoles Lista dei ruoli ammessi da cui estrarre i Client autorizzati per
   *     l'operatore.
   * @return true se l'utente possiede almeno un ruolo associato a uno dei client autorizzati, false
   *     altrimenti.
   */
  private boolean hasMatchingRole(User user, List<RoleDefinition> authorizedRoles) {
    if (ObjectToolkit.isNullOrEmpty(user) || ObjectToolkit.isNullOrEmpty(user.getRoles())) {
      return false;
    }
    if (ObjectToolkit.isNullOrEmpty(authorizedRoles)) {
      return false;
    }
    return user.getRoles().stream()
        .anyMatch(
            userRole ->
                authorizedRoles.stream()
                    .anyMatch(
                        correctRole -> {
                          if (ObjectToolkit.isNullOrEmpty(userRole)
                              || ObjectToolkit.isNullOrEmpty(correctRole)) {
                            return false;
                          }
                          return !ObjectToolkit.isNullOrEmpty(userRole.getClientId())
                              && !ObjectToolkit.isNullOrEmpty(correctRole.getClientId())
                              && correctRole
                                  .getClientId()
                                  .trim()
                                  .equalsIgnoreCase(userRole.getClientId().trim());
                        }));
  }
}
