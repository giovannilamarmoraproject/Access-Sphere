package io.github.giovannilamarmora.accesssphere.user.account;

import io.github.giovannilamarmora.accesssphere.user.dto.UserDeleteResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserUpdateRequest;
import io.github.giovannilamarmora.accesssphere.user.model.ChangePassword;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per le operazioni self-service e il profilo dell'utente autenticato.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link UserAccountController}.
 *   <li>Inoltra le richieste operative a {@link UserAccountService}.
 *   <li>Incapsula il payload restituito nell'involucro standard {@link Response} e {@link
 *       ResponseEntity}.
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.ACCOUNT, description = OpenAPI.Description.ACCOUNT)
public class UserAccountControllerImpl implements UserAccountController {

  private static final Logger LOG = LoggerFilter.getLogger(UserAccountControllerImpl.class);

  @Autowired private UserAccountService userAccountService;

  /**
   * Endpoint per il recupero del profilo anagrafico dell'utente attualmente connesso.
   *
   * @param bearer Token di accesso Bearer.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e i dati del profilo.
   */
  @Override
  public Mono<ResponseEntity<UserResponse>> profile(String bearer, ServerHttpRequest request) {
    return userAccountService
        .profile(bearer, request)
        .map(
            user -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "Profile Data for %s".formatted(user.getUsername()),
                      TraceUtils.getSpanID(),
                      user);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per l'aggiornamento dei dati anagrafici del profilo utente connesso.
   *
   * @param user DTO contenente i dati anagrafici aggiornati.
   * @param bearer Token Bearer della sessione.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e i dati aggiornati.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserResponse>> updateProfile(
      UserUpdateRequest user, String bearer, ServerHttpRequest request) {
    return userAccountService
        .updateProfile(user, bearer, request)
        .map(
            updatedUser -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "User " + updatedUser.getUsername() + " successfully updated!",
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per la cancellazione definitiva del profilo dell'utente autenticato (Self-Service).
   *
   * @param bearer Token Bearer della sessione.
   * @param exchange Scambio HTTP server reattivo.
   * @return Mono contenente ResponseEntity con stato 200 OK di avvenuta eliminazione.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserDeleteResponse>> deleteProfile(
      String bearer, ServerWebExchange exchange) {
    return userAccountService
        .deleteProfile(bearer, exchange)
        .thenReturn(
            ResponseEntity.ok(
                new UserDeleteResponse(
                    HttpStatus.OK.value(),
                    "Your account has been successfully deleted",
                    TraceUtils.getSpanID(),
                    null)));
  }

  /**
   * Endpoint per l'aggiornamento dei dati anagrafici del profilo utente.
   *
   * @param user DTO contenente i dati anagrafici aggiornati.
   * @param bearer Token Bearer della sessione.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e i dati aggiornati.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<UserResponse>> updateUser(
      String identifier, UserUpdateRequest user, String bearer, ServerHttpRequest request) {
    return userAccountService
        .updateUser(identifier, user, bearer, request)
        .map(
            updatedUser -> {
              UserResponse response =
                  new UserResponse(
                      HttpStatus.OK.value(),
                      "User " + updatedUser.getUsername() + " successfully updated!",
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per richiedere il reset della password tramite invio di email o generazione di token.
   *
   * @param changePassword DTO contenente email, templateId e parametri.
   * @param locale Lingua di localizzazione del template email.
   * @param sendEmail Flag per abilitare l'invio dell'email.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'esito della notifica.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    LOG.info(
        "🌐 [ACCOUNT CONTROLLER] Invoking ChangePasswordRequest endpoint for email: {}",
        changePassword.getEmail());
    return userAccountService
        .changePasswordRequest(changePassword, locale, sendEmail)
        .map(
            emailResponse -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Email Sent! Check your email address!",
                      TraceUtils.getSpanID(),
                      emailResponse);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per l'applicazione della nuova password a fronte del token di recupero.
   *
   * @param changePassword DTO contenente token di recupero e nuova password in Base64.
   * @return Mono contenente ResponseEntity con stato 200 OK di avvenuto aggiornamento.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePassword(ChangePassword changePassword) {
    LOG.info("🌐 [ACCOUNT CONTROLLER] Invoking ChangePassword endpoint");
    return userAccountService
        .changePassword(changePassword)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(), "Password Updated!", TraceUtils.getSpanID(), null)));
  }

  /**
   * Endpoint per la modifica della password dell'utente autenticato previa validazione della vecchia password.
   *
   * @param changePassword DTO contenente vecchia e nuova password in Base64.
   * @param bearer Token di accesso Bearer inviato nell'header Authorization.
   * @return Mono contenente ResponseEntity con stato 200 OK di avvenuto aggiornamento.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changeAuthenticatedPassword(
      ChangePassword changePassword, String bearer) {
    LOG.info("🌐 [ACCOUNT CONTROLLER] Invoking authenticated ChangePassword endpoint");
    return userAccountService
        .changeAuthenticatedPassword(changePassword, bearer)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(), "Password Updated!", TraceUtils.getSpanID(), null)));
  }
}
