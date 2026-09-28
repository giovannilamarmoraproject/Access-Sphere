package io.github.giovannilamarmora.accesssphere.user;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.database.UserClientRoleRepository;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.mapper.RoleDefinitionMapper;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.user.database.UserRepository;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private UserRepository userRepository;
  @Autowired private UserClientRoleRepository userClientRoleRepository;
  @Autowired private RoleDefinitionRepository roleDefinitionRepository;

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

    Optional<List<UserClientRole>> optionalUserClientRoles =
        userClientRoleRepository.findByUserIdentifier(user.getIdentifier());

    if (optionalUserClientRoles.isEmpty()) {
      LOG.warn("🔐 No Role found for user: {}", user.getIdentifier());
      return Mono.just(user);
    }

    Optional<List<RoleDefinition>> optionalRoleDefinitions = roleDefinitionRepository.findAll();

    if (optionalRoleDefinitions.isEmpty()) {
      LOG.warn("🔐 No Role Definition found for user: {}", user.getIdentifier());
      return Mono.just(user);
    }

    List<RoleDefinition> roleDefinitions = optionalRoleDefinitions.get();

    roleDefinitions.stream().map(roleDefinition -> {

    })

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

    return Mono.just(user);
  }

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
    return Mono.just(enrichUser(UserMapper.toDTO(userEntity)));
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
   * Verifica se esiste gia un utente registrato con lo username specificato.
   *
   * @param username Lo username da verificare.
   * @return true se presente, false altrimenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean existsByUsername(String username) {
    return userDAO.existsByUsername(username);
  }

  /**
   * Verifica se esiste gia un utente registrato con l'indirizzo email specificato.
   *
   * @param email L'indirizzo email da verificare.
   * @return true se presente, false altrimenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean existsByEmail(String email) {
    return userDAO.existsByEmail(email);
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
    return Mono.just(enrichUser(UserMapper.toDTO(userEntity)));
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
    return Mono.just(enrichUsers(UserMapper.toDTOList(userEntities)));
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
    enrichUser(userDTO);
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
    UserValidator.validateUser(userFind);
    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
    return Mono.just(saveUserEntityIntoDatabase(userEntity));
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
    return enrichUser(UserMapper.toDTO(userSaved));
  }

  /**
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco,
   * rimuovendo a cascata anche le associazioni relazionali di ruoli in USER_CLIENT_ROLE.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserEntityByIdentifier(String identifier) {
    LOG.info("🗑️ Deleting user entity and client roles from database, identifier: {}", identifier);
    userClientRoleDAO.deleteByUserIdentifier(identifier);
    userDAO.deleteUserEntityByIdentifier(identifier);
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

  /**
   * Arricchisce massivamente un elenco di DTO {@link User} con i rispettivi ruoli client-scoped,
   * ottimizzando gli accessi a DB interrogando ROLE_DEFINITIONS con recupero preliminare in batch
   * per evitare query N+1.
   *
   * @param users Lista di DTO utenti da arricchire.
   * @return Lista di DTO utenti arricchita.
   */
  public List<User> enrichUsers(List<User> users) {
    if (users == null || users.isEmpty()) return users;
    List<UserClientRoleEntity> allUcr = userClientRoleDAO.findAll();
    if (allUcr == null || allUcr.isEmpty()) return users;

    Map<String, List<UserClientRoleEntity>> ucrByUser =
        allUcr.stream()
            .filter(u -> u.getUserIdentifier() != null)
            .collect(Collectors.groupingBy(UserClientRoleEntity::getUserIdentifier));

    List<RoleDefinitionEntity> allRoleDefs = roleDefinitionDAO.findAll();
    Map<String, RoleDefinitionEntity> roleDefMap =
        allRoleDefs != null
            ? allRoleDefs.stream()
                .filter(r -> r.getClientId() != null && r.getRole() != null)
                .collect(
                    Collectors.toMap(
                        r ->
                            r.getClientId().trim().toUpperCase()
                                + ":"
                                + r.getRole().trim().toUpperCase(),
                        r -> r,
                        (a, b) -> a))
            : Map.of();

    for (User user : users) {
      if (user == null || user.getIdentifier() == null) continue;
      List<UserClientRoleEntity> ucrEntities = ucrByUser.get(user.getIdentifier());
      if (ucrEntities != null && !ucrEntities.isEmpty()) {
        List<UserClientRole> clientRoleDTOs =
            ucrEntities.stream()
                .map(
                    ucr -> {
                      UserClientRole dto = new UserClientRole(ucr.getClientId(), ucr.getRole());
                      String key =
                          (ucr.getClientId() != null ? ucr.getClientId().trim().toUpperCase() : "")
                              + ":"
                              + (ucr.getRole() != null ? ucr.getRole().trim().toUpperCase() : "");
                      RoleDefinitionEntity rd = roleDefMap.get(key);
                      if (rd != null) {
                        if (rd.getType() != null) dto.setType(rd.getType());
                        if (rd.getDescription() != null) dto.setDescription(rd.getDescription());
                        if (rd.getPermissions() != null) dto.setPermissions(rd.getPermissions());
                      }
                      return dto;
                    })
                .toList();
        user.setRoles(clientRoleDTOs);
      }
    }
    return users;
  }
}
