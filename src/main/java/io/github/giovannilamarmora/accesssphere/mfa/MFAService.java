package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.auth.MFAAuthenticationService;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFADataResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupDataResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASession;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.mfa.session.MFASessionService;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategy;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.UserValidator;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Application Service per l'orchestrazione delle operazioni di Multi-Factor Authentication.
 *
 * <p>Responsabilità:
 * <ul>
 *   <li>Validazione rigorosa dei parametri di richiesta tramite {@link MFAValidator}.</li>
 *   <li>Verifica di sicurezza sull'identità dell'utente operatore tramite {@link UserValidator} e {@link PermissionRoleManager}.</li>
 *   <li>Delega la generazione e verifica delle credenziali alla strategia appropriata (TOTP o EMAIL) tramite {@link MFAStrategyFactory}.</li>
 *   <li>Restituisce DTO di risposta puri, delegando la creazione di {@link ResponseEntity} al Controller layer.</li>
 * </ul>
 */
@Service
@Logged
public class MFAService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private UserService userService;
  @Autowired private MFAStrategyFactory strategyFactory;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private MFASessionService mfaSessionService;
  @Autowired private MFAAuthenticationService mfaAuthenticationService;
  @Autowired private PermissionRoleManager permissionRoleManager;

  /**
   * Inizializza il setup MFA per l'utente generando un secret TOTP (con QR code) o inviando un codice OTP via Email.
   *
   * @param setupRequest DTO con identificativo utente, tipo MFA ed opzioni.
   * @return {@link Mono} contenente {@link MFASetupDataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFASetupDataResponse> generateSecretForUser(MFASetupRequest setupRequest) {
    LOG.info("🔐 [MFA SETUP] Process started for user: {}", setupRequest.identifier());
    MFAValidator.validateSetupRequest(setupRequest);

    if (!permissionRoleManager.hasAdminPermission(SystemPermission.USERS_WRITE)) {
      UserValidator.validateIdentifier(
          setupRequest.identifier(), oAuth2Authorization.getIdentifier());
    }

    return userService
        .getUserByIdentifier(setupRequest.identifier())
        .flatMap(
            user -> {
              MFAStrategy strategy = strategyFactory.getStrategy(setupRequest.type());
              return strategy.generateSecret(user, setupRequest);
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "✅ [MFA SETUP] Process ended successfully for user: {}",
                    setupRequest.identifier()));
  }

  /**
   * Conferma il codice OTP immesso dall'utente attivando definitivamente il metodo configurato.
   *
   * @param confirmationRequest DTO con identificativo, tipo, label e codice OTP.
   * @return {@link Mono} contenente {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> confirmMFA(MFAConfirmationRequest confirmationRequest) {
    LOG.info(
        "🔑 [MFA CONFIRM] OTP confirmation process started for user: {}",
        confirmationRequest.identifier());
    MFAValidator.validateConfirmationRequest(confirmationRequest);

    if (!permissionRoleManager.hasAdminPermission(SystemPermission.USERS_WRITE)) {
      UserValidator.validateIdentifier(
          confirmationRequest.identifier(), oAuth2Authorization.getIdentifier());
    }

    return userService
        .getUserByIdentifier(confirmationRequest.identifier())
        .flatMap(
            user -> {
              MFAMethod mfaMethod =
                  MFAMapper.getMFAMethod(user.getMfaSettings(), confirmationRequest);

              MFAStrategy strategy = strategyFactory.getStrategy(mfaMethod.getType());
              strategy.verifyCode(
                  List.of(mfaMethod), confirmationRequest.otp(), user.getIdentifier());

              user.setMfaSettings(MFAMapper.generateFinalMFA(user, mfaMethod));

              return userService
                  .updateUser(user.getIdentifier(), user)
                  .map(
                      userUpdated ->
                          MFAMapper.toDataResponse(
                              userUpdated,
                              "MFA Confirmed for user: " + userUpdated.getIdentifier()));
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "✅ [MFA CONFIRM] OTP confirmation ended successfully for user: {}",
                    confirmationRequest.identifier()));
  }

  /**
   * Convalida il codice OTP fornito durante la fase di autenticazione login OAuth 2.1 con tempToken Bearer.
   *
   * @param mfaRequest DTO con metodo MFA, codice OTP e opzioni 'rememberDevice'.
   * @param bearer Token temporaneo Bearer rilasciato nella prima fase di login.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con risposta di login completa e rilascio dei token finali.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> verifyMfa(
      MfaVerificationRequest mfaRequest, String bearer, ServerWebExchange exchange) {
    LOG.info("🔑 [MFA VERIFY] Verification for method: {} process started", mfaRequest.mfaMethod());
    MFAValidator.validateVerificationRequest(mfaRequest);

    MFASession mfaSession = mfaSessionService.getByTempToken(bearer);
    MFAValidator.validateMFAMethods(mfaRequest.mfaMethod(), mfaSession.getMfaMethods());

    MFAStrategy strategy = strategyFactory.getStrategy(mfaRequest.mfaMethod());

    return userService
        .getUserByIdentifier(mfaSession.getIdentifier())
        .flatMap(
            user -> {
              strategy.verifyCode(
                  user.getMfaSettings().getMfaMethods(), mfaRequest.otp(), user.getIdentifier());

              return mfaAuthenticationService.verifyMfaAndGenerateToken(
                  mfaRequest, mfaSession, user, exchange, userService);
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [MFA VERIFY] Verification for method: {} completed successfully",
                    mfaRequest.mfaMethod()));
  }

  /**
   * Invia o reinvia un codice OTP via email sia durante il flusso di login (tramite temp_token)
   * sia su richiesta di un utente autenticato.
   *
   * @param bearer Token di autenticazione (tempToken di login o Bearer token utente).
   * @param identifier Identificativo opzionale dell'utente.
   * @return {@link Mono} con {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> sendOtp(String bearer, String identifier) {
    LOG.info("📧 [MFA SEND-OTP] Request received to dispatch OTP code");

    String targetIdentifier = resolveUserIdentifier(bearer, identifier);
    if (ObjectToolkit.isNullOrEmpty(targetIdentifier)) {
      LOG.error("❌ Unable to resolve target user identifier for OTP dispatch");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, "Unable to resolve user identifier for OTP dispatch");
    }

    return userService
        .getUserByIdentifier(targetIdentifier)
        .flatMap(
            user -> {
              MFAStrategy strategy = strategyFactory.getStrategy(MFAType.EMAIL);
              return strategy
                  .sendCode(user)
                  .map(
                      sent -> {
                        String msg =
                            Boolean.TRUE.equals(sent)
                                ? "MFA Email OTP sent successfully to " + user.getEmail()
                                : "Unable to send OTP email. Please ensure Email MFA is configured.";
                        return MFAMapper.toDataResponse(user, msg);
                      });
            })
        .doOnSuccess(
            res ->
                LOG.info("✅ [MFA SEND-OTP] OTP code dispatched for user {}", targetIdentifier));
  }

  /**
   * Risolve l'identificativo utente prioritizzando la sessione MFA da tempToken, poi il token Bearer JWT e infine il parametro esplicito.
   */
  private String resolveUserIdentifier(String bearer, String identifier) {
    if (!ObjectToolkit.isNullOrEmpty(bearer)) {
      try {
        MFASession session = mfaSessionService.getByTempToken(bearer);
        if (session != null && !ObjectToolkit.isNullOrEmpty(session.getIdentifier())) {
          return session.getIdentifier();
        }
      } catch (Exception e) {
        LOG.debug(
            "Bearer is not a valid temp token, checking authenticated context: {}",
            e.getMessage());
      }
      if (!ObjectToolkit.isNullOrEmpty(oAuth2Authorization.getIdentifier())) {
        return oAuth2Authorization.getIdentifier();
      }
    }

    if (!ObjectToolkit.isNullOrEmpty(identifier)) {
      if (!permissionRoleManager.hasAdminPermission(SystemPermission.USERS_WRITE)) {
        UserValidator.validateIdentifier(identifier, oAuth2Authorization.getIdentifier());
      }
      return identifier;
    }

    return null;
  }

  /**
   * Esegue le azioni di abilitazione, disabilitazione o rimozione dei metodi MFA.
   *
   * @param request Payload contenente identificativo, azione ed eventuale label.
   * @return {@link Mono} con {@link MFADataResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFADataResponse> manageMFA(MFAManageRequest request) {
    MFAValidator.validateManageRequest(request);

    if (!permissionRoleManager.hasAdminPermission(SystemPermission.USERS_WRITE)) {
      UserValidator.validateIdentifier(
          request.identifier(), oAuth2Authorization.getIdentifier());
    }

    return switch (request.action()) {
      case ENABLE -> mfaAuthenticationService.enableMethods(request.identifier(), userService);
      case DISABLE -> mfaAuthenticationService.disableMethods(request.identifier(), userService);
      case DELETE -> mfaAuthenticationService.deleteMethods(
          request.identifier(), request.label(), userService);
    };
  }
}
