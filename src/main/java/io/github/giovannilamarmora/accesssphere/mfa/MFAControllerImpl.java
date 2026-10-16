package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per la gestione e l'autenticazione a due fattori (MFA).
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link MFAController}.</li>
 *   <li>Delega la validazione di sicurezza e la logica applicativa a {@link MFAService}.</li>
 *   <li>Espone gli endpoint reattivi WebFlux per setup, conferma, verifica OTP login, reinvio OTP e gestione metodi.</li>
 * </ul>
 */
@Logged
@Validated
@RestController
@RequestMapping("/v1/mfa")
@Tag(name = OpenAPI.Tag.MFA, description = OpenAPI.Description.MFA)
public class MFAControllerImpl implements MFAController {

  @Autowired private MFAService mfaService;

  /**
   * Endpoint per inizializzare il setup di un metodo MFA (TOTP o EMAIL).
   *
   * @param bearer Token JWT Bearer dell'utente.
   * @param mfaSetupRequest DTO con identificativo, tipo ed eventuale richiesta QR code.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente i dati di configurazione.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> setupMfa(
      String bearer, MFASetupRequest mfaSetupRequest, ServerWebExchange exchange) {
    return mfaService.generateSecretForUser(mfaSetupRequest);
  }

  /**
   * Endpoint per confermare il codice OTP ed attivare definitivamente il metodo MFA.
   *
   * @param bearer Token JWT Bearer dell'utente.
   * @param mfaConfirmationRequest DTO con identificativo, tipo e codice OTP.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} di avvenuta attivazione.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> confirmMfa(
      String bearer, MFAConfirmationRequest mfaConfirmationRequest, ServerWebExchange exchange) {
    return mfaService.confirmMFA(mfaConfirmationRequest);
  }

  /**
   * Endpoint per verificare il codice OTP durante il flusso di login OAuth 2.1 con tempToken Bearer.
   *
   * @param mfaVerifyRequest DTO con metodo MFA, codice OTP e opzione device fidato.
   * @param bearer Token temporaneo Bearer rilasciato nella prima fase di login.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con {@link ResponseEntity} contenente i token finali rilasciati.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> verifyMfaCode(
      MfaVerificationRequest mfaVerifyRequest, String bearer, ServerWebExchange exchange) {
    return mfaService.verifyMfa(mfaVerifyRequest, bearer, exchange);
  }

  /**
   * Endpoint per inviare o reinviare il codice OTP via email.
   *
   * @param bearer Token temporaneo Bearer di login oppure token di accesso utente autenticato.
   * @param identifier Identificativo opzionale dell'utente.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con conferma dell'invio.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> sendOtp(
      String bearer, String identifier, ServerWebExchange exchange) {
    return mfaService.sendOtp(bearer, identifier);
  }

  /**
   * Endpoint per gestire lo stato dei metodi MFA (ENABLE, DISABLE o DELETE).
   *
   * @param bearer Token JWT Bearer dell'utente o operatore autorizzato.
   * @param mfaManageRequest DTO con identificativo, azione e label.
   * @param exchange Scambio HTTP server reattivo.
   * @return {@link Mono} con esito dell'azione.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> manageMfa(
      String bearer, MFAManageRequest mfaManageRequest, ServerWebExchange exchange) {
    return mfaService.manageMFA(mfaManageRequest);
  }
}
