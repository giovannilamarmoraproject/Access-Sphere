package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
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

  /**
   * Recupera un utente dal database locale tramite il suo identificatore univoco UUID. Valida la
   * presenza dell'entita' tramite {@link UserValidator#validateUser(UserEntity)} e la mappa in DTO
   * {@link User}.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @return Mono contenente il DTO {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByIdentifier(String identifier) {
    LOG.info("👤 Getting user by identifier: {}", identifier);
    UserEntity userEntity = userDAO.findUserEntityByIdentifier(identifier);
    UserValidator.validateUser(userEntity);
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  /**
   * Overload di retrocompatibilita' per il recupero utente tramite identificatore univoco.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param unused Parametro legacy non utilizzato.
   * @return Mono contenente il DTO {@link User}.
   */
  public Mono<User> getUserByIdentifier(String identifier, boolean unused) {
    return getUserByIdentifier(identifier);
  }

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
    UserEntity userEntity = userDAO.findUserEntityByEmail(email);
    UserValidator.validateUser(userEntity);
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  /**
   * Recupera un utente dal database locale a partire dallo username o dall'email.
   *
   * @param username Username dell'utente (opzionale).
   * @param email Indirizzo email dell'utente (opzionale).
   * @return Entita' {@link UserEntity} trovata o null se non presente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity getUserByUsernameOrEmail(String username, String email) {
    LOG.info("👤 Finding user entity by username: {} or email: {}", username, email);
    return userDAO.findUserEntityByUsernameOrEmail(username, email);
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
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  /**
   * Recupera la lista completa di tutti gli utenti registrati nel database locale. Converte
   * l'elenco delle entita' persistite nel corrispondente elenco di DTO {@link User}.
   *
   * @return Mono contenente la lista di {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsers() {
    LOG.info("👥 Getting all users from database");
    List<UserEntity> userEntities = userDAO.findAll();
    return Mono.just(UserMapper.toDTOList(userEntities));
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

    User userDTO = UserMapper.toDTO(userEntity);
    userDTO.setPassword(null);
    LOG.debug("✅ Credentials successfully validated for user: {}", userDTO.getUsername());
    return Mono.just(userDTO);
  }

  /**
   * Aggiorna un utente nel database locale senza modificare la password.
   *
   * @param userToUpdate DTO dell'utente con i campi aggiornati.
   * @return Mono contenente il DTO {@link User} aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(String identifier, User userToUpdate) {
    return updateUser(identifier, userToUpdate, false);
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
    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
    return Mono.just(saveUserEntityIntoDatabase(userEntity));
  }

  private void setDataBeforeUpdate(
      UserEntity userEntity, UserEntity userFind, boolean isUpdatePassword) {
    if (ObjectToolkit.isNullOrEmpty(userFind)) return;
    if (!ObjectToolkit.isNullOrEmpty(userFind.getId())) userEntity.setId(userFind.getId());
    userEntity.setIdentifier(userFind.getIdentifier());
    if (!isUpdatePassword) userEntity.setPassword(userFind.getPassword());
    userEntity.setRoles(userFind.getRoles());
  }

  /**
   * Persiste un'entita' utente nel database locale dopo aver azzerato la password per sicurezza.
   *
   * @param userEntity Entita' da salvare.
   * @return DTO {@link User} corrispondente all'entita' salvata.
   */
  @Transactional
  public User saveUserEntityIntoDatabase(UserEntity userEntity) {
    UserEntity userSaved = userDAO.saveAndFlush(userEntity);
    userSaved.setPassword(null);
    UserValidator.validateUser(userSaved);
    return UserMapper.toDTO(userSaved);
  }

  /**
   * Salva un nuovo utente nel database locale convertendolo da DTO a entita'.
   *
   * @param user DTO utente da salvare.
   */
  public void saveUserIntoDatabase(User user) {
    UserEntity userEntity = UserMapper.toEntity(user);
    saveUserEntityIntoDatabase(userEntity);
  }

  /**
   * Aggiorna un utente esistente nel database locale a partire dal suo DTO.
   *
   * @param user DTO con i dati aggiornati.
   */
  public void updateUserIntoDatabase(User user) {
    UserEntity userSaved = userDAO.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(userSaved)) {
      UserMapper.updateEntity(userSaved, user);
      saveUserEntityIntoDatabase(userSaved);
    } else {
      LOG.error("❌ User not found for update: {}", user.getIdentifier());
      throw new IllegalArgumentException("User not found: " + user.getIdentifier());
    }
  }

  /**
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserEntityByIdentifier(String identifier) {
    LOG.info("🗑️ Deleting user entity from database, identifier: {}", identifier);
    userDAO.deleteUserEntityByIdentifier(identifier);
  }
}
