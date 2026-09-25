package io.github.giovannilamarmora.accesssphere.data.user.admin;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.OAuth2AuthorizationService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.RoleManager;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

  @Autowired private AdminUserService techUserService;
  @Autowired private OAuth2ClientService oAuth2ClientService;
  @Autowired private UserService userService;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private OAuth2AuthorizationService oAuth2AuthorizationService;
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
  public Mono<List<User>> getUsers(String bearer) {
    LOG.info("👥 [USER ADMIN] Starting retrieval of users list");
    if (StringUtils.hasText(bearer) && !techUserService.isTechUser()) {
      try {
        OAuth2Authorization tokenData =
            oAuth2AuthorizationService.getByAccessTokenOrIdToken(bearer);
        BeanUtils.copyProperties(tokenData, oAuth2Authorization);
      } catch (Exception e) {
        LOG.warn("Could not retrieve AccessTokenData from bearer: {}", e.getMessage());
      }
    }

    if (techUserService.isTechUser()) {
      return oAuth2ClientService
          .getClientCredentialByClientID(techUserService.getAdminClientId())
          .flatMap(
              clientCredential -> {
                TechUserValidator.validateTechRoles(
                    clientCredential, oAuth2Authorization.getRoles());
                return userService.getUsers();
              })
          .doOnSuccess(
              users ->
                  LOG.info(
                      "✅ [USER ADMIN] Successfully retrieved all {} users for technical operator",
                      users.size()));
    }

    return userService
        .getUsers()
        .map(
            users -> users.stream().filter(user -> !techUserService.isTechnicalUser(user)).toList())
        .doOnSuccess(
            filteredUsers ->
                LOG.info(
                    "✅ [USER ADMIN] Successfully retrieved {} users (technical user excluded for normal operator)",
                    filteredUsers.size()));
  }

  /**
   * Recupera la lista di tutti gli utenti registrati delegando a {@link #getUsers(String)}.
   *
   * @return Mono contenente la lista di tutti i DTO {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsers() {
    return getUsers(null);
  }

  /**
   * Registra un nuovo utente nel sistema IAM validando credenziali, regex, token di registrazione e
   * ruoli applicativi configurati sul client.
   *
   * @param bearer Token Bearer opzionale per operazioni privilegiate da utente tecnico.
   * @param user DTO contenente anagrafica e credenziali del nuovo utente.
   * @param clientId Identificatore univoco del client OAuth associato.
   * @param registration_token Token segreto di registrazione verificato rispetto alla
   *     configurazione del client.
   * @param assignNewClient Flag facoltativo per associare nuove credenziali.
   * @return Mono contenente il DTO {@link User} registrato e salvato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> register(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    LOG.info(
        "👤 [USER ADMIN] Starting user registration for username: {}, email: {}, clientId: {}",
        user.getUsername(),
        user.getEmail(),
        clientId);

    UserEntity existUser =
        userService.getUserByUsernameOrEmail(user.getUsername(), user.getEmail());
    if (!ObjectToolkit.isNullOrEmpty(existUser)) {
      LOG.error(
          "👤 The user already exists for username: {} and email: {}",
          user.getUsername(),
          user.getEmail());
      throw new UserException(
          ExceptionMap.ERR_USER_400, "User %s already exist".formatted(user.getUsername()));
    }

    return oAuth2ClientService
        .getClientCredentialByClientID(clientId)
        .flatMap(
            clientCredential -> {
              UserValidator.validateRegistration(registration_token, clientCredential, user);
              return registerUser(bearer, user, clientCredential, assignNewClient);
            })
        .doOnSuccess(
            registeredUser ->
                LOG.info(
                    "✅ [USER ADMIN] Registration successfully completed for username: {}, email: {}",
                    registeredUser.getUsername(),
                    registeredUser.getEmail()));
  }

  /**
   * Registra un nuovo utente nel database locale validando o assegnando i ruoli applicativi
   * configurati sul client OAuth2. Se la richiesta proviene da un utente tecnico autenticato,
   * consente l'assegnazione mirata dei ruoli specificati nel payload. Effettua l'hashing BCrypt
   * della password.
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
    LOG.info(
        "👤 [USER ADMIN] Registering user: {}, email: {}", user.getUsername(), user.getEmail());
    List<AppRole> clientRoles =
        oAuth2Client.getAppRoles() != null ? oAuth2Client.getAppRoles() : List.of();
    List<AppRole> userRoles =
        clientRoles.stream().filter(appRole -> appRole != null && appRole.isUser()).toList();

    if (!ObjectToolkit.isNullOrEmpty(bearer)) {
      LOG.info("🛡️ Bearer token present, checking technical roles");
      OAuth2Authorization tokenData = oAuth2AuthorizationService.getByAccessTokenOrIdToken(bearer);

      if (RoleManager.hasPermissionRoles(tokenData, oAuth2Client)
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
        throw new UserException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id configurations!");
      }
    }
    user.setRoles(
        userRoles.stream()
            .map(
                ar ->
                    new UserClientRoleDTO(
                        oAuth2Client.getClientId(),
                        ar.getRole(),
                        ar.getType(),
                        ar.getDescription(),
                        ar.getPermissions()))
            .toList());

    UserEntity userEntity = UserMapper.toEntity(user);
    if (!ObjectToolkit.isNullOrEmpty(user.getPassword()))
      userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    userEntity.setIdentifier(identifier);

    return Mono.just(userService.saveUserEntityIntoDatabase(userEntity));
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
                return oAuth2ClientService
                    .getClientCredentialByClientID(techUserService.getAdminClientId())
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
                return oAuth2ClientService
                    .getClientCredentialByClientID(techUserService.getAdminClientId())
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
