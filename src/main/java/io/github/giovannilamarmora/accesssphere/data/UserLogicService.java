package io.github.giovannilamarmora.accesssphere.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.auth.MFAAuthenticationService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.accesssphere.webhooks.WebhookService;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.WebhookAction;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public class UserLogicService {

  @Autowired private TechUserService techUserService;

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private MFAAuthenticationService mfaAuthenticationService;
  @Autowired private WebhookService webhookService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByIdentifier(String identifier) {
    UserEntity userEntity = userDataService.findUserEntityByIdentifier(identifier);
    UserDataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByEmail(String email) {
    UserEntity userEntity = userDataService.findUserEntityByEmail(email);
    UserDataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<Void> logout(AccessTokenData accessTokenData) {
    accessTokenService.revokeTokenByIdentifier(accessTokenData.getIdentifier());
    return Mono.empty();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<List<User>> getUsers() {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting all users in database");
    List<UserEntity> userEntities = userDataService.findAll();
    return Mono.just(UserMapper.toDTOList(userEntities));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByTokenReset(String tokenReset) {
    UserEntity userEntity = userDataService.findUserEntityByTokenReset(tokenReset);
    UserDataValidator.validateUser(userEntity);
    UserDataValidator.validateResetToken(userEntity.getUpdateDate());
    return Mono.just(UserMapper.toDTO(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> registerUser(
      String bearer, User user, ClientCredential clientCredential, Boolean assignNewClient) {
    List<AppRole> userRoles =
        clientCredential.getAppRoles().stream()
            .filter(appRole -> appRole.getType().equalsIgnoreCase("default"))
            .toList();

    if (!ObjectToolkit.isNullOrEmpty(bearer)) {
      LOG.info("Bearer token present, checking technical roles");
      AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

      if (techUserService.hasTechUserRoles(accessTokenData, clientCredential)
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
          "Default roles not present on client configuration under client_id {}",
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

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<OAuthTokenResponse> login(
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

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserInfo(JWTData jwtData, String token) {
    if (!techUserService.isTechUser()) {
      UserEntity userEntity = userDataService.findUserEntityByEmail(jwtData.getEmail());
      UserDataValidator.validateUser(userEntity);
      return Mono.just(UserMapper.toDTO(userEntity));
    }
    return techUserService.userInfo(jwtData);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<OAuthTokenResponse> refreshJWTToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      String token,
      ServerHttpRequest request) {
    UserEntity userEntity = userDataService.findUserEntityByEmail(accessTokenData.getEmail());
    UserDataValidator.validateUser(userEntity);
    User userEntityToUser = UserMapper.toDTO(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData =
        JWTData.generateJWTData(
            userEntityToUser, clientCredential, accessTokenData.getSubjectType(), request);
    AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
    return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> updateUser(
      User userToUpdate, boolean isUpdatePassword) {
    UserEntity userEntity = UserMapper.toEntity(userToUpdate);

    if (isUpdatePassword) {
      if (userToUpdate.getPassword() != null && !userToUpdate.getPassword().isBlank()) {
        if (!Utilities.isCharacterAndRegexValid(
            userToUpdate.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
          LOG.error("Invalid regex for field password for user {}", userToUpdate.getUsername());
          throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
        }
        userEntity.setPassword(bCryptPasswordEncoder.encode(userToUpdate.getPassword()));
      }
    }

    UserEntity userFind = userDataService.findUserEntityByIdentifier(userToUpdate.getIdentifier());
    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
    return Mono.just(saveUserEntityIntoDatabase(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<Response> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange) {

    return Mono.fromRunnable(() -> userDataService.deleteByIdentifier(identifier))
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

  protected void saveUserIntoDatabase(User user) {
    UserEntity userEntity = UserMapper.toEntity(user);
    saveUserEntityIntoDatabase(userEntity);
  }

  protected void updateUserIntoDatabase(User user) {
    UserEntity userSaved = userDataService.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(userSaved)) {
      // Aggiornare i campi dell'entità esistente con i valori del ClientCredential
      UserMapper.updateEntity(userSaved, user);
      saveUserEntityIntoDatabase(userSaved);
    } else {
      // Se il client non esiste, possiamo decidere di aggiungerlo o lanciare un'eccezione
      throw new IllegalArgumentException("User not found: " + userSaved.getIdentifier());
    }
    UserMapper.toDTO(userSaved);
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected void deleteUserFromDatabase(User user) {
    // Trovare l'entità esistente nel database
    UserEntity existingUser = userDataService.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(existingUser)) {
      userDataService.delete(existingUser);
    }
  }

  private OAuthTokenResponse performLoginViaDatabase(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    LOG.debug("Login process via Database started for username={} and email={}", username, email);
    UserEntity userEntity = userDataService.findUserEntityByUsernameOrEmail(username, email);

    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("No data where found od database for the user {}, with email {}", username, email);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    boolean matches = bCryptPasswordEncoder.matches(password, userEntity.getPassword());
    if (!matches) {
      LOG.error("An error happen during bCryptPasswordEncoder.matches, the password do not match");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
    User userEntityToUser = UserMapper.toDTO(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData =
        JWTData.generateJWTData(userEntityToUser, clientCredential, SubjectType.CUSTOMER, request);
    AuthToken token = tokenService.generateToken(jwtData, clientCredential, null);
    LOG.debug("Login process via Database ended for username={} and email={}", username, email);
    return new OAuthTokenResponse(userEntityToUser, jwtData, token);
  }

  @Transactional
  private User saveUserEntityIntoDatabase(UserEntity userEntity) {
    UserEntity userSaved = userDataService.saveAndFlush(userEntity);
    userSaved.setPassword(null);
    UserDataValidator.validateUser(userSaved);
    return UserMapper.toDTO(userSaved);
  }

  private void setDataBeforeUpdate(
      UserEntity userEntity, UserEntity userFind, boolean isUpdatePassword) {
    if (ObjectToolkit.isNullOrEmpty(userFind)) return;
    if (!ObjectToolkit.isNullOrEmpty(userFind.getId())) userEntity.setId(userFind.getId());
    userEntity.setIdentifier(userFind.getIdentifier());
    if (!isUpdatePassword) userEntity.setPassword(userFind.getPassword());
    userEntity.setRoles(userFind.getRoles());
  }
}
