package io.github.giovannilamarmora.accesssphere.user;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.UserRoleDomainService;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.user.database.UserRepository;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Servizio applicativo reattivo per la gestione del livello di persistenza e accesso ai dati degli
 * utenti IAM.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Opera come strato di astrazione DAO e persistenza per {@link IUserDAO}.
 *   <li>Fornisce i metodi atomici di ricerca, aggiornamento, cancellazione e cifratura credenziali.
 *   <li>Viene consumato dai servizi di dominio: {@code UserAdminService} per il provisioning e la
 *       gestione amministrativa, {@code UserAccountService} per le operazioni self-service e
 *       profilo, {@code OIDCService} per la risoluzione anagrafica dei token federati/locali e
 *       {@code AuthService} per la validazione delle credenziali di accesso.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class UserService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  @Autowired private IUserDAO userDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private UserRepository userRepository;
  @Autowired private UserRoleDomainService userRoleDomainService;
  @Autowired private WebhookService webhookService;

  /**
   * Recupera un utente dal database locale a partire dal suo indirizzo email. Effettua la
   * validazione di esistenza e la conversione in DTO {@link User}.
   *
   * @param email Indirizzo email dell'utente.
   * @return Mono contenente il DTO {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByEmail(String email) {
    LOG.info("👤 Getting user by email: {}", email);
    Optional<User> optionalUser = userRepository.getUserByEmail(email);

    if (optionalUser.isEmpty()) {
      LOG.error("❌ User: {} not found into database", email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }
    User user = optionalUser.get();

    user.setRoles(userRoleDomainService.getUserRoles(user.getIdentifier()));
    return Mono.just(user);
  }

  /**
   * Recupera la lista completa di tutti gli utenti registrati nel database locale, delegando
   * l'arricchimento batch dei ruoli a {@link UserRoleDomainService}.
   *
   * @return Mono contenente la lista di {@link User} arricchita con i ruoli client-scoped.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsers() {
    LOG.info("👥 Getting all users from database");

    Optional<List<User>> optionalUsers = userRepository.findAll();

    if (optionalUsers.isEmpty() || optionalUsers.get().isEmpty()) {
      LOG.error("❌ Users not found into database");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }

    return Mono.just(userRoleDomainService.enrichUserRoles(optionalUsers.get()));
  }

  /**
   * Persiste un'entita' utente nel database locale e restituisce il corrispondente DTO {@link User}
   * arricchito con i ruoli applicativi.
   *
   * <p>Nota di sicurezza e integrità dati: non azzerare MAI la password sull'istanza managed {@link
   * UserEntity} all'interno del contesto transazionale, per evitare che il dirty checking di
   * Hibernate propaghi un UPDATE con PASSWORD = NULL su database. La rimozione della password per
   * la sicurezza del client viene già gestita a livello di DTO da {@link
   * UserMapper#toModel(UserEntity)}.
   *
   * @param user model da salvare con password cifrata (BCrypt).
   * @return DTO {@link User} corrispondente all'entita' salvata (con password protetta a null).
   */
  @Transactional
  public Mono<User> saveUser(User user) {
    UserValidator.validateUser(user);

    if (userRepository.existsByUsername(user.getUsername())) {
      LOG.error("👤 The user already exists for username: {}", user.getUsername());
      throw new UserException(
          ExceptionMap.ERR_USER_400, "User %s already exists".formatted(user.getUsername()));
    }
    if (userRepository.existsByEmail(user.getEmail())) {
      LOG.error("👤 The user already exists for email: {}", user.getEmail());
      throw new UserException(
          ExceptionMap.ERR_USER_400, "Email %s already exists".formatted(user.getEmail()));
    }

    if (!ObjectToolkit.isNullOrEmpty(user.getPassword())) {
      user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    }
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    user.setBlocked(false);
    user.setConfirmed(false);

    User userSaved = userRepository.saveAndFlush(user);

    List<UserClientRole> savedRoles =
        userRoleDomainService.saveUserRoles(identifier, user.getRoles());
    userSaved.setRoles(savedRoles);
    // TODO: Da sviluppare il Webhook per il Salvataggio
    return Mono.justOrEmpty(userSaved);
  }

  /**
   * Recupera un utente dal database locale tramite il suo identificatore univoco UUID. Valida la
   * presenza dell'entita' tramite {@link UserValidator#validateUser(UserEntity)} e la mappa in DTO
   * {@link User}.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @return Mono contenente il DTO {@link User}.
   */
  public Mono<User> getUserByIdentifier(String identifier) {
    LOG.info("👤 Getting user by identifier: {}", identifier);

    Optional<User> optionalUser = userRepository.findUserByIdentifier(identifier);

    if (optionalUser.isEmpty()) {
      LOG.error("❌ User {} not found into database", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }

    User user = optionalUser.get();

    user.setRoles(userRoleDomainService.getUserRoles(user.getIdentifier()));
    return Mono.just(user);
  }

  /**
   * Aggiorna i dati di un utente nel database locale, gestendo opzionalmente la cifratura e
   * l'aggiornamento della password con validazione tramite regex.
   *
   * @param userToUpdate DTO contenente i nuovi valori dei campi.
   * @return Mono contenente il DTO {@link User} aggiornato e persistito.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(String identifier, User userToUpdate) {
    LOG.info("👤 Updating user in database, identifier: {}", identifier);

    Optional<User> existingUser = userRepository.findUserByIdentifier(identifier);

    if (existingUser.isEmpty()) {
      LOG.error("❌ The User with identifier {} already does not exist", identifier);
      throw new OAuthException(ExceptionMap.ERR_USER_400, "Invalid user provided!");
    }

    userToUpdate.setId(existingUser.get().getId());

    boolean isUpdatePassword =
        !ObjectToolkit.isNullOrEmpty(userToUpdate.getPassword())
            && !userToUpdate.getPassword().equals(existingUser.get().getPassword());

    if (isUpdatePassword) {
      if (!Utilities.isCharacterAndRegexValid(
          userToUpdate.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
        LOG.error("❌ Invalid regex for password for user: {}", userToUpdate.getUsername());
        throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
      }
      userToUpdate.setPassword(bCryptPasswordEncoder.encode(userToUpdate.getPassword()));
    } else {
      userToUpdate.setPassword(existingUser.get().getPassword());
    }

    User userUpdated = userRepository.saveAndFlush(userToUpdate);
    if (!ObjectToolkit.isNullOrEmpty(userToUpdate.getRoles())) {
      userUpdated.setRoles(userToUpdate.getRoles());
    } else {
      userUpdated.setRoles(userRoleDomainService.getUserRoles(userUpdated.getIdentifier()));
    }
    return Mono.justOrEmpty(userUpdated);
  }

  /**
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco,
   * rimuovendo a cascata anche le associazioni relazionali di ruoli in USER_CLIENT_ROLE.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   */
  @Transactional
  public void deleteUserByIdentifier(String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ Deleting user and client roles from database, identifier: {}", identifier);

    userRoleDomainService.deleteUserRoles(identifier);
    userRepository.deleteByIdentifier(identifier);

    LOG.info("🗑️ Deleted user and client roles from database, identifier: {}", identifier);

    // TODO: Da sistemare il Webhook
    webhookService.checkAndExecuteWebhook(WebhookAction.DELETE_USER, bearer, identifier, exchange);
  }

  /**
   * Recupera un utente associato a un token di reset password e ne convalida la scadenza temporale.
   *
   * @param tokenReset Token UUID di recupero password.
   * @return Mono contenente il DTO {@link User} valido per la procedura di reset.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByTokenReset(String tokenReset) {
    LOG.info("🔑 Getting user by password reset token");
    UserEntity userEntity = userDAO.findUserEntityByTokenReset(tokenReset);
    UserValidator.validateUser(userEntity);
    UserValidator.validateResetToken(userEntity.getUpdateDate());
    return Mono.just(enrichUser(UserMapper.toModel(userEntity)));
  }

  /**
   * Autentica le credenziali di un utente verificando la corrispondenza con BCrypt. Utilizzato dal
   * motore di autenticazione {@code AuthService} per validare l'identita' prima di emettere i token
   * OAuth 2.0.
   *
   * @param username Username dell'utente (o null se autenticato via email).
   * @param email Email dell'utente (o null se autenticato via username).
   * @param rawPassword Password in chiaro da confrontare con l'hash archiviato.
   * @return Mono contenente il DTO {@link User} con password azzerata in caso di successo.
   * @throws OAuthException Se l'utente non viene trovato o la password non corrisponde.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> authenticateUser(String username, String email, String rawPassword) {
    LOG.debug("🔑 Validating credentials for username={} or email={}", username, email);
    UserEntity userEntity = userDAO.findUserEntityByUsernameOrEmail(username, email);

    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("❌ No user found on database for username: {}, email: {}", username, email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    boolean matches = bCryptPasswordEncoder.matches(rawPassword, userEntity.getPassword());
    if (!matches) {
      LOG.error("❌ Password verification failed for username: {}, email: {}", username, email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    User userDTO = UserMapper.toModel(userEntity);
    userDTO.setPassword(null);
    enrichUser(userDTO);
    LOG.debug("✅ Credentials successfully validated for user: {}", userDTO.getUsername());
    return Mono.just(userDTO);
  }

  /**
   * Aggiorna i dati di un utente nel database locale, gestendo opzionalmente la cifratura e
   * l'aggiornamento della password con validazione tramite regex.
   *
   * @param userToUpdate DTO contenente i nuovi valori dei campi.
   * @param isUpdatePassword Flag che indica se procedere all'aggiornamento della password.
   * @return Mono contenente il DTO {@link User} aggiornato e persistito.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(String identifier, User userToUpdate, boolean isUpdatePassword) {
    LOG.info(
        "👤 Updating user in database, identifier: {}, updatePassword: {}",
        identifier,
        isUpdatePassword);
    UserEntity userEntity = UserMapper.toEntity(userToUpdate);

    if (isUpdatePassword) {
      if (userToUpdate.getPassword() != null && !userToUpdate.getPassword().isBlank()) {
        if (!Utilities.isCharacterAndRegexValid(
            userToUpdate.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
          LOG.error("❌ Invalid regex for password for user: {}", userToUpdate.getUsername());
          throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
        }
        userEntity.setPassword(bCryptPasswordEncoder.encode(userToUpdate.getPassword()));
      }
    }

    UserEntity userFind = userDAO.findUserEntityByIdentifier(identifier);
    UserValidator.validateUser(userFind);
    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
    User user = UserMapper.toModel(userFind);
    return saveUser(user);
  }

  private void setDataBeforeUpdate(
      UserEntity userEntity, UserEntity userFind, boolean isUpdatePassword) {
    if (ObjectToolkit.isNullOrEmpty(userFind)) return;
    if (!ObjectToolkit.isNullOrEmpty(userFind.getId())) userEntity.setId(userFind.getId());
    userEntity.setIdentifier(userFind.getIdentifier());
    if (!isUpdatePassword) userEntity.setPassword(userFind.getPassword());
    if (ObjectToolkit.isNullOrEmpty(userEntity.getMfaSettings())) {
      userEntity.setMfaSettings(userFind.getMfaSettings());
    }
    if (ObjectToolkit.isNullOrEmpty(userEntity.getTokenReset())) {
      userEntity.setTokenReset(userFind.getTokenReset());
    }
  }

  /**
   * Arricchisce il DTO {@link User} con i ruoli contestualizzati per Client OAuth2 ({@link
   * UserClientRole}) recuperati dalla tabella USER_CLIENT_ROLE, estraendo tipologia, descrizione e
   * permessi granulari dalla tabella relazionale ROLE_DEFINITIONS (con fallback su configurazione
   * legacy del client). Sincronizza inoltre la lista legacy flat {@code roles}.
   *
   * @param user Oggetto utente da arricchire.
   * @return DTO utente arricchito.
   */
  public User enrichUser(User user) {
    if (user == null || user.getIdentifier() == null) return user;
    List<UserClientRoleEntity> ucrEntities =
        userClientRoleDAO.findByUserIdentifier(user.getIdentifier());
    if (ucrEntities != null && !ucrEntities.isEmpty()) {
      List<UserClientRole> clientRoleDTOs =
          ucrEntities.stream()
              .map(
                  ucr -> {
                    UserClientRole dto = new UserClientRole(ucr.getClientId(), ucr.getRole());
                    Optional<RoleDefinitionEntity> roleDef =
                        roleDefinitionDAO.findByClientIdAndRole(ucr.getClientId(), ucr.getRole());
                    if (roleDef.isPresent()) {
                      RoleDefinitionEntity rd = roleDef.get();
                      if (rd.getType() != null) dto.setType(rd.getType());
                      if (rd.getDescription() != null) dto.setDescription(rd.getDescription());
                      if (rd.getPermissions() != null) dto.setPermissions(rd.getPermissions());
                    }
                    return dto;
                  })
              .toList();
      user.setRoles(clientRoleDTOs);
    }
    return user;
  }
}
