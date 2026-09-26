package io.github.giovannilamarmora.accesssphere.data.user.admin;

import io.github.giovannilamarmora.accesssphere.data.user.admin.dto.AddUserRequest;
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
 * Controller reattivo REST per le operazioni amministrative e di provisioning IAM.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link UserAdminController}.
 *   <li>Inoltra le richieste operative a {@link UserAdminService}.
 *   <li>Incapsula il payload nella struttura di risposta standard {@link Response} e {@link
 *       ResponseEntity}.
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.USERS, description = OpenAPI.Description.USERS)
public class UserAdminControllerImpl implements UserAdminController {

  private static final Logger LOG = LoggerFilter.getLogger(UserAdminControllerImpl.class);

  @Autowired private UserAdminService userAdminService;

  /**
   * Endpoint per ottenere l'elenco degli utenti registrati nel sistema IAM. L'operatore tecnico
   * riceve l'elenco completo, mentre per gli utenti normali l'utenza tecnica di sistema viene
   * automaticamente filtrata a livello di backend.
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore.
   * @param request Richiesta HTTP server reattiva.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con la lista
   *     utenti.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userList(String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [ADMIN CONTROLLER] Invoking UserList endpoint");
    return userAdminService
        .getUsers(bearer)
        .map(
            users -> {
              Response response =
                  new Response(HttpStatus.OK.value(), "Users list", TraceUtils.getSpanID(), users);
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Endpoint per il provisioning e la registrazione di un nuovo utente nel sistema IAM.
   *
   * @param bearer Token di autorizzazione opzionale dell'operatore tecnico.
   * @param user DTO contenente i dati anagrafici e i ruoli strutturati del nuovo utente.
   * @return Mono contenente ResponseEntity con stato 201 CREATED e l'involucro Response con
   *     l'utente registrato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> registerUser(String bearer, AddUserRequest user) {
    LOG.info(
        "🌐 [ADMIN CONTROLLER] Invoking RegisterUser endpoint for username: {}",
        user.getUsername());
    return userAdminService
        .register(bearer, user)
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
   * Endpoint per la cancellazione definitiva di un utente (riservato ad operatori tecnici).
   *
   * @param identifier Identificatore univoco UUID dell'utente da eliminare.
   * @param bearer Token di autorizzazione tecnico.
   * @param exchange Scambio HTTP reattivo corrente.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response di conferma.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange) {
    LOG.info("🌐 [ADMIN CONTROLLER] Invoking DeleteUser endpoint for identifier: {}", identifier);
    return userAdminService
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
   * Endpoint per bloccare o sbloccare l'accesso a un utente IAM (riservato ad operatori tecnici).
   *
   * @param bearer Token di autorizzazione tecnico.
   * @param identifier Identificatore univoco UUID dell'utente.
   * @param block True per bloccare, false per sbloccare.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con l'utente
   *     aggiornato.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> unlockUser(
      String bearer, String identifier, Boolean block) {
    LOG.info(
        "🌐 [ADMIN CONTROLLER] Invoking UnlockUser/Lock endpoint for identifier: {}, block: {}",
        identifier,
        block);
    return userAdminService
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
}
