package io.github.giovannilamarmora.accesssphere.mfa.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.dto.*;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.MFATokenDataService;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFATokenData;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.ExposedHeaders;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.RequestManager;
import io.github.giovannilamarmora.utils.web.ResponseManager;
import java.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@Logged
public class MFAAuthenticationService {

  @Value("${cookie-domain:}")
  private String cookieDomain;

  private static final Logger LOG = LoggerFilter.getLogger(MFAStrategyFactory.class);
  @Autowired private ClientService clientService;
  @Autowired private TokenService tokenService;
  @Autowired private MFATokenDataService mfaTokenDataService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static boolean isMfaEnabled(User user, ClientCredential clientCredential) {
    boolean isEnabled =
        !ObjectToolkit.isNullOrEmpty(user.getMfaSettings())
            && user.getMfaSettings().getEnabled()
            && !ObjectToolkit.isNullOrEmpty(user.getMfaSettings().getMfaMethods())
            && !ObjectToolkit.isNullOrEmpty(clientCredential.getMfaEnabled())
            && clientCredential.getMfaEnabled();
    if (!isEnabled)
      LOG.info(
          "🔐 No MFA found for user {} and client {} (Config is {})",
          user.getIdentifier(),
          clientCredential.getClientId(),
          clientCredential.getMfaEnabled());
    return isEnabled;
  }

  /**
   * Verifica se l'autenticazione MFA e' abilitata per l'utente e genera il token temporaneo per la verifica.
   *
   * @param payload Payload opzionale o claim aggiuntivi.
   * @param jwtData Dati del token JWT.
   * @param user Utente in fase di login.
   * @param clientCredential Credenziali del Client chiamante.
   * @param request Richiesta HTTP in ingresso.
   * @return Mono con la risposta OAuthTokenResponse per step MFA, oppure Mono.empty() se MFA non richiesto.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> checkMFAAndMakeLogin(
      Map<String, Object> payload,
      JWTData jwtData,
      User user,
      ClientCredential clientCredential,
      ServerHttpRequest request) {

    if (MFAAuthenticationService.isMfaEnabled(user, clientCredential)) {
      String deviceToken =
          RequestManager.getCookieOrHeaderData(ExposedHeaders.DEVICE_TOKEN, request);

      if (!ObjectToolkit.isNullOrEmpty(deviceToken)) {
        MFATokenData mfaTokenData = mfaTokenDataService.getByDeviceToken(deviceToken);

        if (!ObjectToolkit.isNullOrEmpty(mfaTokenData)) {
          Date now = new Date();
          boolean isExpired = now.after(new Date(mfaTokenData.getExpireDate()));
          boolean isRevokedOrExpired =
              mfaTokenData.getStatus() == TokenStatus.EXPIRED
                  || mfaTokenData.getStatus() == TokenStatus.REVOKED;

          if (!isExpired && !isRevokedOrExpired) {
            LOG.info("Valid MFA device token found, skipping MFA login step");
            return Mono.empty();
          } else {
            LOG.warn(
                "MFA token is expired or revoked (exp: {}, status: {})",
                mfaTokenData.getExpireDate(),
                mfaTokenData.getStatus());
          }
        } else {
          LOG.warn("MFA token not found for device token");
        }
      } else {
        LOG.warn("No device token found in request");
      }

      LOG.info("Generating new MFA token");
      if (payload != null) {
        payload.put("expires_at", jwtData.getExp());
      }
      AuthToken mfaToken = tokenService.generateMFAToken(user, clientCredential, payload);
      return Mono.just(
          new OAuthTokenResponse(
              mfaToken, payload != null ? Mapper.convertObject(payload, JsonNode.class) : null, jwtData, user));
    }

    return Mono.empty();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> verifyMfaAndGenerateToken(
      MfaVerificationRequest mfaRequest,
      MFATokenData mfaTokenData,
      User user,
      ServerWebExchange exchange,
      UserService userService) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(mfaTokenData.getClientId());
    return clientCredentialMono.flatMap(
        clientCredential -> {
          JWTData jwtData =
              JWTData.generateJWTData(user, clientCredential, SubjectType.CUSTOMER, request);
          return userService
              .getUserInfo(jwtData, null)
              .map(
                  user1 -> {
                    String deviceToken = null;
                    if (!ObjectToolkit.isNullOrEmpty(mfaRequest.rememberDevice())
                        && mfaRequest.rememberDevice()) {
                      deviceToken = UUID.randomUUID().toString();
                      ResponseManager.setCookieAndHeaderData(
                          ExposedHeaders.DEVICE_TOKEN, deviceToken, cookieDomain, response);
                    }
                    mfaTokenDataService.completeLogin(
                        mfaTokenData, deviceToken, mfaRequest.mfaMethod().name());
                    jwtData.setRoles(user1.getRoles());
                    AuthToken token =
                        tokenService.generateToken(jwtData, clientCredential, mfaTokenData.getPayload());
                    OAuthTokenResponse oAuthTokenResponse =
                        new OAuthTokenResponse(
                            token,
                            mfaTokenData.getPayload() != null ? Mapper.convertObject(mfaTokenData.getPayload(), JsonNode.class) : null,
                            jwtData,
                            user1);

                    return AuthService.setLoginResponse(
                        oAuthTokenResponse,
                        clientCredential,
                        includeUserInfo,
                        includeUserData,
                        mfaRequest.redirectUri(),
                        cookieDomain,
                        response);
                  });
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> enableMethods(
      String identifier, UserService userService) {
    LOG.info("🔐 [MFA ENABLE] - Process started for user '{}'", identifier);
    Mono<User> userMono = userService.getUserByIdentifier(identifier, true);

    return userMono
        .flatMap(
            user -> {
              MFASetting mfaSetting = user.getMfaSettings();
              if (ObjectToolkit.isNullOrEmpty(mfaSetting)
                  || mfaSetting.getMfaMethods().stream().noneMatch(MFAMethod::isConfirmed)) {
                LOG.error("MFA is not yet defined or not confirmed");
                throw new MFAException(
                    ExceptionMap.ERR_MFA_400,
                    ExceptionType.OTP_NOT_CONFIGURED,
                    "MFA Method are not defined to enable");
              }
              Response response =
                  new Response(HttpStatus.OK.value(), "MFA Enabled", TraceUtils.getSpanID(), null);

              if (mfaSetting.getEnabled()) {
                LOG.info("MFA Already enabled, no need to update the settings");
                return Mono.just(ResponseEntity.ok(response));
              }
              mfaSetting.setEnabled(true);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(user, false)
                  .map(user1 -> ResponseEntity.ok(response));
            })
        .doOnSuccess(re -> LOG.info("✅ [MFA ENABLE] - Process ended for user '{}'", identifier));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> disableMethods(
      String identifier, UserService userService) {
    LOG.info("🔐 [MFA DISABLE] - Process started for user '{}'", identifier);
    Mono<User> userMono = userService.getUserByIdentifier(identifier, true);

    return userMono
        .flatMap(
            user -> {
              MFASetting mfaSetting = user.getMfaSettings();
              if (ObjectToolkit.isNullOrEmpty(mfaSetting)) {
                LOG.error("MFA is not yet defined to disable");
                throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA Method are not defined");
              }
              Response response =
                  new Response(HttpStatus.OK.value(), "MFA Disabled", TraceUtils.getSpanID(), null);

              if (!mfaSetting.getEnabled()) {
                LOG.info("MFA Already disable, no need to update the settings");
                return Mono.just(ResponseEntity.ok(response));
              }
              mfaSetting.setEnabled(false);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(user, false)
                  .map(user1 -> ResponseEntity.ok(response));
            })
        .doOnSuccess(re -> LOG.info("✅ [MFA DISABLE] - Process ended for user '{}'", identifier));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> deleteMethods(
      String identifier, TOTPLabel label, UserService userService) {
    LOG.info("🔐 [MFA DELETE] - Process started for user '{}', label '{}'", identifier, label);

    Mono<User> userMono = userService.getUserByIdentifier(identifier, true);

    return userMono
        .flatMap(
            user -> {
              MFASetting mfaSetting = user.getMfaSettings();

              if (ObjectToolkit.isNullOrEmpty(mfaSetting)
                  || ObjectToolkit.isNullOrEmpty(mfaSetting.getMfaMethods())) {
                LOG.error("⚠️ MFA methods are not defined for user: {}", identifier);
                throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA Methods are not defined");
              }

              List<MFAMethod> methods = new ArrayList<>(mfaSetting.getMfaMethods());

              boolean removed = methods.removeIf(method -> method.getLabel().equals(label));

              if (!removed) {
                LOG.warn("⚠️ No MFA method found with label '{}' for user: {}", label, identifier);
                throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA Method with label not found");
              }

              // Se non ci sono più metodi, disattiva completamente l'MFA
              if (methods.isEmpty()) {
                mfaSetting.setEnabled(false);
              }
              mfaSetting.setMfaMethods(methods);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(user, false)
                  .map(
                      updatedUser -> {
                        String msg =
                            methods.isEmpty()
                                ? "All MFA methods removed. MFA disabled."
                                : "MFA method removed.";
                        LOG.info("✅ {}", msg);
                        Response response =
                            new Response(HttpStatus.OK.value(), msg, TraceUtils.getSpanID(), null);
                        return ResponseEntity.ok(response);
                      });
            })
        .doOnSuccess(
            re ->
                LOG.info(
                    "✅ [MFA DELETE] - Process ended for user '{}', label '{}'", identifier, label));
  }
}
