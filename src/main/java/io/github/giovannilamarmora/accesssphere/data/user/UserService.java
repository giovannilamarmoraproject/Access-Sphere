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
   * Recupera un utente a partire dal suo identificatore univoco dal database locale. Valida
   * l'esistenza dell'entita' tramite {@link UserValidator#validateUser(UserEntity)} e mappa
   * l'entita' persistita nel DTO {@link User}.
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
  public Mono<List<User>> getUsersFromDatabase() {
    LOG.info("👥 Getting all users from database");
    List<UserEntity> userEntities = userDAO.findAll();
    return Mono.just(UserMapper.toDTOList(userEntities));
  }

  /**
   * Registra un nuovo utente nel database locale, validando o assegnando i ruoli applicativi
   * definiti sul client OAuth. Se la richiesta proviene da un utente tecnico autenticato, consente
   * l'assegnazione mirata dei ruoli specificati nel payload. Effettua l'hashing bcrypt della
   * password prima del salvataggio.
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
   * Esegue l'autenticazione dell'utente tramite credenziali locali (username/email e password) o
   * delega a {@link TechUserService} nel caso si tratti di un utente tecnico. Genera la risposta
   * con token OAuth2 completo e dati JWT associati.
   *
   * @param username Username dell'utente (opzionale se specificata email).
   * @param email Indirizzo email dell'utente (opzionale se specificato username).
   * @param password Password in chiaro da validare con BCrypt.
   * @param clientCredential Credenziali del client OAuth che richiede l'autenticazione.
   * @param request Richiesta HTTP reattiva per il tracciamento e la compilazione dei claim JWT.
   * @return Mono contenente l'oggetto {@link OAuthTokenResponse} generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> performLogin(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    LOG.debug(
        "🔑 Login process via Database started for username={} and email={}", username, email);
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
    return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, token));
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
    // if (!techUserService.isTechUser()) {
    UserEntity userEntity = userDAO.findUserEntityByEmail(jwtData.getEmail());
    UserValidator.validateUser(userEntity);
    return Mono.just(UserMapper.toDTO(userEntity));
    // }
    // return techUserService.userInfo(jwtData);
  }

  /**
   * Esegue il rinnovo del token JWT a partire dai dati della sessione di accesso esistente e dalle
   * credenziali del client.
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
   * Aggiorna i dati di un utente nel database locale, gestendo opzionalmente la cifratura e
   * l'aggiornamento della password con validazione tramite regex.
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
   * Cancella fisicamente un utente dal database locale tramite il suo identificatore univoco ed
   * esegue l'eventuale webhook associato all'azione di cancellazione.
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

  /**
   * Recupera le informazioni OIDC / UserInfo dell'utente autenticato basandosi sui claim del token Bearer.
   * Interconnessioni logiche:
   * - Interroga {@link ClientService} per risolvere la configurazione e i permessi del client chiamante.
   * - Valida la conformita' del tipo di autenticazione tramite {@link UserValidator#validateAuthType}.
   * - Decifra e convalida la firma del token tramite {@link TokenService#parseToken}.
   * - A seconda del provider (BEARER interno o federato GOOGLE tramite {@link GrpcService}), recupera l'utente
   *   dal database locale ({@link #getUserInfo} o {@link #getUserByEmail}) e compone il payload di risposta.
   * - Se il parametro query 'include_user_data' e' abilitato, integra nel payload sia i claim JWT sia il profilo utente.
   *
   * @param bearer Token di accesso Bearer inviato dal client nell'header Authorization.
   * @param request Richiesta HTTP server per l'estrazione dei query param (es. include_user_data).
   * @return Mono contenente la {@link Response} applicativa con i dettagli UserInfo o JWT.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> userInfo(String bearer, ServerHttpRequest request) {
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    LOG.info("👤 [USER INFO] Starting UserInfo retrieval (includeUserData: {})", includeUserData);

    return clientService
        .getClientCredentialByClientID(accessTokenData.getClientId())
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
                          user ->
                              new Response(
                                  HttpStatus.OK.value(),
                                  "UserInfo Data for " + user.getUsername(),
                                  TraceUtils.getSpanID(),
                                  includeUserData
                                      ? new OAuthTokenResponse(decryptToken, user)
                                      : decryptToken));
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
                            user ->
                                new Response(
                                    HttpStatus.OK.value(),
                                    "UserInfo Data for " + user.getUsername(),
                                    TraceUtils.getSpanID(),
                                    new OAuthTokenResponse(decryptToken, user)));
                  }
                  return Mono.just(
                      new Response(
                          HttpStatus.OK.value(),
                          "UserInfo Data for " + decryptToken.getSub(),
                          TraceUtils.getSpanID(),
                          decryptToken));
                }
                default -> {
                  return UserValidator.defaultErrorOnType();
                }
              }
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [USER INFO] UserInfo process completed successfully (includeUserData: {})",
                    includeUserData));
  }

  /**
   * Recupera i dati anagrafici e di profilo dell'utente autenticato per la sessione corrente.
   * Interconnessioni logiche:
   * - Interroga {@link ClientService} per risolvere le credenziali del client.
   * - Valida la tipologia di autorizzazione tramite {@link UserValidator#validateAuthType}.
   * - Decodifica i token JWT tramite {@link TokenService#parseToken}.
   * - Risolve il profilo utente locale dal database (via {@link #getUserInfo} per token Bearer o
   *   via {@link #getUserByEmail} previa validazione token federato per account Google).
   *
   * @param bearer Token di accesso Bearer fornito dal client.
   * @param request Richiesta HTTP server.
   * @return Mono contenente la {@link Response} applicativa con i dati del profilo utente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> profile(String bearer, ServerHttpRequest request) {
    LOG.info("👤 [USER PROFILE] Starting user profile retrieval process");

    return clientService
        .getClientCredentialByClientID(accessTokenData.getClientId())
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
                          user ->
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Profile Data for " + user.getUsername(),
                                  TraceUtils.getSpanID(),
                                  user));
                }
                case GOOGLE -> {
                  GoogleModel userInfo =
                      GrpcService.userInfo(
                          accessTokenData.getPayload().get("id_token").textValue(),
                          clientCredential);
                  UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
                  return getUserByEmail(decryptToken.getEmail())
                      .map(
                          user ->
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Profile Data for " + user.getUsername(),
                                  TraceUtils.getSpanID(),
                                  user));
                }
                default -> {
                  return UserValidator.defaultErrorOnType();
                }
              }
            })
        .doOnSuccess(res -> LOG.info("✅ [USER PROFILE] User profile retrieval completed successfully"));
  }

  /**
   * Registra un nuovo utente nel sistema IAM validando credenziali, regex, token di registrazione e ruoli applicativi.
   * Interconnessioni logiche:
   * - Interroga {@link ClientService#getClientCredentialByClientID(String)} per recuperare la configurazione del client.
   * - Valida la richiesta di registrazione tramite {@link UserValidator#validateRegistration}.
   * - Invoca il metodo interno {@link #registerUser(String, User, ClientCredential, Boolean)} per gestire l'hashing BCrypt,
   *   l'assegnazione dei ruoli di default e l'eventuale concessione di ruoli tecnici privilegiati da {@link TechUserService}.
   * - Incapsula il DTO dell'utente registrato nella {@link Response}.
   *
   * @param bearer Token Bearer opzionale per operazioni eseguite da operatori tecnici.
   * @param user DTO contenente anagrafica e credenziali del nuovo utente.
   * @param clientId Identificatore del client OAuth associato.
   * @param registration_token Token segreto di registrazione verificato rispetto alla configurazione del client.
   * @param assignNewClient Flag facoltativo per associare nuove credenziali.
   * @return Mono contenente la {@link Response} applicativa con l'utente registrato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> register(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    LOG.info(
        "👤 [USER REGISTER] Starting user registration for username: {}, email: {}, clientId: {}",
        user.getUsername(),
        user.getEmail(),
        clientId);

    return clientService
        .getClientCredentialByClientID(clientId)
        .flatMap(
            clientCredential -> {
              UserValidator.validateRegistration(registration_token, clientCredential, user);
              return registerUser(bearer, user, clientCredential, assignNewClient)
                  .map(
                      user1 ->
                          new Response(
                              HttpStatus.CREATED.value(),
                              "User " + user.getUsername() + " successfully registered!",
                              TraceUtils.getSpanID(),
                              user1));
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [USER REGISTER] Registration successfully completed for username: {}, email: {}",
                    user.getUsername(),
                    user.getEmail()));
  }

  /**
   * Aggiorna i dati anagrafici di un utente nel database locale, con verifica dei privilegi e dell'identita'.
   * Interconnessioni logiche:
   * - Verifica tramite {@link TechUserService#hasTechUserRoles} se l'utente in sessione possiede ruoli tecnici amministrativi.
   * - Se non e' un utente tecnico, verifica che l'identificatore del token corrisponda all'utente da aggiornare con {@link UserValidator#validateUpdate}.
   * - Esegue la persistenza delle modifiche sul database tramite {@link #updateUser(User)} preservando password e ruoli.
   * - Incapsula il DTO utente aggiornato nella {@link Response}.
   *
   * @param userToUpdate DTO con i dati anagrafici da aggiornare.
   * @param bearer Token di autorizzazione Bearer.
   * @param request Richiesta HTTP server.
   * @return Mono contenente la {@link Response} applicativa con i dati dell'utente aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> updateUser(
      User userToUpdate, String bearer, ServerHttpRequest request) {
    userToUpdate.setPassword(null);
    LOG.info(
        "👤 [USER UPDATE] Starting user update for username: {}, identifier: {}",
        userToUpdate.getUsername(),
        userToUpdate.getIdentifier());

    return techUserService
        .hasTechUserRoles(accessTokenData, clientService)
        .flatMap(
            hasTechRoles -> {
              if (!hasTechRoles) {
                UserValidator.validateUpdate(accessTokenData, userToUpdate);
              }
              return updateUser(userToUpdate)
                  .map(
                      user ->
                          new Response(
                              HttpStatus.OK.value(),
                              "User " + user.getUsername() + " successfully updated!",
                              TraceUtils.getSpanID(),
                              user));
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [USER UPDATE] User update successfully completed for username: {}, identifier: {}",
                    userToUpdate.getUsername(),
                    userToUpdate.getIdentifier()));
  }

  /**
   * Blocca o sblocca lo stato di accesso di un utente (operazione protetta riservata a utente tecnico).
   * Interconnessioni logiche:
   * - Verifica che il chiamante sia un utente tecnico autorizzato tramite {@link TechUserService#isTechUser}.
   * - Valida i ruoli tecnici sulle credenziali del client amministrativo tramite {@link TechUserValidator#validateTechRoles}.
   * - Recupera l'utente dal database tramite {@link #getUserByIdentifier(String)}.
   * - Modifica il flag 'blocked' e persiste l'aggiornamento tramite {@link #updateUser(User, boolean)}.
   *
   * @param identifier UUID univoco dell'utente da bloccare o sbloccare.
   * @param block True per bloccare l'accesso all'utente, false per sbloccarlo.
   * @return Mono contenente la {@link Response} applicativa con il DTO dell'utente aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> unlockUser(String identifier, Boolean block) {
    LOG.info(
        "🛡️ [USER STATUS] Starting user {} process for identifier: {}",
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
              return getUserByIdentifier(identifier)
                  .flatMap(
                      user -> {
                        user.setBlocked(block);
                        return updateUser(user, false)
                            .map(
                                updatedUser ->
                                    new Response(
                                        HttpStatus.OK.value(),
                                        "User "
                                            + updatedUser.getUsername()
                                            + " successfully "
                                            + (block ? "locked!" : "unlocked!"),
                                        TraceUtils.getSpanID(),
                                        updatedUser));
                      });
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [USER STATUS] User {} process completed successfully for identifier: {}",
                    block ? "lock" : "unlock",
                    identifier));
  }

  /**
   * Avvia la procedura di richiesta reset password generando un token univoco di reset.
   * Se sendEmail e' abilitato, invia l'email tramite il servizio di notifica dedicato;
   * in caso contrario restituisce il token direttamente nel payload (utile per ambienti dev/test).
   * Interconnessioni logiche:
   * - Convalida l'indirizzo email con regex {@link RegEx#EMAIL}.
   * - Recupera l'utente dal database con {@link #getUserByEmail(String)}.
   * - Genera un nuovo UUID token di reset e lo persiste con {@link #updateUser(User)}.
   * - Recupera il template email con {@link EmailTemplateService#getTemplateByCategoryLocaleAndClient}.
   * - Se sendEmail e' true, delega a {@link EmailSenderService#sendEmail} per il recapito asincrono.
   *
   * @param changePassword DTO con email del destinatario, clientId e templateId opzionale.
   * @param locale Lingua di localizzazione per il template di posta.
   * @param sendEmail Flag che controlla l'invio fisico del messaggio email.
   * @return Mono contenente la {@link Response} applicativa con i dettagli di spedizione o il token generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    LOG.info(
        "🔑 [PASSWORD RESET] Starting password reset request for email: {}, locale: {}, sendEmail: {}",
        changePassword.getEmail(),
        locale,
        sendEmail);
    if (!Utilities.isCharacterAndRegexValid(changePassword.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("❌ Invalid email format: {}", changePassword.getEmail());
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

              if (sendEmail) {
                return emailSenderService
                    .sendEmail(objects.getT2(), emailParams, emailContent)
                    .zipWith(updatedUserMono)
                    .map(
                        objects1 ->
                            new Response(
                                HttpStatus.OK.value(),
                                "Email Sent! Check your email address!",
                                TraceUtils.getSpanID(),
                                objects1.getT1()));
              } else {
                return updatedUserMono.map(
                    user -> {
                      EmailResponse responseEmail = new EmailResponse();
                      responseEmail.setToken(user.getTokenReset());
                      return new Response(
                          HttpStatus.OK.value(),
                          "Email Sent! Check your email address!",
                          TraceUtils.getSpanID(),
                          responseEmail);
                    });
              }
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [PASSWORD RESET] Password reset request successfully processed for email: {}",
                    changePassword.getEmail()));
  }

  /**
   * Esegue l'aggiornamento della password a fronte di un token di recupero valido.
   * Interconnessioni logiche:
   * - Decodifica la password dal formato Base64 e valida la complessita' tramite regex {@link RegEx#PASSWORD_FULL}.
   * - Recupera l'utente associato al token con {@link #getUserByTokenReset(String)}, convalidandone la validita' entro le 24 ore.
   * - Resetta il token di recupero, cifra la nuova password con BCrypt e aggiorna il record con {@link #updateUser(User, boolean)}.
   *
   * @param changePassword DTO contenente il token di recupero e la nuova password codificata in Base64.
   * @return Mono contenente la {@link Response} applicativa di conferma.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> changePassword(ChangePassword changePassword) {
    LOG.info("🔑 [PASSWORD UPDATE] Starting password change execution via reset token");
    try {
      changePassword.setPassword(
          new String(Base64.getDecoder().decode(changePassword.getPassword())));
    } catch (IllegalArgumentException e) {
      LOG.error("❌ Failed to decode Base64 password: {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid Base64 password encoding");
    }

    if (!Utilities.isCharacterAndRegexValid(
        changePassword.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("❌ Invalid password complexity regex check failed");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid regex for field password");
    }

    String finalPassword = changePassword.getPassword();
    return getUserByTokenReset(changePassword.getToken())
        .flatMap(
            user -> {
              user.setPassword(finalPassword);
              user.setTokenReset(null);
              return updateUser(user, true)
                  .map(
                      user1 ->
                          new Response(
                              HttpStatus.OK.value(),
                              "Password Updated!",
                              TraceUtils.getSpanID(),
                              null));
            })
        .doOnSuccess(res -> LOG.info("✅ [PASSWORD UPDATE] Password successfully changed"));
  }

  /**
   * Recupera la lista di tutti gli utenti registrati (operazione protetta riservata a utente tecnico).
   * Interconnessioni logiche:
   * - Verifica che il chiamante sia un utente tecnico autorizzato tramite {@link TechUserService#isTechUser}.
   * - Valida i ruoli tecnici sulle credenziali del client amministrativo tramite {@link TechUserValidator#validateTechRoles}.
   * - Recupera tutti gli utenti dal database con {@link #getUsersFromDatabase()}.
   * - Incapsula la lista di DTO utente nel payload della {@link Response}.
   *
   * @return Mono contenente la {@link Response} applicativa con la lista degli utenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> getUsers() {
    LOG.info("👥 [USER LIST] Starting retrieval of all users");
    if (!techUserService.isTechUser()) {
      LOG.error("❌ Forbidden: Only a tech user can retrieve the list of users");
      throw new OAuthException(ExceptionMap.ERR_TECH_403, ExceptionMap.ERR_TECH_403.getMessage());
    }

    return clientService
        .getClientCredentialByClientID(techUserService.getTechClientId())
        .flatMap(
            clientCredential -> {
              TechUserValidator.validateTechRoles(clientCredential, accessTokenData.getRoles());
              return getUsersFromDatabase()
                  .map(
                      users ->
                          new Response(
                              HttpStatus.OK.value(), "Users list", TraceUtils.getSpanID(), users));
            })
        .doOnSuccess(res -> LOG.info("✅ [USER LIST] Successfully retrieved all users"));
  }

  /**
   * Cancella definitivamente un utente dal sistema IAM (operazione protetta riservata a utente tecnico)
   * ed esegue il webhook associato.
   * Interconnessioni logiche:
   * - Verifica che il chiamante sia un utente tecnico tramite {@link TechUserService#isTechUser}.
   * - Valida i ruoli tecnici sulle credenziali del client amministrativo tramite {@link TechUserValidator#validateTechRoles}.
   * - Invoca il metodo interno {@link #deleteUserInternal(String, String, ServerWebExchange)} per la rimozione fisica
   *   dal database tramite {@link IUserDAO} e l'esecuzione asincrona del webhook di notifica DELETE_USER.
   *
   * @param identifier UUID univoco dell'utente da cancellare.
   * @param bearer Token di autorizzazione dell'utente tecnico.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono contenente la {@link Response} applicativa di avvenuta eliminazione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Response> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🗑️ [USER DELETE] Starting user deletion process for identifier: {}", identifier);
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
            res ->
                LOG.info(
                    "✅ [USER DELETE] User deletion successfully completed for identifier: {}",
                    identifier));
  }
}
