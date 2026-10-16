package io.github.giovannilamarmora.accesssphere.user;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.UserRoleDomainService;
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
    return webhookService
        .checkAndExecuteWebhook(WebhookAction.CREATE_USER, identifier)
        .thenReturn(userSaved);
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
   * Aggiorna un utente quando l'entità persistita è già stata precedentemente recuperata (ad
   * esempio da UserAccountService o UserAdminService per controlli di sicurezza/scoping), azzerando
   * le query duplicate di lookup.
   *
   * @param existingUser Utente attualmente persistito a database.
   * @param userToUpdate DTO con i nuovi campi da aggiornare.
   * @return Mono con l'utente aggiornato e persistito.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(User existingUser, User userToUpdate) {
    if (existingUser == null || userToUpdate == null) {
      throw new OAuthException(ExceptionMap.ERR_USER_400, "Invalid user provided!");
    }
    LOG.info("👤 Updating user in database, identifier: {}", existingUser.getIdentifier());

    userToUpdate.setId(existingUser.getId());

    boolean isUpdatePassword =
        !ObjectToolkit.isNullOrEmpty(userToUpdate.getPassword())
            && !userToUpdate.getPassword().equals(existingUser.getPassword());

    if (isUpdatePassword) {
      if (!Utilities.isCharacterAndRegexValid(
          userToUpdate.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
        LOG.error("❌ Invalid regex for password for user: {}", userToUpdate.getUsername());
        throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
      }
      userToUpdate.setPassword(bCryptPasswordEncoder.encode(userToUpdate.getPassword()));
    } else {
      userToUpdate.setPassword(existingUser.getPassword());
    }

    User userUpdated = userRepository.saveAndFlush(userToUpdate);
    if (!ObjectToolkit.isNullOrEmpty(userToUpdate.getRoles())) {
      userUpdated.setRoles(userToUpdate.getRoles());
    } else {
      userUpdated.setRoles(userRoleDomainService.getUserRoles(userUpdated.getIdentifier()));
    }
    return Mono.justOrEmpty(userUpdated);
  }

  /** Aggiorna un utente ricercandolo preventivamente per identificatore. */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(String identifier, User userToUpdate) {
    LOG.info("👤 Checking and updating user in database, identifier: {}", identifier);

    Optional<User> existingUser = userRepository.findUserByIdentifier(identifier);
    if (existingUser.isEmpty()) {
      LOG.error("❌ The User with identifier {} does not exist", identifier);
      throw new OAuthException(ExceptionMap.ERR_USER_400, "Invalid user provided!");
    }

    return updateUser(existingUser.get(), userToUpdate);
  }

  /**
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco,
   * rimuovendo a cascata anche le associazioni relazionali di ruoli in USER_CLIENT_ROLE ed
   * eseguendo i webhook configurati.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param bearer Token di autorizzazione del chiamante.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono vuoto indicante il completamento della cancellazione e dei webhook.
   */
  @Transactional
  public Mono<Void> deleteUserByIdentifier(String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ Deleting user and client roles from database, identifier: {}", identifier);

    userRoleDomainService.deleteUserRoles(identifier);
    userRepository.deleteByIdentifier(identifier);

    LOG.info("🗑️ Deleted user and client roles from database, identifier: {}", identifier);

    return webhookService.checkAndExecuteWebhook(WebhookAction.DELETE_USER, bearer, identifier, exchange);
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
    User user = UserMapper.toModel(userEntity);
    user.setRoles(userRoleDomainService.getUserRoles(user.getIdentifier()));
    return Mono.just(user);
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
    userDTO.setRoles(userRoleDomainService.getUserRoles(userDTO.getIdentifier()));
    LOG.debug("✅ Credentials successfully validated for user: {}", userDTO.getUsername());
    return Mono.just(userDTO);
  }

  /**
   * Verifica se la password in chiaro corrisponde all'hash BCrypt archiviato a database.
   *
   * @param rawPassword Password in chiaro fornita dal client.
   * @param encodedPassword Hash BCrypt archiviato nel database.
   * @return true se le credenziali corrispondono, false altrimenti.
   */
  public boolean matchesPassword(String rawPassword, String encodedPassword) {
    return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
  }
}
