package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * Contratto strategico per i diversi provider di autenticazione a due fattori (MFA).
 *
 * <p>Implementato specificamente per {@code TOTP} (Time-Based One-Time Password RFC 6238) e {@code
 * EMAIL} (codice monouso a 6 cifre recapitato via posta elettronica).
 */
public interface MFAStrategy {

  /**
   * Inizializza la configurazione MFA generando il secret temporaneo o il primo codice OTP.
   *
   * @param user Utente per il quale configurare l'MFA.
   * @param setupRequest Richiesta di setup con parametri specifici.
   * @return {@link Mono} con {@link ResponseEntity} contenente il payload informativo.
   */
  Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest setupRequest);

  /**
   * Convalida il codice OTP fornito dall'utente a fronte dei metodi registrati.
   *
   * @param mfaMethods Lista dei metodi associati all'utente.
   * @param otp Codice a 6 cifre da verificare.
   * @param identifier Identificativo dell'utente per logging ed eccezioni.
   */
  void verifyCode(List<MFAMethod> mfaMethods, String otp, String identifier);

  /**
   * Invia o reinvia un codice OTP all'utente sul canale dedicato (es. Email).
   *
   * @param user Utente destinatario del codice.
   * @return {@link Mono} booleano indicante il completamento con successo dell'operazione.
   */
  default Mono<Boolean> sendCode(User user) {
    return Mono.just(true);
  }
}
