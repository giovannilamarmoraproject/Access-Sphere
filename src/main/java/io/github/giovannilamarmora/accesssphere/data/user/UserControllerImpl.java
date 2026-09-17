package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per la gestione degli utenti IAM e delle informazioni di sessione.
 * Interconnessioni logiche: - Implementa l'interfaccia OpenAPI {@link UserController}. - Inoltra le
 * richieste operative a {@link UserService}, che elabora la logica di business e restituisce i DTO
 * di dominio puri (es. {@link User}, {@link List}, ecc.). - Costruisce l'involucro standard
 * dell'API {@link Response} (con codice di stato, messaggio, spanId di tracciamento e payload di
 * dati) e lo incapsula in {@link ResponseEntity} reattiva gestendo gli stati HTTP (200 OK o 201
 * CREATED per registrazione).
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.USERS, description = "API to manage users")
public class UserControllerImpl implements UserController {

  private static final Logger LOG = LoggerFilter.getLogger(UserControllerImpl.class);

  @Autowired private UserService userService;

  /**
   * Endpoint per il recupero delle informazioni OIDC / UserInfo dell'utente autenticato.
   *
   * @param bearer Token di accesso Bearer fornito nell'header di autorizzazione.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con i dati
   *     UserInfo.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [CONTROLLER] Invoking UserInfo endpoint");
    return userService
        .userInfo(bearer, request)
        .map(
            userInfoData -> {
              String name =
                  Optional.ofNullable(userInfoData.getUser())
                      .map(User::getUsername)
                      .orElseGet(() -> userInfoData.getUserInfo().getSub());

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "UserInfo Data for " + name,
                      TraceUtils.getSpanID(),
                      userInfoData);

              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per il recupero del profilo anagrafico dell'utente connesso.
   *
   * @param bearer Token di accesso Bearer.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con i dati del
   *     profilo.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> profile(String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [CONTROLLER] Invoking Profile endpoint");
    return userService
        .profile(bearer, request)
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

  /**
   * Endpoint riservato a utenti con privilegi tecnici per ottenere la lista di tutti gli utenti
   * censiti.
   *
   * @param bearer Token di autorizzazione dell'utente tecnico.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con la lista
   *     utenti.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userList(String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [CONTROLLER] Invoking UserList endpoint");
    return userService
        .getUsers()
        .map(
            users -> {
              Response response =
                  new Response(HttpStatus.OK.value(), "Users list", TraceUtils.getSpanID(), users);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per la registrazione di un nuovo utente nel sistema IAM. Costruisce la Response e
   * restituisce lo stato HTTP 201 CREATED.
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore per concessione ruoli tecnici.
   * @param user DTO contenente i dati del nuovo utente da registrare.
   * @param clientId Identificativo univoco del client OAuth associato.
   * @param registration_token Token segreto di registrazione configurato sul client.
   * @param assignNewClient Flag opzionale per l'assegnazione di nuove credenziali.
   * @return Mono contenente ResponseEntity con stato 201 CREATED e l'involucro Response con i dati
   *     dell'utente registrato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> registerUser(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    LOG.info("🌐 [CONTROLLER] Invoking RegisterUser endpoint for username: {}", user.getUsername());
    return userService
        .register(bearer, user, clientId, registration_token, assignNewClient)
        .map(
            registeredUser -> {
              Response response =
                  new Response(
                      HttpStatus.CREATED.value(),
                      "User " + registeredUser.getUsername() + " successfully registered!",
                      TraceUtils.getSpanID(),
                      registeredUser);
              return ResponseEntity.status(HttpStatus.CREATED).body(response);
            });
  }

  /**
   * Endpoint per l'aggiornamento dei dati anagrafici di un utente.
   *
   * @param user DTO contenente i dati aggiornati.
   * @param bearer Token Bearer della sessione.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con i dati
   *     dell'utente aggiornato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateUser(
      User user, String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [CONTROLLER] Invoking UpdateUser endpoint for username: {}", user.getUsername());
    return userService
        .updateUser(user, bearer, request)
        .map(
            updatedUser -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "User " + updatedUser.getUsername() + " successfully updated!",
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per la cancellazione definitiva di un utente (riservato ad operatori tecnici).
   *
   * @param identifier Identificatore univoco UUID dell'utente da eliminare.
   * @param bearer Token di autorizzazione tecnico.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response di conferma
   *     eliminazione.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🌐 [CONTROLLER] Invoking DeleteUser endpoint for identifier: {}", identifier);
    return userService
        .deleteUser(identifier, bearer, exchange)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(),
                    "User " + identifier + " successfully deleted",
                    TraceUtils.getSpanID(),
                    null)));
  }

  /**
   * Endpoint per bloccare o sbloccare un utente nel sistema IAM (riservato ad operatori tecnici).
   *
   * @param bearer Token di autorizzazione tecnico.
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param block Flag di blocco (true per bloccare, false per sbloccare).
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con l'utente
   *     aggiornato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> unlockUser(
      String bearer, String identifier, Boolean block) {
    LOG.info(
        "🌐 [CONTROLLER] Invoking UnlockUser/Lock endpoint for identifier: {}, block: {}",
        identifier,
        block);
    return userService
        .unlockUser(identifier, block)
        .map(
            updatedUser -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "User "
                          + updatedUser.getUsername()
                          + " successfully "
                          + (block ? "locked!" : "unlocked!"),
                      TraceUtils.getSpanID(),
                      updatedUser);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per richiedere il reset della password tramite invio di email o generazione di token.
   *
   * @param changePassword DTO contenente email, templateId e clientId.
   * @param locale Lingua di localizzazione per il template email.
   * @param sendEmail Flag per confermare l'invio dell'email di reset.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con esito della
   *     notifica o token generato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    LOG.info(
        "🌐 [CONTROLLER] Invoking ChangePasswordRequest endpoint for email: {}",
        changePassword.getEmail());
    return userService
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
   * Endpoint per l'applicazione della nuova password a fronte del token di recupero valido.
   *
   * @param changePassword DTO contenente il token di recupero e la nuova password in Base64.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response di avvenuto
   *     aggiornamento.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePassword(ChangePassword changePassword) {
    LOG.info("🌐 [CONTROLLER] Invoking ChangePassword endpoint");
    return userService
        .changePassword(changePassword)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(), "Password Updated!", TraceUtils.getSpanID(), null)));
  }
}
