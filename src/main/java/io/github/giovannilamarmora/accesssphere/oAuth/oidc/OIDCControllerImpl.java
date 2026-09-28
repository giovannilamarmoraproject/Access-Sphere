package io.github.giovannilamarmora.accesssphere.oAuth.oidc;

import io.github.giovannilamarmora.accesssphere.user.model.User;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller REST reattivo per l'endpoint OIDC UserInfo.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link OIDCController}.
 *   <li>Inoltra la richiesta di autenticazione e risoluzione claim a {@link OIDCService}.
 *   <li>Incapsula il payload restituito nell'involucro standard {@link Response} e in {@link
 *       ResponseEntity}.
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.OIDC, description = OpenAPI.Description.OIDC)
public class OIDCControllerImpl implements OIDCController {

  private static final Logger LOG = LoggerFilter.getLogger(OIDCControllerImpl.class);

  @Autowired private OIDCService oidcService;

  /**
   * Endpoint per il recupero delle informazioni OIDC / UserInfo dell'utente autenticato.
   *
   * @param bearer Token di accesso Bearer fornito nell'header di autorizzazione.
   * @param request Richiesta HTTP server reattiva.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con i dati
   *     UserInfo.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [OIDC CONTROLLER] Invoking UserInfo endpoint");
    return oidcService
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
   * Endpoint per il recupero delle informazioni OIDC / UserInfo dell'utente autenticato.
   *
   * @param bearer Token di accesso Bearer fornito nell'header di autorizzazione.
   * @param request Richiesta HTTP server reattiva.
   * @return Mono contenente ResponseEntity con stato 200 OK e l'involucro Response con i dati
   *     UserInfo.
   */
  @Deprecated
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userInfoDeprecated(
      String bearer, ServerHttpRequest request) {
    LOG.info("🌐 [OIDC CONTROLLER] Invoking UserInfo endpoint");
    return oidcService
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
}
