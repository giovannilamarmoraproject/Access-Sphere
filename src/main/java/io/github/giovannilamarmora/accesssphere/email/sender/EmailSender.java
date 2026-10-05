package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Contratto enterprise unificato per le operazioni di invio email in Access-Sphere.
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Consumato da {@code UserAccountService} per comunicazioni transazionali (es. reset
 *       password).
 *   <li>Consumato da {@code EmailTemplateService} per l'invio di email di test e notifiche di
 *       sistema.
 *   <li>Disaccoppia l'applicazione dal meccanismo di trasporto sottostante, consentendo
 *       l'alternanza trasparente tra client REST esterni (microservizio email-sender) e motori SMTP
 *       / mail server interni dedicati.
 * </ul>
 */
public interface EmailSender {

  /**
   * Invia un'email a partire da un payload pre-compilato.
   *
   * @param emailContent Dati completi dell'email (destinatario, oggetto, corpo HTML/testo,
   *     mittente).
   * @return {@link Mono} contenente la risposta dell'operazione di invio.
   */
  Mono<EmailResponse> sendEmail(EmailContent emailContent);

  /**
   * Compila un template con i parametri specificati e invia l'email risultante.
   *
   * @param emailTemplateResponse DTO del template contenente il testo grezzo e i parametri di
   *     default.
   * @param params Parametri dinamici di contesto da sostituire nel template.
   * @param emailContent Contesto dell'email (destinatario, oggetto, ecc.).
   * @return {@link Mono} contenente la risposta dell'invio.
   * @throws UtilsException In caso di anomalie di processing o serializzazione.
   */
  Mono<EmailResponse> sendEmail(
      EmailTemplateResponse emailTemplateResponse,
      Map<String, String> params,
      EmailContent emailContent)
      throws UtilsException;
}
