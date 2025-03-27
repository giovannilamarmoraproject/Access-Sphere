package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.TemplateParam;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiEmailTemplate;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiLocale;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private DataService dataService;
  @Autowired private ClientService clientService;
  @Autowired private TokenService tokenService;
  @Autowired private StrapiService strapiService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private TechUserService techUserService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F UserInfo process started, include Data: {}", includeUserData);
    // AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateAuthType(clientCredential, accessTokenData);
              JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
              switch (decryptToken.getType()) {
                case BEARER -> {
                  return dataService
                      .getUserInfo(
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
                    return dataService
                        .getUserByEmail(decryptToken.getEmail())
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
                    "\uD83E\uDD37\u200D♂\uFE0F UserInfo process ended, include Data: {}",
                    includeUserData));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> profile(String bearer, ServerHttpRequest request) {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting profile process started");
    // AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateAuthType(clientCredential, accessTokenData);
              JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
              switch (decryptToken.getType()) {
                case BEARER -> {
                  return dataService
                      .getUserInfo(
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
                  return dataService
                      .getUserByEmail(decryptToken.getEmail())
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
                LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting profile process ended"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> register(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Registration process started, username: {}, email: {}",
        user.getUsername(),
        user.getEmail());
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              UserValidator.validateRegistration(registration_token, clientCredential, user);
              return dataService
                  .registerUser(bearer, user, clientCredential, assignNewClient)
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
                    "\uD83E\uDD37\u200D♂\uFE0F Registration process successfully ended, username: {}, email: {}",
                    user.getUsername(),
                    user.getEmail()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> updateUser(
      User userToUpdate, String bearer, ServerHttpRequest request) {
    userToUpdate.setPassword(null);
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Updating user process started, username: {}, email: {}",
        userToUpdate.getUsername(),
        userToUpdate.getEmail());

    // AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);
    // Setting UserData
    return techUserService
        .hasTechUserRoles(accessTokenData, clientService)
        .flatMap(
            aBoolean -> {
              if (!aBoolean) UserValidator.validateUpdate(accessTokenData, userToUpdate);
              return dataService
                  .updateUser(userToUpdate)
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
                              "\uD83E\uDD37\u200D♂\uFE0F Updating user process ended, username: {}, email: {}",
                              userToUpdate.getUsername(),
                              userToUpdate.getEmail()));
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> unlockUser(String identifier, Boolean block) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Unlocking user process started, identifier: {}", identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("Only a tech user can unlock the user {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    return strapiService
        .getUserByIdentifier(identifier)
        .flatMap(
            strapiUser -> {
              // User user = StrapiMapper.mapFromStrapiUserToUser(strapiUser);
              // user.setId(strapiUser.getId());
              // user.setBlocked(block);
              strapiUser.setBlocked(block);
              return strapiService
                  .updateUser(strapiUser)
                  .flatMap(
                      strapiUser1 -> {
                        User userRes = StrapiMapper.mapFromStrapiUserToUser(strapiUser1);
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "User "
                                    + userRes.getUsername()
                                    + " successfully "
                                    + (block ? "locked!" : "unlocked!"),
                                TraceUtils.getSpanID(),
                                userRes);
                        return Mono.just(ResponseEntity.ok(response));
                      })
                  .doOnSuccess(
                      responseResponseEntity ->
                          LOG.info(
                              "\uD83E\uDD37\u200D♂\uFE0F Unlocking user process ended, identifier: {}",
                              identifier));
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Change password request user process started, locale: {}, sendEmail: {}",
        locale,
        sendEmail);
    if (!Utilities.isCharacterAndRegexValid(changePassword.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("Invalid regex for field email {}", changePassword.getEmail());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid field email, try again!");
    }

    Mono<List<StrapiLocale>> strapiLocaleMono = strapiService.locales();

    Mono<User> userMono =
        dataService
            .getUserByEmail(changePassword.getEmail())
            .map(
                user -> {
                  String token = UUID.randomUUID().toString();
                  user.setTokenReset(token);
                  return user;
                });

    Mono<String> finalLocaleMono =
        strapiLocaleMono.map(
            strapiLocales ->
                strapiLocales.stream()
                    .filter(loc -> loc.getCode().equals(locale))
                    .findFirst()
                    .map(
                        foundLocale -> {
                          LOG.info("Locale found: {}", foundLocale.getCode());
                          return foundLocale.getCode();
                        })
                    .orElseGet(
                        () ->
                            strapiLocales.stream()
                                .filter(loc -> Boolean.TRUE.equals(loc.getIsDefault()))
                                .findFirst()
                                .map(
                                    defaultLocale -> {
                                      LOG.warn(
                                          "Locale '{}' not found. Using default locale: {}",
                                          locale,
                                          defaultLocale.getCode());
                                      return defaultLocale.getCode();
                                    })
                                .orElseGet(
                                    () -> {
                                      LOG.error(
                                          "Locale '{}' not found and no default locale available. Using fallback: en-GB",
                                          locale);
                                      return "en-GB";
                                    })));

    Mono<StrapiEmailTemplate> strapiEmailTemplateMono =
        finalLocaleMono.flatMap(
            finalLocale ->
                strapiService.getTemplateById(changePassword.getTemplateId(), finalLocale));

    return Mono.zip(userMono, strapiEmailTemplateMono)
        .flatMap(
            objects -> {
              Mono<User> updatedUserMono = dataService.updateUser(objects.getT1());
              // Send Email
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
                          // objects1.getT1().setToken(objects1.getT2().getTokenReset());
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
                    "\uD83E\uDD37\u200D♂\uFE0F Change password request user process ended, locale: {}, sendEmail: {}",
                    locale,
                    sendEmail));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changePassword(ChangePassword changePassword) {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Change password user process started");
    changePassword.setPassword(
        new String(Base64.getDecoder().decode(changePassword.getPassword())));
    if (!Utilities.isCharacterAndRegexValid(
        changePassword.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("Invalid regex for field password");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid regex for field password");
    }

    String finalPassword = changePassword.getPassword();
    return dataService
        .getUserByTokenReset(changePassword.getToken())
        .flatMap(
            user -> {
              user.setPassword(finalPassword);
              user.setTokenReset(null);
              return dataService
                  .updateUser(user, true)
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
                LOG.info("\uD83E\uDD37\u200D♂\uFE0F Change password user process ended"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getUsers() {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Get list of user process started");

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(techUserService.getTech_client_id());

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              OAuthValidator.validateUserRoles(clientCredential, accessTokenData.getRoles());
              String strapi_token = OAuthMapper.getStrapiAccessToken(accessTokenData);
              if (ObjectToolkit.isNullOrEmpty(strapi_token)) {
                LOG.error("Strapi token not found for user {}", accessTokenData.getIdentifier());
                throw new OAuthException(ExceptionMap.ERR_OAUTH_403);
              }
              return dataService
                  .getStrapiUsers(strapi_token)
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
                LOG.info("\uD83E\uDD37\u200D♂\uFE0F Get list of user process ended"));
  }
}
