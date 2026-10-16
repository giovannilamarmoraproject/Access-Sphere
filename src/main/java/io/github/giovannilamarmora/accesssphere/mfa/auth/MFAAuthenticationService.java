package io.github.giovannilamarmora.accesssphere.mfa.auth;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.MFAMapper;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFADataResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASession;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASetting;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.mfa.model.TOTPLabel;
import io.github.giovannilamarmora.accesssphere.mfa.session.MFASessionService;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.EmailStrategy;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.token.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.ExposedHeaders;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.RequestManager;
import io.github.giovannilamarmora.utils.web.ResponseManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Servizio di coordinamento dell'autenticazione MFA nei flussi OAuth 2.1 e gestione dei metodi utente.
 */
@Service
@Logged
public class MFAAuthenticationService {

  private static final Logger LOG = LoggerFilter.getLogger(MFAAuthenticationService.class);

  @Value("${cookie-domain:}")
  private String cookieDomain;

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private TokenService tokenService;
  @Autowired private MFASessionService mfaSessionService;
  @Autowired private AdminUserService techUserService;
  @Autowired @Lazy private EmailStrategy emailStrategy;

  /**
   * Determina se l'autenticazione MFA è attiva sia a livello utente che a livello di configurazione client OAuth2.
   *
   * @param user Utente da verificare.
   * @param oAuth2Client Client OAuth 2.0 chiamante.
   * @return true se l'MFA è obbligatoria e configurata.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static boolean isMfaEnabled(User user, OAuth2Client oAuth2Client) {
    boolean isEnabled =
        !ObjectToolkit.isNullOrEmpty(user.getMfaSettings())
            && Boolean.TRUE.equals(user.getMfaSettings().getEnabled())
            && !ObjectToolkit.isNullOrEmpty(user.getMfaSettings().getMfaMethods())
            && user.getMfaSettings().getMfaMethods().stream().anyMatch(MFAMethod::isConfirmed)
            && !ObjectToolkit.isNullOrEmpty(oAuth2Client.getMfaEnabled())
            && Boolean.TRUE.equals(oAuth2Client.getMfaEnabled());

    if (!isEnabled) {
      LOG.debug(
          "🔐 No MFA required for user {} and client {} (client MFA config is {})",
          user.getIdentifier(),
          oAuth2Client.getClientId(),
          oAuth2Client.getMfaEnabled());
    }
    return isEnabled;
  }

  /**
   * Verifica se l'autenticazione MFA è abilitata per l'utente e genera il token temporaneo per la verifica.
   * Se il dispositivo chiamante possiede un device_token valido e non scaduto, salta lo step MFA.
   * Se il metodo configurato include EMAIL, invia proattivamente il codice OTP all'indirizzo email dell'utente.
   *
   * @param payload Payload opzionale o claim aggiuntivi.
   * @param jwtData Dati del token JWT.
   * @param user Utente in fase di login.
   * @param oAuth2Client Credenziali del Client chiamante.
   * @param request Richiesta HTTP in ingresso.
   * @return {@link Mono} con {@link OAuthTokenResponse} contenente temp_token per step MFA, oppure Mono.empty().
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> checkMFAAndMakeLogin(
      Map<String, Object> payload,
      JWTData jwtData,
      User user,
      OAuth2Client oAuth2Client,
      ServerHttpRequest request) {

    if (MFAAuthenticationService.isMfaEnabled(user, oAuth2Client)) {
      String deviceToken =
          RequestManager.getCookieOrHeaderData(ExposedHeaders.DEVICE_TOKEN, request);

      if (!ObjectToolkit.isNullOrEmpty(deviceToken)) {
        MFASession mfaSession = mfaSessionService.getByDeviceToken(deviceToken);

        if (!ObjectToolkit.isNullOrEmpty(mfaSession)) {
          Date now = new Date();
          boolean isExpired = now.after(new Date(mfaSession.getExpireDate()));
          boolean isRevokedOrExpired =
              mfaSession.getStatus() == TokenStatus.EXPIRED
                  || mfaSession.getStatus() == TokenStatus.REVOKED;

          if (!isExpired && !isRevokedOrExpired) {
            LOG.info("✅ Valid trusted MFA device token found, skipping MFA login step for user {}", user.getIdentifier());
            return Mono.empty();
          } else {
            LOG.warn(
                "⚠️ MFA device token is expired or revoked (exp: {}, status: {})",
                mfaSession.getExpireDate(),
                mfaSession.getStatus());
          }
        }
      }

      LOG.info("🔐 Generating new MFA temporary token for user {}", user.getIdentifier());
      if (payload != null) {
        payload.put("expires_at", jwtData.getExp());
      }
      AuthToken mfaToken = tokenService.generateMFAToken(user, oAuth2Client, payload);

      // Se l'utente dispone del metodo EMAIL confermato, invia proattivamente il codice OTP
      boolean hasEmailMfa =
          user.getMfaSettings() != null
              && user.getMfaSettings().getMfaMethods() != null
              && user.getMfaSettings().getMfaMethods().stream()
                  .anyMatch(m -> m.getType() == MFAType.EMAIL && m.isConfirmed());

      if (hasEmailMfa) {
        LOG.info("📧 Proactively dispatching Email MFA OTP code to user [{}]", user.getEmail());
        return emailStrategy
            .sendCode(user)
            .map(sent -> new OAuthTokenResponse(mfaToken, jwtData, user));
      }

      return Mono.just(new OAuthTokenResponse(mfaToken, jwtData, user));
    }

    return Mono.empty();
  }

  /**
   * Finalizza la procedura MFA durante il login, rilasciando l'access token JWT/JWE definitivo e configurando i cookie e redirect.
   *
   * @param mfaRequest Dati di verifica codice e preferenze ('rememberDevice').
   * @param mfaSession Sessione temporanea associata al temp_token.
   * @param user Utente autenticato.
   * @param exchange Scambio HTTP server reattivo.
   * @param userService Servizio utente.
   * @return {@link Mono} con {@link ResponseEntity} di avvenuto login.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> verifyMfaAndGenerateToken(
      MfaVerificationRequest mfaRequest,
      MFASession mfaSession,
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

    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(mfaSession.getClientId());

    return clientCredentialMono.flatMap(
        clientCredential -> {
          SubjectType subjectType =
              techUserService.isTechnicalUser(user) ? SubjectType.TECHNICAL : SubjectType.CUSTOMER;
          JWTData jwtData = JWTData.generateJWTData(user, clientCredential, subjectType, request);
          return userService
              .getUserByEmail(jwtData.getEmail())
              .map(
                  user1 -> {
                    String deviceToken = null;
                    if (Boolean.TRUE.equals(mfaRequest.rememberDevice())) {
                      deviceToken = UUID.randomUUID().toString();
                      ResponseManager.setCookieAndHeaderData(
                          ExposedHeaders.DEVICE_TOKEN, deviceToken, cookieDomain, response);
                    }
                    mfaSessionService.completeLogin(
                        mfaSession, deviceToken, mfaRequest.mfaMethod().name());
                    jwtData.setRoles(
                        user1.getRoles() != null
                            ? user1.getRoles().stream()
                                .map(UserClientRole::getRole)
                                .filter(Objects::nonNull)
                                .toList()
                            : null);
                    AuthToken token =
                        tokenService.generateToken(
                            jwtData, clientCredential, mfaSession.getPayload());
                    OAuthTokenResponse oAuthTokenResponse =
                        new OAuthTokenResponse(token, jwtData, user1);

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

  /**
   * Abilita globalmente lo stato MFA per l'utente a patto che sia presente almeno un metodo confermato.
   *
   * @param identifier Identificativo dell'utente.
   * @param userService Servizio utente.
   * @return {@link Mono} con {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> enableMethods(String identifier, UserService userService) {
    LOG.info("🔐 [MFA ENABLE] - Process started for user '{}'", identifier);
    Mono<User> userMono = userService.getUserByIdentifier(identifier);

    return userMono
        .flatMap(
            user -> {
              MFASetting mfaSetting = user.getMfaSettings();
              if (ObjectToolkit.isNullOrEmpty(mfaSetting)
                  || mfaSetting.getMfaMethods().stream().noneMatch(MFAMethod::isConfirmed)) {
                LOG.error("❌ MFA is not yet defined or not confirmed for user {}", identifier);
                throw new MFAException(
                    ExceptionMap.ERR_MFA_400,
                    ExceptionType.OTP_NOT_CONFIGURED,
                    "MFA methods are not defined or confirmed to enable");
              }

              if (Boolean.TRUE.equals(mfaSetting.getEnabled())) {
                LOG.info("ℹ️ MFA already enabled for user {}, no update needed", identifier);
                return Mono.just(MFAMapper.toDataResponse(user, "MFA Enabled"));
              }
              mfaSetting.setEnabled(true);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(identifier, user)
                  .map(updated -> MFAMapper.toDataResponse(updated, "MFA Enabled"));
            })
        .doOnSuccess(re -> LOG.info("✅ [MFA ENABLE] - Process ended for user '{}'", identifier));
  }

  /**
   * Disabilita globalmente l'MFA per l'utente, mantenendo i metodi registrati.
   *
   * @param identifier Identificativo dell'utente.
   * @param userService Servizio utente.
   * @return {@link Mono} con {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> disableMethods(String identifier, UserService userService) {
    LOG.info("🔐 [MFA DISABLE] - Process started for user '{}'", identifier);
    Mono<User> userMono = userService.getUserByIdentifier(identifier);

    return userMono
        .flatMap(
            user -> {
              MFASetting mfaSetting = user.getMfaSettings();
              if (ObjectToolkit.isNullOrEmpty(mfaSetting)) {
                LOG.error("❌ MFA settings are not defined for user {}", identifier);
                throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA methods are not defined");
              }

              if (!Boolean.TRUE.equals(mfaSetting.getEnabled())) {
                LOG.info("ℹ️ MFA already disabled for user {}, no update needed", identifier);
                return Mono.just(MFAMapper.toDataResponse(user, "MFA Disabled"));
              }
              mfaSetting.setEnabled(false);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(identifier, user)
                  .map(updated -> MFAMapper.toDataResponse(updated, "MFA Disabled"));
            })
        .doOnSuccess(re -> LOG.info("✅ [MFA DISABLE] - Process ended for user '{}'", identifier));
  }

  /**
   * Elimina un metodo MFA specifico identificato da label o rimuove l'ultimo metodo disabilitando l'MFA.
   *
   * @param identifier Identificativo dell'utente.
   * @param label Label del metodo da eliminare.
   * @param userService Servizio utente.
   * @return {@link Mono} con {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> deleteMethods(
      String identifier, TOTPLabel label, UserService userService) {
    LOG.info("🔐 [MFA DELETE] - Process started for user '{}', label '{}'", identifier, label);

    Mono<User> userMono = userService.getUserByIdentifier(identifier);

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

              boolean removed = methods.removeIf(method -> Objects.equals(method.getLabel(), label));

              if (!removed) {
                LOG.warn("⚠️ No MFA method found with label '{}' for user: {}", label, identifier);
                throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA Method with label not found");
              }

              if (methods.isEmpty()) {
                mfaSetting.setEnabled(false);
              }
              mfaSetting.setMfaMethods(methods);
              user.setMfaSettings(mfaSetting);

              return userService
                  .updateUser(identifier, user)
                  .map(
                      updatedUser -> {
                        String msg =
                            methods.isEmpty()
                                ? "All MFA methods removed. MFA disabled."
                                : "MFA method removed.";
                        LOG.info("✅ {}", msg);
                        return MFAMapper.toDataResponse(updatedUser, msg);
                      });
            })
        .doOnSuccess(
            re ->
                LOG.info(
                    "✅ [MFA DELETE] - Process ended for user '{}', label '{}'", identifier, label));
  }
}
