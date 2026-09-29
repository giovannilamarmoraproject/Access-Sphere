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
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.UserException;
import io.github.giovannilamarmora.accesssphere.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserRequest;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
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
@Logged
@RequiredArgsConstructor
public class UserAdminService {

  private static final Logger LOG = LoggerFilter.getLogger(UserAdminService.class);
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  private static final String USER_ADMIN_LOG = "User Admin";

  @Autowired private AdminUserService techUserService;
  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private UserService userService;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private OAuth2AuthorizationService oAuth2AuthorizationService;
  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired @Lazy private WebhookService webhookService;

  /**
   * Recupera la lista degli utenti registrati nel sistema IAM.
   *
   * <p>Interconnessioni logiche:
   *
   * <ul>
   *   <li>Se l'operatore autenticato e' l'utente tecnico ({@link AdminUserService#isTechUser()}),
   *       valida i ruoli tecnici su {@link OAuth2ClientService} e restituisce l'elenco completo di
   *       tutti gli utenti da {@link UserService}.
   *   <li>Se l'operatore autenticato e' un utente normale/amministratore delegato, restituisce
   *       l'elenco filtrato escludendo l'utenza tecnica di sistema ({@link
   *       AdminUserService#isTechnicalUser(User)}), garantendone l'invisibilita' e la protezione da
   *       manomissioni o lockout accidentali.
   * </ul>
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore per recupero contesto.
   * @return Mono contenente la lista dei DTO {@link User} scoped in base all'identita'
   *     dell'operatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
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
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
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

                    List<UserClientRole> enforcedUserRoles = new ArrayList<>();
                    clientIds.forEach(
                        clientId -> {
                          OAuth2Client client = clientMap.get(clientId.trim().toUpperCase());
                          if (ObjectToolkit.isNullOrEmpty(client)) {
                            LOG.error("❌ Client {} not found", clientId);
                            throw new OAuthException(
                                ExceptionMap.ERR_OAUTH_400, "Invalid client_id: " + clientId);
                          }
                          UserValidator.validateClientRegistrationEnabled(client);

                          // Ricerca della definizione di ruolo applicativo con tipologia USER sul client target
                          List<RoleDefinition> clientAppRoles =
                              !ObjectToolkit.isNullOrEmpty(client.getAppRoles())
                                  ? client.getAppRoles()
                                  : List.of();
                          RoleDefinition userRoleDef =
                              clientAppRoles.stream()
                                  .filter(ar -> !ObjectToolkit.isNullOrEmpty(ar) && ar.isUser())
                                  .findFirst()
                                  .orElse(null);

                          // Se il client non ha alcun ruolo censito di tipo USER, blocca la registrazione anonima
                          if (ObjectToolkit.isNullOrEmpty(userRoleDef)) {
                            LOG.error(
                                "❌ Registration rejected: Client {} does not have a role configured with type USER",
                                client.getClientId());
                            throw new OAuthException(
                                ExceptionMap.ERR_OAUTH_403,
                                "Registration blocked: Client %s does not have a role configured with type USER"
                                    .formatted(client.getClientId()));
                          }

                          // Assegnazione forzata dello specifico ruolo standard USER per prevenire privilege escalation
                          enforcedUserRoles.add(
                              new UserClientRole(
                                  client.getClientId(),
                                  userRoleDef.getRole(),
                                  userRoleDef.getType(),
                                  userRoleDef.getDescription(),
                                  userRoleDef.getPermissions()));
                        });

                    // Sovrascrittura dei ruoli richiesti con quelli verificati e blindati con tipologia USER
                    user.setRoles(enforcedUserRoles);
                    return Mono.empty();
                  });
    }

    return registrationValidationMono
        .then(
            Mono.fromCallable(() -> userService.saveUser(user))
                .subscribeOn(Schedulers.boundedElastic()))
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

  /**
   * Determina se l'operatore chiamante possiede i privilegi amministrativi per bypassare il blocco
   * preventivo della registrazione autonoma (Super Admin, utente tecnico o utente dotato del
   * permesso 'users:write' / '*').
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore.
   * @param targetClientId Identificativo del client OAuth2 bersaglio.
   * @return true se l'operatore e autorizzato ad aggiungere utenti anche con registrazione
   *     disattivata.
   */
  public boolean canBypassRegistrationCheck(String bearer, String targetClientId) {
    if (techUserService.isTechUser()) {
      return true;
    }
    if (!StringUtils.hasText(bearer)) {
      return false;
    }
    try {
      OAuth2Authorization tokenData = oAuth2AuthorizationService.getByAccessTokenOrIdToken(bearer);
      if (tokenData == null) {
        return false;
      }
      return permissionRoleManager.canWriteUsers(tokenData, targetClientId);
    } catch (Exception e) {
      LOG.debug(
          "Bearer token is not an authorized operator token for registration bypass: {}",
          e.getMessage());
      return false;
    }
  }

  /**
   * Registra un nuovo utente (utilizzato anche dal flusso di federazione Google) validando il
   * client, verificando il blocco preventivo di registrazione e persistendo l'utente e il ruolo su
   * USER_CLIENT_ROLE.
   *
   * @param bearer Token di autorizzazione opzionale dell'utente chiamante.
   * @param user DTO con i dati del nuovo utente.
   * @param oAuth2Client Credenziali del client OAuth2 associato.
   * @param assignNewClient Flag per eventuale generazione nuove credenziali.
   * @return Mono contenente il DTO {@link User} persistito.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> registerUser(
      String bearer, User user, OAuth2Client oAuth2Client, Boolean assignNewClient) {
    return Mono.defer(
        () -> {
          LOG.info(
              "👤 [USER ADMIN] Registering user: {}, email: {}",
              user.getUsername(),
              user.getEmail());

          // Gli utenti admin o con permesso users:write possono registrare utenti anche se
          // enableRegistration e disattivato
          if (!canBypassRegistrationCheck(bearer, oAuth2Client.getClientId())) {
            UserValidator.validateClientRegistrationEnabled(oAuth2Client);
          }

          List<RoleDefinition> clientRoles =
              oAuth2Client.getAppRoles() != null ? oAuth2Client.getAppRoles() : List.of();
          List<RoleDefinition> userRoles =
              clientRoles.stream().filter(appRole -> appRole != null && appRole.isUser()).toList();

          if (!ObjectToolkit.isNullOrEmpty(bearer)) {
            LOG.info("🛡️ Bearer token present, checking technical roles");
            OAuth2Authorization tokenData =
                oAuth2AuthorizationService.getByAccessTokenOrIdToken(bearer);

            if (permissionRoleManager.hasPermissionRoles(tokenData, oAuth2Client)
                && !ObjectToolkit.isNullOrEmpty(user.getRoles())) {
              userRoles =
                  clientRoles.stream()
                      .filter(
                          appRole ->
                              appRole != null
                                  && user.getRoles().stream()
                                      .anyMatch(r -> r != null && appRole.matches(r.getRole())))
                      .toList();
            }
          }

          if (ObjectUtils.isEmpty(userRoles)) {
            if (!clientRoles.isEmpty()) {
              userRoles = List.of(clientRoles.getFirst());
              LOG.warn(
                  "⚠️ No USER role explicitly found on client {}; falling back to first available role [{}]",
                  oAuth2Client.getClientId(),
                  userRoles.getFirst().getRole());
            } else {
              LOG.error(
                  "❌ Roles not present on client configuration for client_id {}",
                  oAuth2Client.getClientId());
              throw new UserException(
                  ExceptionMap.ERR_OAUTH_400, "Invalid client_id configurations!");
            }
          }
          user.setRoles(
              userRoles.stream()
                  .map(
                      ar ->
                          new UserClientRole(
                              oAuth2Client.getClientId(),
                              ar.getRole(),
                              ar.getType(),
                              ar.getDescription(),
                              ar.getPermissions()))
                  .toList());

          return Mono.fromCallable(
                  () -> {
                    UserEntity userEntity = UserMapper.toEntity(user);
                    if (!ObjectToolkit.isNullOrEmpty(user.getPassword())) {
                      userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
                    }
                    String identifier = UUID.randomUUID().toString();
                    user.setIdentifier(identifier);
                    userEntity.setIdentifier(identifier);
                    user.setBlocked(false);
                    user.setConfirmed(false);

                    userService.saveUser(user);

                    List<UserClientRoleEntity> ucrEntities =
                        user.getRoles().stream()
                            .map(
                                r ->
                                    new UserClientRoleEntity(
                                        identifier, r.getClientId(), r.getRole()))
                            .toList();
                    userClientRoleDAO.saveAll(ucrEntities);

                    user.setPassword(null);
                    return user;
                  })
              .subscribeOn(Schedulers.boundedElastic());
        });
  }

  /**
   * Blocca o sblocca lo stato di accesso di un utente.
   *
   * <p>Interconnessioni logiche:
   *
   * <ul>
   *   <li>Risolve l'utente target da {@link UserService#getUserByIdentifier(String)}.
   *   <li>Verifica tramite {@link AdminUserService#isTechnicalUser(User)} che un utente non tecnico
   *       non possa mai bloccare o sbloccare l'utenza tecnica di sistema (genera {@link
   *       ExceptionMap#ERR_TECH_403}).
   *   <li>Se l'operatore e' tecnico, valida i ruoli con {@link
   *       TechUserValidator#validateTechRoles}.
   *   <li>Aggiorna lo stato sul database tramite {@link UserService#updateUser(String, User)}.
   * </ul>
   *
   * @param identifier UUID univoco dell'utente da bloccare o sbloccare.
   * @param block True per bloccare l'accesso, false per sbloccarlo.
   * @return Mono contenente il DTO {@link User} aggiornato con il nuovo stato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> unlockUser(String identifier, Boolean block) {
    LOG.info(
        "🛡️ [USER ADMIN] Starting user {} process for identifier: {}",
        block ? "lock" : "unlock",
        identifier);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            targetUser -> {
              if (techUserService.isTechnicalUser(targetUser) && !techUserService.isTechUser()) {
                LOG.error(
                    "❌ Forbidden: Non-tech user cannot change lock status of technical user {}",
                    identifier);
                return Mono.error(
                    new OAuthException(
                        ExceptionMap.ERR_TECH_403, "Cannot alter technical user status"));
              }

              if (techUserService.isTechUser()) {
                return oAuth2ClientDomainService
                    .getOAuth2ClientByClientID(techUserService.getAdminClientId())
                    .flatMap(
                        clientCredential -> {
                          TechUserValidator.validateTechRoles(
                              clientCredential, oAuth2Authorization.getRoles());
                          targetUser.setBlocked(block);
                          return userService.updateUser(identifier, targetUser);
                        });
              } else {
                targetUser.setBlocked(block);
                return userService.updateUser(identifier, targetUser);
              }
            })
        .doOnSuccess(
            updatedUser ->
                LOG.info(
                    "✅ [USER ADMIN] User {} process completed successfully for identifier: {}",
                    block ? "lock" : "unlock",
                    identifier));
  }

  /**
   * Cancella definitivamente un utente dal sistema IAM ed esegue il webhook associato.
   *
   * <p>Interconnessioni logiche:
   *
   * <ul>
   *   <li>Risolve l'utente target da {@link UserService#getUserByIdentifier(String)}.
   *   <li>Impedisce tassativamente l'eliminazione dell'utenza tecnica di sistema ({@link
   *       AdminUserService#isTechnicalUser(User)}), restituendo {@link ExceptionMap#ERR_TECH_403}.
   *   <li>Se l'operatore e' tecnico, valida i ruoli con {@link
   *       TechUserValidator#validateTechRoles}.
   *   <li>Cancella l'entita' dal database ed esegue il webhook associato tramite {@link
   *       #deleteUserInternal}.
   * </ul>
   *
   * @param identifier UUID univoco dell'utente da cancellare.
   * @param bearer Token di autorizzazione del chiamante.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono vuoto indicante il completamento della cancellazione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteUser(String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ [USER ADMIN] Starting user deletion process for identifier: {}", identifier);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            targetUser -> {
              if (techUserService.isTechnicalUser(targetUser)) {
                LOG.error("❌ Forbidden: The technical user {} cannot be deleted", identifier);
                return Mono.error(
                    new OAuthException(
                        ExceptionMap.ERR_TECH_403, "The technical user cannot be deleted"));
              }

              if (techUserService.isTechUser()) {
                return oAuth2ClientDomainService
                    .getOAuth2ClientByClientID(techUserService.getAdminClientId())
                    .flatMap(
                        clientCredential -> {
                          TechUserValidator.validateTechRoles(
                              clientCredential, oAuth2Authorization.getRoles());
                          return deleteUserInternal(identifier, bearer, exchange);
                        });
              } else {
                return deleteUserInternal(identifier, bearer, exchange);
              }
            })
        .doOnSuccess(
            unused ->
                LOG.info(
                    "✅ [USER ADMIN] User deletion successfully completed for identifier: {}",
                    identifier));
  }

  /**
   * Rimuove l'utente dal database ed esegue in maniera reattiva il webhook {@code DELETE_USER}.
   *
   * @param identifier UUID dell'utente da rimuovere.
   * @param bearer Token di autorizzazione.
   * @param exchange Scambio HTTP reattivo.
   * @return Mono vuoto indicante la fine delle operazioni.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteUserInternal(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ [USER ADMIN] Deleting user internally from database, identifier: {}", identifier);
    return Mono.fromRunnable(() -> userService.deleteUserEntityByIdentifier(identifier))
        .then(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, bearer, identifier, exchange));
  }
}
