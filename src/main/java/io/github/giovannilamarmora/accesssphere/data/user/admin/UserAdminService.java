package io.github.giovannilamarmora.accesssphere.data.user.admin;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Servizio applicativo reattivo per la gestione amministrativa e il provisioning degli utenti IAM.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Verifica l'autorizzazione tecnica degli operatori tramite {@link TechUserService} e {@link
 *       TechUserValidator}.
 *   <li>Risolve la configurazione dei client OAuth tramite {@link ClientService}.
 *   <li>Esegue le operazioni di lettura, creazione e cancellazione delegando al layer di
 *       persistenza puro {@link UserService}.
 *   <li>Interagisce con {@link AccessTokenService} per l'analisi dei privilegi di token Bearer in
 *       fase di registrazione.
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

  @Autowired private TechUserService techUserService;
  @Autowired private ClientService clientService;
  @Autowired private UserService userService;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired @Lazy private WebhookService webhookService;

  /**
   * Recupera la lista di tutti gli utenti registrati (operazione protetta riservata ad operatori
   * tecnici).
   *
   * @return Mono contenente la lista di tutti i DTO {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsers() {
    LOG.info("👥 [USER ADMIN] Starting retrieval of all users");
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Forbidden: Only a tech user can retrieve the list of users");
      throw new OAuthException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    return clientService
        .getClientCredentialByClientID(techUserService.getTechClientId())
        .flatMap(
            clientCredential -> {
              TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
              return userService.getUsers();
            })
        .doOnSuccess(
            users -> LOG.info("✅ [USER ADMIN] Successfully retrieved {} users", users.size()));
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

    return clientService
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
   * @param clientCredential Credenziali del client OAuth2 associato.
   * @param assignNewClient Flag per eventuale generazione nuove credenziali.
   * @return Mono contenente il DTO {@link User} persistito.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> registerUser(
      String bearer, User user, ClientCredential clientCredential, Boolean assignNewClient) {
    LOG.info(
        "👤 [USER ADMIN] Registering user: {}, email: {}", user.getUsername(), user.getEmail());
    List<AppRole> clientRoles =
        clientCredential.getAppRoles() != null ? clientCredential.getAppRoles() : List.of();
    List<AppRole> userRoles =
        clientRoles.stream()
            .filter(appRole -> appRole != null && appRole.isDefault())
            .toList();

    if (!ObjectToolkit.isNullOrEmpty(bearer)) {
      LOG.info("🛡️ Bearer token present, checking technical roles");
      AccessTokenData tokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

      if (techUserService.hasTechUserRoles(tokenData, clientCredential)
          && !ObjectToolkit.isNullOrEmpty(user.getRoles())) {
        userRoles =
            clientRoles.stream()
                .filter(
                    appRole ->
                        appRole != null
                            && user.getRoles().stream()
                                .anyMatch(appRole::matches))
                .toList();
      }
    }

    if (ObjectUtils.isEmpty(userRoles)) {
      if (!clientRoles.isEmpty()) {
        userRoles = List.of(clientRoles.getFirst());
        LOG.warn(
            "⚠️ No DEFAULT role explicitly found on client {}; falling back to first available role [{}]",
            clientCredential.getClientId(),
            userRoles.getFirst().getRole());
      } else {
        LOG.error(
            "❌ Default roles not present on client configuration for client_id {}",
            clientCredential.getClientId());
        throw new UserException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id configurations!");
      }
    }
    user.setRoles(userRoles.stream().map(AppRole::getRole).toList());

    UserEntity userEntity = UserMapper.toEntity(user);
    if (!ObjectToolkit.isNullOrEmpty(user.getPassword()))
      userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    userEntity.setIdentifier(identifier);

    return Mono.just(userService.saveUserEntityIntoDatabase(userEntity));
  }

  /**
   * Blocca o sblocca lo stato di accesso di un utente (operazione protetta riservata a operatore
   * tecnico).
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
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Forbidden: Only a tech user can change lock status of user {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    return clientService
        .getClientCredentialByClientID(techUserService.getTechClientId())
        .flatMap(
            clientCredential -> {
              TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
              return userService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setBlocked(block);
                        return userService.updateUser(identifier, user);
                      });
            })
        .doOnSuccess(
            updatedUser ->
                LOG.info(
                    "✅ [USER ADMIN] User {} process completed successfully for identifier: {}",
                    block ? "lock" : "unlock",
                    identifier));
  }

  /**
   * Cancella definitivamente un utente dal sistema IAM (operazione protetta riservata a utente
   * tecnico) ed esegue il webhook associato.
   *
   * @param identifier UUID univoco dell'utente da cancellare.
   * @param bearer Token di autorizzazione dell'utente tecnico.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono vuoto indicante il completamento della cancellazione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteUser(String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ [USER ADMIN] Starting user deletion process for identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Forbidden: Only a tech user can delete user {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    return clientService
        .getClientCredentialByClientID(techUserService.getTechClientId())
        .flatMap(
            clientCredential -> {
              TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
              return deleteUserInternal(identifier, bearer, exchange);
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
