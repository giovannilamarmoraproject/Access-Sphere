package io.github.giovannilamarmora.accesssphere.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
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
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class UserLogicService {

  @Getter
  @Setter
  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  @Autowired private TechUserService techUserService;

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private StrapiService strapiService;
  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private MFAAuthenticationService mfaAuthenticationService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByIdentifier(String identifier, boolean getStrapiId) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, finding user, using identifier: {}", identifier);
      return strapiService
          .getUserByIdentifier(identifier)
          .flatMap(
              strapiUser -> {
                User userToReturn = StrapiMapper.mapFromStrapiUserToUser(strapiUser);
                if (getStrapiId) userToReturn.setId(strapiUser.getId());
                return Mono.just(userToReturn);
              })
          .onErrorResume(
              throwable -> {
                if (!OAuthException.isHandleException(throwable)) {
                  LOG.info(
                      "Error on strapi, finding user by identifier into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByIdentifier(identifier);
                  UserDataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByIdentifier(identifier);
    UserDataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByEmail(String email, boolean getStrapiId) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, finding user, using email {}", email);
      return strapiService
          .getUserByEmail(email)
          .flatMap(
              strapiUser -> {
                User userToReturn = StrapiMapper.mapFromStrapiUserToUser(strapiUser);
                if (getStrapiId) userToReturn.setId(strapiUser.getId());
                return Mono.just(userToReturn);
              })
          .onErrorResume(
              throwable -> {
                // if (!throwable
                //    .getMessage()
                //    .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                if (!OAuthException.isHandleException(throwable)) {
                  LOG.info(
                      "Error on strapi, finding user by email into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByEmail(email);
                  UserDataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(email);
    UserDataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<Void> logout(String refresh_token, AccessTokenData accessTokenData) {
    if (isStrapiEnabled && !ObjectUtils.isEmpty(refresh_token)) {
      LOG.debug("Strapi is enabled, logout user");
      return strapiService
          .logout(refresh_token)
          .doOnSuccess(
              unused -> {
                accessTokenService.revokeTokenByIdentifier(accessTokenData.getIdentifier());
              })
          .then();
    }
    accessTokenService.revokeTokenByIdentifier(accessTokenData.getIdentifier());
    return Mono.empty();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<List<User>> getUsers() {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting all users in database");
    List<UserEntity> userEntities = userDataService.findAll();
    return Mono.just(UserMapper.mapUserEntitiesToUsers(userEntities));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<List<User>> getStrapiUsers(String strapi_bearer) {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting all users in strapi");

    return strapiService
        .getUsers(strapi_bearer)
        .flatMap(strapiUsers -> Mono.just(StrapiMapper.mapFromStrapiUsersToUsers(strapiUsers)));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserByTokenReset(String tokenReset) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, finding user, using token {}", tokenReset);
      return strapiService
          .getUserByTokenReset(tokenReset)
          .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
          .onErrorResume(
              throwable -> {
                if (throwable instanceof OAuthException) {
                  return Mono.error(throwable);
                } else {
                  LOG.info(
                      "Error on strapi, finding user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByTokenReset(tokenReset);
                  UserDataValidator.validateUser(userEntity);
                  UserDataValidator.validateResetToken(userEntity.getUpdateDate());
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByTokenReset(tokenReset);
    UserDataValidator.validateUser(userEntity);
    UserDataValidator.validateResetToken(userEntity.getUpdateDate());
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
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
    //  user.setRoles(clientCredential.getDefaultRole().stream().map(AppRole::getRole).toList());
    user.setRoles(userRoles.stream().map(AppRole::getRole).toList());
    // Se l'utente non ha password?
    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    if (!ObjectToolkit.isNullOrEmpty(user.getPassword()))
      userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    String identifier = UUID.randomUUID().toString();
    user.setIdentifier(identifier);
    userEntity.setIdentifier(identifier);

    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, registering user with email {} on strapi", user.getEmail());
      List<AppRole> finalUserRoles = userRoles;
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
                return saveUserEntityIntoDatabase(userEntity);
              })
          .onErrorResume(
              throwable -> {
                if (!OAuthException.isHandleException(throwable)) {
                  LOG.info(
                      "Error on strapi, register user into database, message is {}",
                      throwable.getMessage());
                  return Mono.just(saveUserEntityIntoDatabase(userEntity));
                }
                if (assignNewClient) {
                  return strapiService
                      .getUserByEmail(user.getEmail())
                      .flatMap(
                          strapiUser -> {
                            // Controlla se almeno un ruolo di userRoles è già presente in
                            // strapiUser.getApp_roles()
                            boolean hasRole =
                                finalUserRoles.stream()
                                    .anyMatch(role -> strapiUser.getApp_roles().contains(role));

                            if (hasRole) {
                              LOG.error(
                                  "The current user already has one of the roles for {}",
                                  clientCredential.getClientId());
                              return Mono.error(throwable);
                            }

                            // Aggiunge tutti i ruoli della lista userRoles a
                            // strapiUser.getApp_roles()
                            strapiUser.getApp_roles().addAll(finalUserRoles);
                            // User userToUpdate = StrapiMapper.mapFromStrapiUserToUser(strapiUser);
                            return strapiService
                                .updateUser(strapiUser)
                                .flatMap(
                                    strapiUser1 ->
                                        Mono.just(
                                            StrapiMapper.mapFromStrapiUserToUser(strapiUser)));
                          })
                      .onErrorResume(throwable1 -> Mono.error(throwable));
                }
                return Mono.error(throwable);
              })
          .doOnSuccess(user1 -> LOG.info("User {} saved", user1.getUsername()));
    }
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
      if (isStrapiEnabled) {
        LOG.debug(
            "Strapi is enabled, making login via strapi, using identifier {}",
            ObjectUtils.isEmpty(email) ? username : email);
        return strapiService
            .login(ObjectUtils.isEmpty(email) ? username : email, password)
            .flatMap(
                strapiResponse -> {
                  User user = StrapiMapper.mapFromStrapiUserToUser(strapiResponse.getUser());
                  JWTData jwtData =
                      JWTData.generateJWTData(
                          user, clientCredential, SubjectType.CUSTOMER, request);
                  Map<String, Object> strapiToken = new HashMap<>();
                  strapiToken.put("refresh_token", strapiResponse.getRefresh_token());
                  strapiToken.put(
                      TokenData.STRAPI_ACCESS_TOKEN.getToken(), strapiResponse.getJwt());

                  return mfaAuthenticationService
                      .checkMFAAndMakeLogin(strapiToken, jwtData, user, clientCredential, request)
                      .switchIfEmpty(
                          getUserInfo(jwtData, strapiResponse.getJwt())
                              .flatMap(
                                  user1 -> {
                                    jwtData.setRoles(user1.getRoles());
                                    AuthToken token =
                                        tokenService.generateToken(
                                            jwtData, clientCredential, strapiToken);
                                    strapiToken.put("expires_at", jwtData.getExp());
                                    return Mono.just(
                                        new OAuthTokenResponse(
                                            token,
                                            Utils.mapper()
                                                .convertValue(strapiToken, JsonNode.class),
                                            jwtData,
                                            user1));
                                  }));
                })
            .onErrorResume(
                throwable -> {
                  if (!OAuthException.isHandleException(throwable)) {
                    LOG.error("Error on strapi, login via database");
                    return Mono.just(
                        performLoginViaDatabase(
                            username, email, password, clientCredential, request));
                  }
                  return Mono.error(throwable);
                });
      }
      return Mono.just(
          performLoginViaDatabase(username, email, password, clientCredential, request));
    } else return techUserService.loginTechUser(username, clientCredential, request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> getUserInfo(JWTData jwtData, String token) {
    if (!techUserService.isTechUser()) {
      if (isStrapiEnabled) {
        LOG.debug("Strapi is enabled, getting userInfo");
        return strapiService
            .userInfo(token)
            .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
            .onErrorResume(
                throwable -> {
                  if (!OAuthException.isHandleException(throwable)) {
                    LOG.info(
                        "Error on strapi, getting userInfo into database, message is {}",
                        throwable.getMessage());
                    UserEntity userEntity =
                        userDataService.findUserEntityByEmail(jwtData.getEmail());
                    UserDataValidator.validateUser(userEntity);
                    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                  }
                  return Mono.error(throwable);
                });
      }
      UserEntity userEntity = userDataService.findUserEntityByEmail(jwtData.getEmail());
      UserDataValidator.validateUser(userEntity);
      return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
    }
    return techUserService.userInfo(jwtData);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<OAuthTokenResponse> refreshJWTToken(
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
                              JWTData.generateJWTData(
                                  user1,
                                  clientCredential,
                                  accessTokenData.getSubjectType(),
                                  request);
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
                  UserDataValidator.validateUser(userEntity);
                  User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
                  userEntityToUser.setPassword(null);
                  JWTData jwtData =
                      JWTData.generateJWTData(
                          userEntityToUser,
                          clientCredential,
                          accessTokenData.getSubjectType(),
                          request);
                  AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
                  return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(accessTokenData.getEmail());
    UserDataValidator.validateUser(userEntity);
    User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
    userEntityToUser.setPassword(null);
    JWTData jwtData =
        JWTData.generateJWTData(
            userEntityToUser, clientCredential, accessTokenData.getSubjectType(), request);
    AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
    return Mono.just(new OAuthTokenResponse(userEntityToUser, jwtData, authToken));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<User> updateUser(
      User userToUpdate, boolean isUpdatePassword, boolean callStrapiUserByIdentifier) {
    UserEntity userEntity = UserMapper.mapUserToUserEntity(userToUpdate);

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

    if (isStrapiEnabled) {
      LOG.debug(
          "Strapi is enabled, updating user with email {} on strapi", userToUpdate.getEmail());
      /*
       * The Identifier of the user is already validated so we call strapi to get the strapi id to
       * let update the user into the strapi, then we get the strapi user in order to update the user into the database
       */
      Mono<User> updateUserMono =
          strapiService
              .updateUser(userToUpdate)
              .map(
                  strapiUserUpdated -> {
                    userEntity.setStrapiId(strapiUserUpdated.getId());
                    // if (!ObjectUtils.isEmpty(userToUpdate.getRoles()))
                    //  userEntity.setRoles(String.join(" ", userToUpdate.getRoles()));
                    // Registro l'utente a prescindere che strapi funzioni o meno
                    UserEntity userFind =
                        userDataService.findUserEntityByIdentifier(userToUpdate.getIdentifier());
                    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
                    saveUserEntityIntoDatabase(userEntity);
                    return StrapiMapper.mapFromStrapiUserToUser(strapiUserUpdated);
                  })
              .onErrorResume(
                  throwable -> {
                    if (!OAuthException.isHandleException(throwable)) {
                      LOG.info(
                          "Error on strapi, update user into database, message is {}",
                          throwable.getMessage());
                      UserEntity userFind =
                          userDataService.findUserEntityByIdentifier(userToUpdate.getIdentifier());
                      setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
                      return Mono.just(saveUserEntityIntoDatabase(userEntity));
                    }
                    return Mono.error(throwable);
                  })
              .doOnSuccess(user1 -> LOG.info("User {} updated", user1.getUsername()));

      if (callStrapiUserByIdentifier)
        return strapiService
            .getUserByIdentifier(userToUpdate.getIdentifier())
            .flatMap(
                strapiUser -> {
                  userToUpdate.setId(strapiUser.getId());
                  return updateUserMono;
                });
      return updateUserMono;
    }
    UserEntity userFind = userDataService.findUserEntityByIdentifier(userToUpdate.getIdentifier());
    setDataBeforeUpdate(userEntity, userFind, isUpdatePassword);
    return Mono.just(saveUserEntityIntoDatabase(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  protected Mono<Response> deleteUser(String identifier, String strapi_token) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, deleting user with identifier {} on strapi", identifier);

      return strapiService
          .getUserByIdentifier(identifier)
          .flatMap(
              strapiUser ->
                  strapiService
                      .deleteUsers(strapiUser.getId(), strapi_token)
                      .flatMap(
                          strapiUser1 -> {
                            User userRes = StrapiMapper.mapFromStrapiUserToUser(strapiUser1);
                            Response response =
                                new Response(
                                    HttpStatus.OK.value(),
                                    "User " + userRes.getUsername() + " successfully deleted",
                                    TraceUtils.getSpanID(),
                                    userRes);
                            return Mono.just(response);
                          }))
          .doOnSuccess(responseResponseEntity -> userDataService.deleteByIdentifier(identifier));
    } else {
      userDataService.deleteByIdentifier(identifier);
      Response response =
          new Response(
              HttpStatus.OK.value(),
              "User " + identifier + " successfully deleted",
              TraceUtils.getSpanID(),
              null);
      return Mono.just(response);
    }
  }

  protected void saveUserIntoDatabase(User user) {
    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    saveUserEntityIntoDatabase(userEntity);
  }

  protected void updateUserIntoDatabase(User user) {
    UserEntity userSaved = userDataService.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(userSaved)) {
      // Aggiornare i campi dell'entità esistente con i valori del ClientCredential
      UserMapper.updateUserEntityFields(userSaved, user);
      saveUserEntityIntoDatabase(userSaved);
    } else {
      // Se il client non esiste, possiamo decidere di aggiungerlo o lanciare un'eccezione
      throw new IllegalArgumentException("User not found: " + userSaved.getIdentifier());
    }
    UserMapper.mapUserEntityToUser(userSaved);
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
    User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
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
    return UserMapper.mapUserEntityToUser(userSaved);
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
