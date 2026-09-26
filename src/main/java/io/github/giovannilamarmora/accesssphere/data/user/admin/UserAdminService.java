package io.github.giovannilamarmora.accesssphere.data.user.admin;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.admin.dto.AddUserRequest;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.OAuth2AuthorizationService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.RoleManager;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.ArrayList;
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
import reactor.core.publisher.Flux;
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

  @Autowired private AdminUserService techUserService;
  @Autowired private OAuth2ClientService oAuth2ClientService;
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
   * Registra un nuovo utente nel sistema IAM validando anagrafica, regex, ruoli applicativi
   * configurati per ciascun client specificato e applicando il blocco preventivo qualora la
   * registrazione sia disabilitata sul client.
   *
   * @param bearer Token Bearer opzionale per operazioni privilegiate da operatore tecnico.
   * @param addUserRequest DTO contenente i dati anagrafici, credenziali e ruoli del nuovo utente.
   * @return Mono contenente il DTO {@link User} registrato e salvato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> register(String bearer, AddUserRequest addUserRequest) {
    LOG.info(
        "👤 [USER ADMIN] Starting user registration for username: {}, email: {}",
        addUserRequest != null ? addUserRequest.getUsername() : "null",
        addUserRequest != null ? addUserRequest.getEmail() : "null");

    // TODO: Controllo se l'utente ha i permessi di registrarsi, controllando prima se l'utente è
    // loggato e sta cercando di registrare un nuovo utente tramite il permesso user:write o se il
    // client ha la registrazione abilitata

    return Mono.defer(
        () -> {
          UserValidator.validateUserData(addUserRequest);
          User user = UserMapper.toModel(addUserRequest);

          return Mono.fromCallable(
                  () -> {
                    if (userService.existsByUsername(user.getUsername())) {
                      LOG.error("👤 The user already exists for username: {}", user.getUsername());
                      throw new UserException(
                          ExceptionMap.ERR_USER_400,
                          "User %s already exists".formatted(user.getUsername()));
                    }
                    if (userService.existsByEmail(user.getEmail())) {
                      LOG.error("👤 The user already exists for email: {}", user.getEmail());
                      throw new UserException(
                          ExceptionMap.ERR_USER_400,
                          "Email %s already exists".formatted(user.getEmail()));
                    }
                    return true;
                  })
              .subscribeOn(Schedulers.boundedElastic())
              .flatMap(ignored -> validateAndEnrichRoles(user.getRoles(), bearer))
              .flatMap(
                  enrichedRoles -> {
                    user.setRoles(enrichedRoles);
                    return Mono.fromCallable(
                            () -> {
                              UserEntity userEntity = UserMapper.toEntity(user);
                              if (!ObjectToolkit.isNullOrEmpty(user.getPassword())) {
                                userEntity.setPassword(
                                    bCryptPasswordEncoder.encode(user.getPassword()));
                              }
                              String identifier = UUID.randomUUID().toString();
                              user.setIdentifier(identifier);
                              userEntity.setIdentifier(identifier);
                              user.setBlocked(false);
                              user.setConfirmed(false);

                              userService.saveUserEntityIntoDatabase(userEntity);

                              List<UserClientRoleEntity> ucrEntities =
                                  enrichedRoles.stream()
                                      .map(
                                          r ->
                                              new UserClientRoleEntity(
                                                  identifier, r.getClientId(), r.getRole()))
                                      .toList();
                              userClientRoleDAO.saveAll(ucrEntities);

                              return user;
                            })
                        .subscribeOn(Schedulers.boundedElastic());
                  })
              .doOnSuccess(
                  registeredUser ->
                      LOG.info(
                          "✅ [USER ADMIN] Registration successfully completed for username: {}, email: {}",
                          registeredUser.getUsername(),
                          registeredUser.getEmail()));
        });
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
   * Convalida che ciascun ruolo passato nel payload appartenga a un client esistente con
   * registrazione abilitata (o bypassata se l'operatore ha permessi users:write/admin), e che il
   * ruolo sia effettivamente presente nel catalogo ruoli del client.
   *
   * @param requestedRoles Lista di ruoli richiesti con associazione client.
   * @param bearer Token di autorizzazione opzionale dell'operatore.
   * @return Mono con la lista dei ruoli arricchiti con tipologia, descrizione e permessi.
   */
  private Mono<List<UserClientRole>> validateAndEnrichRoles(
      List<UserClientRole> requestedRoles, String bearer) {
    List<String> clientIds =
        requestedRoles.stream()
            .map(
                r -> {
                  if (r == null || !StringUtils.hasText(r.getClientId())) {
                    LOG.error("❌ Registration rejected: clientId missing on role");
                    throw new UserException(
                        ExceptionMap.ERR_USER_400, "Client ID is required for each role!");
                  }
                  if (!StringUtils.hasText(r.getRole())) {
                    LOG.error("❌ Registration rejected: role name missing");
                    throw new UserException(
                        ExceptionMap.ERR_USER_400, "Role name is required for each role!");
                  }
                  return r.getClientId().trim().toUpperCase();
                })
            .distinct()
            .toList();

    return Flux.fromIterable(clientIds)
        .flatMap(clientId -> oAuth2ClientService.getClientCredentialByClientID(clientId))
        .collectMap(client -> client.getClientId().toUpperCase())
        .map(
            clientMap -> {
              List<UserClientRole> enriched = new ArrayList<>();
              for (UserClientRole r : requestedRoles) {
                String cId = r.getClientId().trim().toUpperCase();
                OAuth2Client client = clientMap.get(cId);
                if (client == null) {
                  LOG.error("❌ Client {} not found", cId);
                  throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id: " + cId);
                }

                // Gli utenti admin o con permesso users:write possono registrare utenti anche se
                // enableRegistration e disattivato
                if (!canBypassRegistrationCheck(bearer, client.getClientId())) {
                  UserValidator.validateClientRegistrationEnabled(client);
                }

                List<RoleDefinition> clientAppRoles =
                    client.getAppRoles() != null ? client.getAppRoles() : List.of();
                RoleDefinition matched =
                    clientAppRoles.stream()
                        .filter(ar -> ar != null && ar.matches(r.getRole()))
                        .findFirst()
                        .orElse(null);

                if (matched == null) {
                  LOG.error(
                      "❌ Role '{}' does not exist for client '{}'",
                      r.getRole(),
                      client.getClientId());
                  throw new UserException(
                      ExceptionMap.ERR_USER_400,
                      "Role '%s' does not exist for client '%s'"
                          .formatted(r.getRole(), client.getClientId()));
                }

                enriched.add(
                    new UserClientRole(
                        client.getClientId(),
                        matched.getRole(),
                        matched.getType(),
                        matched.getDescription(),
                        matched.getPermissions()));
              }
              return enriched;
            });
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

                    userService.saveUserEntityIntoDatabase(userEntity);

                    List<UserClientRoleEntity> ucrEntities =
                        user.getRoles().stream()
                            .map(
                                r ->
                                    new UserClientRoleEntity(
                                        identifier, r.getClientId(), r.getRole()))
                            .toList();
                    userClientRoleDAO.saveAll(ucrEntities);

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
