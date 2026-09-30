package io.github.giovannilamarmora.accesssphere.user.account;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.TemplateParam;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.email.EmailTemplateService;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.user.UserException;
import io.github.giovannilamarmora.accesssphere.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserUpdateRequest;
import io.github.giovannilamarmora.accesssphere.user.model.ChangePassword;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Servizio applicativo reattivo dedicato alle operazioni self-service dell'utente e al ciclo di
 * vita dell'account (profilo e gestione password).
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Risolve la configurazione del client e le tipologie di autenticazione tramite {@link
 *       OAuth2ClientService}.
 *   <li>Decodifica i token Bearer con {@link TokenService#parseToken} e valida le identita' Google
 *       federate tramite {@link GrpcService#userInfo}.
 *   <li>Interroga e aggiorna l'anagrafica utente delegando al layer di persistenza {@link
 *       UserService}.
 *   <li>Verifica i diritti di auto-modifica del profilo o la presenza di ruoli privilegiati tramite
 *       {@link AdminUserService} e {@link UserValidator#validateUpdate}.
 *   <li>Orchestra il flusso di reset password in due fasi, integrandosi con {@link
 *       EmailTemplateService} per il template localizzato e con {@link EmailSenderService} per la
 *       notifica.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class UserAccountService {

  private static final Logger LOG = LoggerFilter.getLogger(UserAccountService.class);
  private static final String USER_ACCOUNT_LOG = "User Account";

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private TokenService tokenService;
  @Autowired private AdminUserService techUserService;
  @Autowired private EmailTemplateService emailTemplateService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private UserService userService;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private PermissionRoleManager permissionRoleManager;

  /**
   * Recupera il profilo dell'utente attualmente autenticato nella sessione corrente.
   *
   * @param bearer Token di accesso Bearer inviato dal client.
   * @param request Richiesta HTTP server.
   * @return Mono contenente il DTO {@link User} del profilo.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserDataResponse> profile(String bearer, ServerHttpRequest request) {
    LOG.info("👤 {} Starting user profile retrieval process", USER_ACCOUNT_LOG);

    String email = oAuth2Authorization.getEmail();

    return userService
        .getUserByEmail(email)
        .map(UserMapper::toResponse)
        .doOnSuccess(
            userDataResponse ->
                LOG.info(
                    "✅ {} Profile retrieval completed successfully for user: {}",
                    USER_ACCOUNT_LOG,
                    userDataResponse.getUsername()));
  }

  /**
   * Aggiorna i dati di profilo dell'utente nel database locale, verificando l'identita' o i
   * privilegi tecnici.
   *
   * @param userUpdateRequest DTO contenente i dati anagrafici da aggiornare.
   * @param bearer Token Bearer della sessione.
   * @param request Richiesta HTTP server.
   * @return Mono contenente il DTO {@link User} aggiornato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserDataResponse> updateUser(
      String identifier,
      UserUpdateRequest userUpdateRequest,
      String bearer,
      ServerHttpRequest request) {
    if (ObjectToolkit.isNullOrEmpty(identifier)) {
      LOG.error("❌ {} Target user identifier is missing", USER_ACCOUNT_LOG);
      throw new UserException(ExceptionMap.ERR_USER_400, "Identifier is required!");
    }

    User userToUpdate = UserMapper.toModel(userUpdateRequest);
    userToUpdate.setIdentifier(identifier);

    LOG.info("👤 {} Starting profile update for identifier: {}", USER_ACCOUNT_LOG, identifier);

    return userService
        .getUserByIdentifier(identifier)
        .flatMap(
            targetUser -> {
              boolean isAdmin =
                  permissionRoleManager.hasAdminPermission(SystemPermission.USERS_WRITE);

              if (isAdmin) {
                // Utente Admin: puo aggiornare qualsiasi utente senza vincoli di perimetro client.
                // Se non ha specificato ruoli, preserva quelli esistenti dell'utente target.
                if (ObjectToolkit.isNullOrEmpty(userToUpdate.getRoles())) {
                  userToUpdate.setRoles(targetUser.getRoles());
                }
              } else {
                // Utente Non-Admin: non puo mai modificare l'utenza Admin di sistema
                if (permissionRoleManager.isAdminUser(targetUser.getUsername())) {
                  LOG.error(
                      "⛔ {} Forbidden: User cannot alter the admin user {}",
                      USER_ACCOUNT_LOG,
                      identifier);
                  return Mono.error(
                      new OAuthException(ExceptionMap.ERR_OAUTH_403, "Cannot alter admin user"));
                }

                boolean isSelf = permissionRoleManager.isCurrentUser(targetUser.getIdentifier());

                if (isSelf) {
                  // Utente che modifica il proprio profilo (self-service):
                  // puo modificare i propri dati anagrafici, ma non puo alterare i propri ruoli.
                  userToUpdate.setRoles(targetUser.getRoles());
                } else {
                  // Utente 1 che vuole modificare Utente 2:
                  // puo farlo se e solo se ha il permesso USERS_WRITE e se entrambi appartengono
                  // allo stesso client.
                  List<RoleDefinition> authorizedRoles =
                      permissionRoleManager.checkPermissionAndReturnUserRoles(
                          SystemPermission.USERS_WRITE);

                  boolean sameClient = hasMatchingClient(targetUser, authorizedRoles);
                  if (!sameClient) {
                    LOG.error(
                        "⛔ {} Access denied: Operator {} and target user {} do not belong to the same client with USERS_WRITE permission",
                        USER_ACCOUNT_LOG,
                        oAuth2Authorization.getIdentifier(),
                        identifier);
                    return Mono.error(
                        new OAuthException(
                            ExceptionMap.ERR_OAUTH_403,
                            "Forbidden: Client mismatch between operator and target user"));
                  }

                  // Preserva i ruoli dell'utente target (modificabili solo tramite RolesController)
                  userToUpdate.setRoles(targetUser.getRoles());
                }
              }

              userToUpdate = UserMapper.updateUser(targetUser, userToUpdate);

              return userService.updateUser(identifier, userToUpdate);
            })
        .map(UserMapper::toResponse)
        .doOnSuccess(
            updatedUser ->
                LOG.info(
                    "✅ {} Profile update successfully completed for username: {}, identifier: {}",
                    USER_ACCOUNT_LOG,
                    updatedUser.getUsername(),
                    identifier));
  }

  /**
   * Verifica se l'utente target appartiene ad almeno uno dei Client OAuth2 per cui l'operatore
   * possiede ruoli con il permesso richiesto.
   *
   * @param targetUser Utente target recuperato dal database con i rispettivi ruoli assegnati.
   * @param authorizedRoles Ruoli dell'operatore che concedono il permesso richiesto.
   * @return true se targetUser possiede almeno un ruolo associato a uno dei client autorizzati
   *     dell'operatore, false altrimenti.
   */
  private boolean hasMatchingClient(User targetUser, List<RoleDefinition> authorizedRoles) {
    if (ObjectToolkit.isNullOrEmpty(targetUser)
        || ObjectToolkit.isNullOrEmpty(targetUser.getRoles())
        || ObjectToolkit.isNullOrEmpty(authorizedRoles)) {
      return false;
    }

    return targetUser.getRoles().stream()
        .anyMatch(
            userRole ->
                authorizedRoles.stream()
                    .anyMatch(
                        correctRole ->
                            !ObjectToolkit.isNullOrEmpty(userRole.getClientId())
                                && !ObjectToolkit.isNullOrEmpty(correctRole.getClientId())
                                && correctRole
                                    .getClientId()
                                    .trim()
                                    .equalsIgnoreCase(userRole.getClientId().trim())));
  }

  /**
   * Avvia la procedura di richiesta reset password (Step 1). Genera un token univoco di reset,
   * aggiorna il record utente e, se {@code sendEmail} e' abilitato, invia l'email localizzata.
   *
   * @param changePassword DTO con email del destinatario, clientId e templateId opzionale.
   * @param locale Lingua di localizzazione per il template di posta.
   * @param sendEmail Flag per abilitare l'invio asincrono del messaggio email.
   * @return Mono contenente {@link EmailResponse} con i dati di consegna o il token generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> changePasswordRequest(
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
        userService
            .getUserByEmail(changePassword.getEmail())
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
              Mono<User> updatedUserMono =
                  userService.updateUser(objects.getT1().getIdentifier(), objects.getT1());
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
                    .flatMap(updatedUserMono::thenReturn);
              } else {
                return updatedUserMono.map(
                    user -> {
                      EmailResponse responseEmail = new EmailResponse();
                      responseEmail.setToken(user.getTokenReset());
                      return responseEmail;
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
   * Esegue l'aggiornamento della password a fronte di un token di recupero valido (Step 2).
   *
   * @param changePassword DTO con token di recupero e nuova password in Base64.
   * @return Mono vuoto indicante il completamento del salvataggio.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> changePassword(ChangePassword changePassword) {
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
    return userService
        .getUserByTokenReset(changePassword.getToken())
        .flatMap(
            user -> {
              user.setPassword(finalPassword);
              user.setTokenReset(null);
              return userService.updateUser(user.getIdentifier(), user, true);
            })
        .doOnSuccess(
            user ->
                LOG.info(
                    "✅ [PASSWORD UPDATE] Password successfully changed for user: {}",
                    user.getUsername()))
        .then();
  }
}
