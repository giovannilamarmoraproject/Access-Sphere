package io.github.giovannilamarmora.accesssphere.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.LoggerFilter;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class DataService {

  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private StrapiService strapiService;
  @Autowired private TokenService tokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByEmail(String email) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, finding user, using email {}", email);
      return strapiService
          .getUserByEmail(email)
          .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
          .onErrorResume(
              throwable -> {
                if (!throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                  LOG.info(
                      "Error on strapi, finding user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByEmail(email);
                  DataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(email);
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> registerUser(User user, ClientCredential clientCredential) {
    user.setRoles(
        ObjectUtils.isEmpty(clientCredential.getDefaultRoles())
            ? null
            : clientCredential.getDefaultRoles().stream().map(AppRole::getRole).toList());
    // Se l'utente non ha password?
    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    userEntity.setIdentifier(identifier);

    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, registering user with email {} on strapi", user.getEmail());
      return strapiService
          .registerUserToStrapi(user, clientCredential)
          .map(
              strapiResponse -> {
                userEntity.setStrapiId(strapiResponse.getUser().getId());
                if (!ObjectUtils.isEmpty(strapiResponse.getUser().getApp_roles()))
                  userEntity.setRoles(
                      String.join(
                          " ",
                          strapiResponse.getUser().getApp_roles().stream()
                              .map(AppRole::getRole)
                              .toList()));
                // Registro l'utente a prescindere che strapi funzioni o meno
                UserEntity userSaved = userDataService.saveAndFlush(userEntity);
                return UserMapper.mapUserEntityToUser(userSaved);
              })
          .onErrorResume(
              throwable -> {
                if (!throwable.getMessage().contains("Email or Username are already taken")) {
                  LOG.info(
                      "Error on strapi, register user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userSaved = userDataService.saveAndFlush(userEntity);
                  DataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
                }
                return Mono.error(throwable);
              })
          .doOnSuccess(user1 -> LOG.info("User {} saved", user1.getUsername()));
    }
    UserEntity userSaved = userDataService.saveAndFlush(userEntity);
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> login(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    if (isStrapiEnabled) {
      LOG.debug(
          "Strapi is enabled, making login via strapi, using identifier {}",
          ObjectUtils.isEmpty(email) ? username : email);
      return strapiService
          .login(ObjectUtils.isEmpty(email) ? username : email, password)
          .flatMap(
              strapiResponse -> {
                User user = StrapiMapper.mapFromStrapiUserToUser(strapiResponse.getUser());
                JWTData jwtData = JWTData.generateJWTData(user, clientCredential, request);
                Map<String, Object> strapiToken = new HashMap<>();
                strapiToken.put("refresh_token", strapiResponse.getRefresh_token());
                strapiToken.put("access_token", strapiResponse.getJwt());
                return getUserInfo(jwtData, strapiResponse.getJwt())
                    .flatMap(
                        user1 -> {
                          jwtData.setRoles(user1.getRoles());
                          AuthToken token =
                              tokenService.generateToken(jwtData, clientCredential, strapiToken);
                          strapiToken.put("expires_at", jwtData.getExp());
                          return Mono.just(
                              new OAuthTokenResponse(
                                  token,
                                  Utils.mapper().convertValue(strapiToken, JsonNode.class),
                                  jwtData,
                                  user1));
                        });
              })
          .onErrorResume(
              throwable -> {
                if (!throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_401.getMessage())) {
                  LOG.error("Error on strapi, login via database");
                  return Mono.just(
                      performLoginViaDatabase(
                          username, email, password, clientCredential, request));
                }
                return Mono.error(throwable);
              });
    }
    return Mono.just(performLoginViaDatabase(username, email, password, clientCredential, request));
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
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData = JWTData.generateJWTData(userEntityToUser, clientCredential, request);
    AuthToken token = tokenService.generateToken(jwtData, clientCredential, null);
    LOG.debug("Login process via Database ended for username={} and email={}", username, email);
    return new OAuthTokenResponse(userEntityToUser, jwtData, token);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserInfo(JWTData jwtData, String token) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, getting userInfo");
      return strapiService
          .userInfo(token)
          .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
          .onErrorResume(
              throwable -> {
                if (!throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_401.getMessage())) {
                  LOG.info(
                      "Error on strapi, getting userInfo into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByEmail(jwtData.getEmail());
                  DataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(jwtData.getEmail());
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> refreshJWTToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      String token,
      ServerHttpRequest request) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, getting refresh token");
      return strapiService
          .refreshJWToken(token)
          .flatMap(
              strapiResponse -> {
                JWTData jwtData = new JWTData();
                jwtData.setEmail(accessTokenData.getEmail());
                Map<String, Object> strapiToken = new HashMap<>();
                strapiToken.put("refresh_token", strapiResponse.getRefresh_token());
                strapiToken.put("access_token", strapiResponse.getJwt());
                return getUserInfo(jwtData, strapiResponse.getJwt())
                    .map(
                        user1 -> {
                          JWTData jwtDataFinal =
                              JWTData.generateJWTData(user1, clientCredential, request);
                          jwtDataFinal.setRoles(user1.getRoles());
                          AuthToken authToken =
                              tokenService.generateToken(
                                  jwtDataFinal, clientCredential, strapiToken);
                          strapiToken.put("expires_at", jwtDataFinal.getExp());
                          return new OAuthTokenResponse(
                              authToken,
                              Utils.mapper().convertValue(strapiToken, JsonNode.class),
                              jwtDataFinal,
                              user1);
                        });
              })
          .onErrorResume(
              throwable -> {
                if (!throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_401.getMessage())) {
                  LOG.info(
                      "Error on strapi, getting refresh token into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity =
                      userDataService.findUserEntityByEmail(accessTokenData.getEmail());
                  DataValidator.validateUser(userEntity);
                  User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
                  userEntityToUser.setPassword(null);
                  JWTData jwtData =
                      JWTData.generateJWTData(userEntityToUser, clientCredential, request);
                  AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
                  return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(accessTokenData.getEmail());
    DataValidator.validateUser(userEntity);
    User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData = JWTData.generateJWTData(userEntityToUser, clientCredential, request);
    AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
    return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> updateUser(User userToUpdate, String strapiJwt) {

    if (userToUpdate.getPassword() != null && !userToUpdate.getPassword().isBlank()) {
      userToUpdate.setPassword(new String(Base64.getDecoder().decode(userToUpdate.getPassword())));
      if (!Utils.checkCharacterAndRegexValid(
          userToUpdate.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
        LOG.error("Invalid regex for field password for user {}", userToUpdate.getUsername());
        throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
      }
    }
    // Se l'utente non ha password?
    UserEntity userEntity = UserMapper.mapUserToUserEntity(userToUpdate);
    userEntity.setPassword(bCryptPasswordEncoder.encode(userToUpdate.getPassword()));

    if (isStrapiEnabled) {
      LOG.debug(
          "Strapi is enabled, updating user with email {} on strapi", userToUpdate.getEmail());
      return strapiService
          .updateUser(userToUpdate, strapiJwt)
          .map(
              strapiUser -> {
                userEntity.setStrapiId(strapiUser.getId());
                if (!ObjectUtils.isEmpty(userToUpdate.getRoles()))
                  userEntity.setRoles(String.join(" ", userToUpdate.getRoles()));
                // Registro l'utente a prescindere che strapi funzioni o meno
                UserEntity userSaved =
                    userDataService.updateUserEntityByIdentifier(
                        userEntity, userToUpdate.getIdentifier());
                return UserMapper.mapUserEntityToUser(userSaved);
              })
          .onErrorResume(
              throwable -> {
                if (!throwable.getMessage().contains("NotFoundError")) {
                  LOG.info(
                      "Error on strapi, update user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userSaved =
                      userDataService.updateUserEntityByIdentifier(
                          userEntity, userToUpdate.getIdentifier());
                  DataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
                }
                return Mono.error(throwable);
              })
          .doOnSuccess(user1 -> LOG.info("User {} updated", user1.getUsername()));
    }
    UserEntity userSaved =
        userDataService.updateUserEntityByIdentifier(userEntity, userToUpdate.getIdentifier());
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
  }
}
