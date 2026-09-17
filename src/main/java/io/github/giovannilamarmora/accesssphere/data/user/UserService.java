package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.TemplateParam;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.email.EmailTemplateService;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@Logged
@RequiredArgsConstructor
public class UserService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  @Autowired private IUserDAO userDAO;
  @Autowired private ClientService clientService;
  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private EmailTemplateService emailTemplateService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private TechUserService techUserService;
  @Autowired @Lazy private WebhookService webhookService;

  /**
   * Recupera un utente a partire dal suo identificatore univoco dal database locale.
   * Valida l'esistenza dell'entita' tramite {@link UserValidator#validateUser(UserEntity)}
   * e mappa l'entita' persistita nel DTO {@link User}.
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
   * Recupera un utente dal database locale a partire dal suo indirizzo email.
   * Effettua la validazione di esistenza e la conversione in DTO {@link User}.
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
   * Recupera la lista completa di tutti gli utenti registrati nel database locale.
   * Converte l'elenco delle entita' persistite nel corrispondente elenco di DTO {@link User}.
   *
   * @return Mono contenente la lista di {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsersFromDatabase() {
    LOG.info("👥 Getting all users from database");
    List<UserEntity> userEntities = userDAO.findAll();
    return Mono.just(UserMapper.toDTOList(userEntities));
  }

  /**
   * Registra un nuovo utente nel database locale, validando o assegnando i ruoli applicativi
   * definiti sul client OAuth. Se la richiesta proviene da un utente tecnico autenticato,
   * consente l'assegnazione mirata dei ruoli specificati nel payload.
   * Effettua l'hashing bcrypt della password prima del salvataggio.
   *
   * @param bearer Token di autorizzazione opzionale dell'utente chiamante.
   * @param user DTO contenente i dati di registrazione dell'utente.
   * @param clientCredential Credenziali di configurazione del client OAuth2.
   * @param assignNewClient Flag per l'eventuale assegnazione di nuove credenziali client.
   * @return Mono contenente il DTO {@link User} registrato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> registerUser(
      String bearer, User user, ClientCredential clientCredential, Boolean assignNewClient) {
    LOG.info("👤 Registering user: {}, email: {}", user.getUsername(), user.getEmail());
    List<AppRole> userRoles =
        clientCredential.getAppRoles().stream()
            .filter(appRole -> appRole.getType().equalsIgnoreCase("default"))
            .toList();

    if (!ObjectToolkit.isNullOrEmpty(bearer)) {
      LOG.info("🛡️ Bearer token present, checking technical roles");
      AccessTokenData tokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

      if (techUserService.hasTechUserRoles(tokenData, clientCredential)
          && !ObjectToolkit.isNullOrEmpty(user.getRoles())) {
        userRoles =
            clientCredential.getAppRoles().stream()
                .filter(
                    appRole ->
                        user.getRoles().stream()
                            .anyMatch(role -> role.equalsIgnoreCase(appRole.getRole())))
                .toList();
      }
    }

    if (ObjectUtils.isEmpty(userRoles)) {
      LOG.error(
          "❌ Default roles not present on client configuration for client_id {}",
          clientCredential.getClientId());
      throw new UserException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id configurations!");
    }
    user.setRoles(userRoles.stream().map(AppRole::getRole).toList());

    UserEntity userEntity = UserMapper.toEntity(user);
    if (!ObjectToolkit.isNullOrEmpty(user.getPassword()))
      userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    userEntity.setIdentifier(identifier);

    return Mono.just(saveUserEntityIntoDatabase(userEntity));
  }

  /**
   * Esegue l'autenticazione dell'utente tramite credenziali locali (username/email e password)
   * o delega a {@link TechUserService} nel caso si tratti di un utente tecnico.
   * Genera la risposta con token OAuth2 completo e dati JWT associati.
   *
   * @param username Username dell'utente (opzionale se specificata email).
   * @param email Indirizzo email dell'utente (opzionale se specificato username).
   * @param password Password in chiaro da validare con BCrypt.
   * @param clientCredential Credenziali del client OAuth che richiede l'autenticazione.
   * @param request Richiesta HTTP reattiva per il tracciamento e la compilazione dei claim JWT.
   * @return Mono contenente l'oggetto {@link OAuthTokenResponse} generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> login(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    if (!techUserService.checkIfTechUser(
        ObjectToolkit.getOrDefault(username, email), password, clientCredential.getClientId())) {
      return Mono.just(
          performLoginViaDatabase(username, email, password, clientCredential, request));
    } else return techUserService.loginTechUser(username, clientCredential, request);
  }

  private OAuthTokenResponse performLoginViaDatabase(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    LOG.debug("🔑 Login process via Database started for username={} and email={}", username, email);
    UserEntity userEntity = userDAO.findUserEntityByUsernameOrEmail(username, email);

    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("❌ No user found on database for username: {}, email: {}", username, email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    boolean matches = bCryptPasswordEncoder.matches(password, userEntity.getPassword());
    if (!matches) {
      LOG.error("❌ Password verification failed for username: {}, email: {}", username, email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
    User userEntityToUser = UserMapper.toDTO(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData =
        JWTData.generateJWTData(userEntityToUser, clientCredential, SubjectType.CUSTOMER, request);
    AuthToken token = tokenService.generateToken(jwtData, clientCredential, null);
    LOG.debug("✅ Login process via Database ended for username={} and email={}", username, email);
    return new OAuthTokenResponse(userEntityToUser, jwtData, token);
  }

  /**
   * Recupera i dati di profilo e le informazioni utente basandosi sui claim del JWT decifrato.
   * Supporta il fallback per utenti tecnici tramite {@link TechUserService#userInfo(JWTData)}.
   *
   * @param jwtData Claim estratti dal token JWT.
   * @param token Token di accesso in stringa (opzionale).
   * @return Mono contenente il DTO {@link User}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserInfo(JWTData jwtData, String token) {
    if (!techUserService.isTechUser()) {
      UserEntity userEntity = userDAO.findUserEntityByEmail(jwtData.getEmail());
      UserValidator.validateUser(userEntity);
      return Mono.just(UserMapper.toDTO(userEntity));
    }
    return techUserService.userInfo(jwtData);
  }

  /**
   * Esegue il rinnovo del token JWT a partire dai dati della sessione di accesso esistente
   * e dalle credenziali del client.
   *
   * @param accessTokenData Dati decifrati del token di accesso corrente.
   * @param clientCredential Credenziali del client OAuth.
   * @param token Token di refresh.
   * @param request Richiesta HTTP server.
   * @return Mono contenente la risposta {@link OAuthTokenResponse} con il nuovo token generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> refreshJWTToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      String token,
      ServerHttpRequest request) {
    LOG.info("🔄 Refreshing JWT token for user email: {}", accessTokenData.getEmail());
    UserEntity userEntity = userDAO.findUserEntityByEmail(accessTokenData.getEmail());
    UserValidator.validateUser(userEntity);
    User userEntityToUser = UserMapper.toDTO(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData =
        JWTData.generateJWTData(
            userEntityToUser, clientCredential, accessTokenData.getSubjectType(), request);
    AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
    return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
  }

  /**
   * Aggiorna un utente nel database locale senza modificare la password.
   *
   * @param userToUpdate DTO dell'utente con i campi aggiornati.
   * @return Mono contenente il DTO {@link User} aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(User userToUpdate) {
    return updateUser(userToUpdate, false);
  }

  /**
   * Aggiorna i dati di un utente nel database locale, gestendo opzionalmente la cifratura
   * e l'aggiornamento della password con validazione tramite regex.
   *
   * @param userToUpdate DTO contenente i nuovi valori dei campi.
   * @param isUpdatePassword Flag che indica se procedere all'aggiornamento della password.
   * @return Mono contenente il DTO {@link User} aggiornato e persistito.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(User userToUpdate, boolean isUpdatePassword) {
    LOG.info(
        "👤 Updating user in database, identifier: {}, updatePassword: {}",
        userToUpdate.getIdentifier(),
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

    UserEntity userFind = userDAO.findUserEntityByIdentifier(userToUpdate.getIdentifier());
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

  @Transactional
  private User saveUserEntityIntoDatabase(UserEntity userEntity) {
    UserEntity userSaved = userDAO.saveAndFlush(userEntity);
    userSaved.setPassword(null);
    UserValidator.validateUser(userSaved);
    return UserMapper.toDTO(userSaved);
  }

  /**
   * Revoca il token di accesso e invalida la sessione dell'utente nel registro token.
   *
   * @param accessTokenData Dati del token di accesso dell'utente.
   * @return Mono vuoto indicante il completamento del logout.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> logout(AccessTokenData accessTokenData) {
    LOG.info("🚪 Logging out user: {}", accessTokenData.getIdentifier());
    accessTokenService.revokeTokenByIdentifier(accessTokenData.getIdentifier());
    return Mono.empty();
  }

  /**
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco
   * ed esegue l'eventuale webhook associato all'azione di cancellazione.
   *
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param bearer Token bearer della richiesta.
   * @param exchange Contesto dello scambio HTTP reattivo.
   * @return Mono con la risposta di avvenuta cancellazione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> deleteUserInternal(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ Deleting user internally from database, identifier: {}", identifier);
    return Mono.fromRunnable(() -> userDAO.deleteUserEntityByIdentifier(identifier))
        .then(
            webhookService.checkAndExecuteWebhook(
                WebhookAction.DELETE_USER, bearer, identifier, exchange))
        .thenReturn(
            new Response(
                HttpStatus.OK.value(),
                "User " + identifier + " successfully deleted",
                TraceUtils.getSpanID(),
                null));
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
   * Elimina un utente dal database locale a partire dal suo DTO.
   *
   * @param user DTO dell'utente da eliminare.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserFromDatabase(User user) {
    UserEntity existingUser = userDAO.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(existingUser)) {
      userDAO.delete(existingUser);
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    LOG.info("👤 [USER INFO] UserInfo process started, include Data: {}", includeUserData);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateAuthType(clientCredential, accessTokenData);
              JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
              switch (decryptToken.getType()) {
                case BEARER -> {
                  return getUserInfo(
                          decryptToken,
                          accessTokenData.getPayload().get("access_token").textValue())
                      .map(
                          user -> {
                            Response response =
                                new Response(
                                    HttpStatus.OK.value(),
                                    "UserInfo Data for " + user.getUsername(),
                                    TraceUtils.getSpanID(),
                                    includeUserData
                                        ? new OAuthTokenResponse(decryptToken, user)
                                        : decryptToken);
                            return ResponseEntity.ok(response);
                          });
                }
                case GOOGLE -> {
                  GoogleModel userInfo =
                      GrpcService.userInfo(
                          accessTokenData.getPayload().get("id_token").textValue(),
                          clientCredential);
                  UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
                  if (includeUserData) {
                    return getUserByEmail(decryptToken.getEmail())
                        .map(
                            user -> {
                              Response response =
                                  new Response(
                                      HttpStatus.OK.value(),
                                      "UserInfo Data for " + user.getUsername(),
                                      TraceUtils.getSpanID(),
                                      new OAuthTokenResponse(decryptToken, user));
                              return ResponseEntity.ok(response);
                            });
                  }
                  Response response =
                      new Response(
                          HttpStatus.OK.value(),
                          "UserInfo Data for " + decryptToken.getSub(),
                          TraceUtils.getSpanID(),
                          decryptToken);
                  return Mono.just(ResponseEntity.ok(response));
                }
                default -> {
                  return UserValidator.defaultErrorOnType();
                }
              }
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [USER INFO] UserInfo process ended, include Data: {}",
                    includeUserData));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> profile(String bearer, ServerHttpRequest request) {
    LOG.info("👤 [USER PROFILE] Getting profile process started");

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateAuthType(clientCredential, accessTokenData);
              JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
              switch (decryptToken.getType()) {
                case BEARER -> {
                  return getUserInfo(
                          decryptToken,
                          accessTokenData.getPayload().get("access_token").textValue())
                      .map(
                          user -> {
                            Response response =
                                new Response(
                                    HttpStatus.OK.value(),
                                    "Profile Data for " + user.getUsername(),
                                    TraceUtils.getSpanID(),
                                    user);
                            return ResponseEntity.ok(response);
                          });
                }
                case GOOGLE -> {
                  GoogleModel userInfo =
                      GrpcService.userInfo(
                          accessTokenData.getPayload().get("id_token").textValue(),
                          clientCredential);
                  UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
                  return getUserByEmail(decryptToken.getEmail())
                      .map(
                          user -> {
                            Response response =
                                new Response(
                                    HttpStatus.OK.value(),
                                    "Profile Data for " + user.getUsername(),
                                    TraceUtils.getSpanID(),
                                    user);
                            return ResponseEntity.ok(response);
                          });
                }
                default -> {
                  return UserValidator.defaultErrorOnType();
                }
              }
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info("✅ [USER PROFILE] Getting profile process ended"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> register(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    LOG.info(
        "👤 [USER REGISTER] Registration process started, username: {}, email: {}",
        user.getUsername(),
        user.getEmail());
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateRegistration(registration_token, clientCredential, user);
              return registerUser(bearer, user, clientCredential, assignNewClient)
                  .map(
                      user1 -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "User " + user.getUsername() + " successfully registered!",
                                TraceUtils.getSpanID(),
                                user1);
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [USER REGISTER] Registration process successfully ended, username: {}, email: {}",
                    user.getUsername(),
                    user.getEmail()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> updateUser(
      User userToUpdate, String bearer, ServerHttpRequest request) {
    userToUpdate.setPassword(null);
    LOG.info(
        "👤 [USER UPDATE] Updating user process started, username: {}, email: {}",
        userToUpdate.getUsername(),
        userToUpdate.getEmail());

    return techUserService
        .hasTechUserRoles(accessTokenData, clientService)
        .flatMap(
            aBoolean -> {
              if (!aBoolean) UserValidator.validateUpdate(accessTokenData, userToUpdate);
              return updateUser(userToUpdate)
                  .flatMap(
                      user -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "User " + user.getUsername() + " successfully updated!",
                                TraceUtils.getSpanID(),
                                user);
                        return Mono.just(ResponseEntity.ok(response));
                      })
                  .doOnSuccess(
                      responseResponseEntity ->
                          LOG.info(
                              "✅ [USER UPDATE] Updating user process ended, username: {}, email: {}",
                              userToUpdate.getUsername(),
                              userToUpdate.getEmail()));
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> unlockUser(String identifier, Boolean block) {
    LOG.info(
        "🔓 [USER UNLOCK] Unlocking user process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can unlock user {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(techUserService.getTech_client_id());

    return clientCredentialMono.flatMap(
        clientCredential -> {
          TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
          return getUserByIdentifier(identifier)
              .flatMap(
                  user -> {
                    user.setBlocked(block);
                    return updateUser(user, false)
                        .flatMap(
                            user1 -> {
                              Response response =
                                  new Response(
                                      HttpStatus.OK.value(),
                                      "User "
                                          + user1.getUsername()
                                          + " successfully "
                                          + (block ? "locked!" : "unlocked!"),
                                      TraceUtils.getSpanID(),
                                      user1);
                              return Mono.just(ResponseEntity.ok(response));
                            });
                  })
              .doOnSuccess(
                  responseResponseEntity ->
                      LOG.info(
                          "✅ [USER UNLOCK] Unlocking user process ended, identifier: {}",
                          identifier));
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    LOG.info(
        "🔑 [PASSWORD RESET] Change password request started, locale: {}, sendEmail: {}",
        locale,
        sendEmail);
    if (!Utilities.isCharacterAndRegexValid(changePassword.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("❌ Invalid regex for field email {}", changePassword.getEmail());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid field email, try again!");
    }

    Mono<User> userMono =
        getUserByEmail(changePassword.getEmail())
            .map(
                user -> {
                  String token = UUID.randomUUID().toString();
                  user.setTokenReset(token);
                  return user;
                });

    String categoryOrId =
        StringUtils.hasText(changePassword.getTemplateId())
            ? changePassword.getTemplateId()
            : "RESET_PASSWORD";
    String requestedClientId = changePassword.getClientId();

    Mono<EmailTemplateDTO> emailTemplateMono =
        emailTemplateService.getTemplateByCategoryLocaleAndClient(
            categoryOrId, locale, requestedClientId);

    return Mono.zip(userMono, emailTemplateMono)
        .flatMap(
            objects -> {
              Mono<User> updatedUserMono = updateUser(objects.getT1());
              EmailContent emailContent =
                  EmailContent.builder()
                      .subject(objects.getT2().getSubject())
                      .to(changePassword.getEmail())
                      .sentDate(new Date())
                      .build();

              Map<String, String> emailParams = TemplateParam.getTemplateParam(objects.getT1());
              if (!ObjectToolkit.isNullOrEmpty(changePassword.getParams()))
                emailParams.putAll(changePassword.getParams());

              if (sendEmail)
                return emailSenderService
                    .sendEmail(objects.getT2(), emailParams, emailContent)
                    .zipWith(updatedUserMono)
                    .flatMap(
                        objects1 -> {
                          String message = "Email Sent! Check your email address!";
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  message,
                                  TraceUtils.getSpanID(),
                                  objects1.getT1());
                          return Mono.just(ResponseEntity.ok(response));
                        });
              else
                return updatedUserMono.flatMap(
                    user -> {
                      EmailResponse responseEmail = new EmailResponse();
                      responseEmail.setToken(user.getTokenReset());
                      String message = "Email Sent! Check your email address!";
                      Response response =
                          new Response(
                              HttpStatus.OK.value(),
                              message,
                              TraceUtils.getSpanID(),
                              responseEmail);
                      return Mono.just(ResponseEntity.ok(response));
                    });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [PASSWORD RESET] Change password request ended, locale: {}, sendEmail: {}",
                    locale,
                    sendEmail));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changePassword(ChangePassword changePassword) {
    LOG.info("🔑 [PASSWORD UPDATE] Change password user process started");
    changePassword.setPassword(
        new String(Base64.getDecoder().decode(changePassword.getPassword())));
    if (!Utilities.isCharacterAndRegexValid(
        changePassword.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("❌ Invalid regex for field password");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid regex for field password");
    }

    String finalPassword = changePassword.getPassword();
    return getUserByTokenReset(changePassword.getToken())
        .flatMap(
            user -> {
              user.setPassword(finalPassword);
              user.setTokenReset(null);
              return updateUser(user, true)
                  .flatMap(
                      user1 -> {
                        String message = "Password Updated!";
                        Response response =
                            new Response(
                                HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);
                        return Mono.just(ResponseEntity.ok(response));
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info("✅ [PASSWORD UPDATE] Change password user process ended"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getUsers() {
    LOG.info("👥 [USER LIST] Get list of users process started");
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can get users");
      throw new OAuthException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(techUserService.getTech_client_id());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
              return getUsersFromDatabase()
                  .flatMap(
                      users -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(), "Users list", TraceUtils.getSpanID(), users);
                        return Mono.just(ResponseEntity.ok(response));
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info("✅ [USER LIST] Get list of users process ended"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ [USER DELETE] Deleting user process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Only a tech user can delete the user {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(techUserService.getTech_client_id());

    return clientCredentialMono.flatMap(
        clientCredential -> {
          TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
          return deleteUserInternal(identifier, bearer, exchange)
              .flatMap(response -> Mono.just(ResponseEntity.ok(response)))
              .doOnSuccess(
                  responseResponseEntity ->
                      LOG.info(
                          "✅ [USER DELETE] Delete user process ended, identifier: {}",
                          identifier));
        });
  }
}
