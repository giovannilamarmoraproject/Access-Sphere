package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Componente core di dispatching SMTP incaricato della validazione dei mittenti e dell'invio fisico dei messaggi.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Consumato da {@link EmailSenderService} per la spedizione dei messaggi costruiti da {@link EmailMapper}.</li>
 *   <li>Inietta {@link JavaMailSender} configurato tramite le proprietà applicative {@code spring.mail.*}.</li>
 *   <li>Valida l'indirizzo mittente contro la lista bianca configurata in {@code spring.mail.sender-emails}.</li>
 * </ul>
 */
@Component
@Logged
public class EmailDispatcher {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private JavaMailSender emailSender;

  @Value(value = "${spring.mail.sender-emails:}")
  private List<String> senderEmails;

  /**
   * Spedisce un messaggio email semplice o MIME via server SMTP configurato.
   *
   * @param simpleMailMessage Messaggio semplice di testo, o {@code null} se si trasmette un MIME message.
   * @param mimeMessage Messaggio MIME strutturato (HTML o con allegati), o {@code null} se messaggio semplice.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void sendMessage(SimpleMailMessage simpleMailMessage, MimeMessage mimeMessage) {
    validateEmails(simpleMailMessage, mimeMessage);
    if (simpleMailMessage != null) {
      LOG.info("Sending simple email with Subject: {}", simpleMailMessage.getSubject());
      try {
        emailSender.send(simpleMailMessage);
      } catch (MailException exception) {
        LOG.error("An error occurred during sending simple email, message: {}", exception.getMessage());
        throw new EmailException(
            ExceptionMap.ERR_MAIL_SEND_002,
            "An error occurred during send email",
            exception.getMessage());
      }
    } else if (mimeMessage != null) {
      try {
        LOG.info("Sending MIME email with Subject: {}", mimeMessage.getSubject());
      } catch (MessagingException e) {
        throw new EmailException(
            ExceptionMap.ERR_MAIL_SEND_002, "An error occurred during send email", e.getMessage());
      }
      try {
        emailSender.send(mimeMessage);
      } catch (MailException exception) {
        LOG.error("An error occurred during sending MIME email, message: {}", exception.getMessage());
        throw new EmailException(
            ExceptionMap.ERR_MAIL_SEND_002,
            "An error occurred during send email",
            exception.getMessage());
      }
    }
  }

  /**
   * Valida la coerenza del mittente impostato nel messaggio rispetto agli indirizzi configurati.
   * Se il mittente è omesso, imposta automaticamente il primo indirizzo della lista autorizzata.
   */
  private void validateEmails(SimpleMailMessage simpleMailMessage, MimeMessage mimeMessage) {
    String emailFrom;
    boolean hasSenderEmails = senderEmails != null && !senderEmails.isEmpty();
    String defaultSender = hasSenderEmails ? senderEmails.getFirst() : null;

    if (!ObjectToolkit.isNullOrEmpty(simpleMailMessage)) {
      emailFrom =
          ObjectToolkit.isNullOrEmpty(simpleMailMessage.getFrom())
              ? defaultSender
              : simpleMailMessage.getFrom();
      if (emailFrom != null) {
        simpleMailMessage.setFrom(emailFrom);
      }
    } else if (mimeMessage != null) {
      try {
        emailFrom =
            (mimeMessage.getFrom() == null
                    || mimeMessage.getFrom().length == 0
                    || ObjectToolkit.isNullOrEmpty(mimeMessage.getFrom()[0]))
                ? defaultSender
                : mimeMessage.getFrom()[0].toString();
        if (emailFrom != null) {
          mimeMessage.setFrom(emailFrom);
        }
      } catch (MessagingException e) {
        throw new EmailException(
            ExceptionMap.ERR_MAIL_SEND_002, "An error occurred during send email", e.getMessage());
      }
    } else {
      return;
    }

    if (hasSenderEmails && emailFrom != null && !senderEmails.contains(emailFrom)) {
      LOG.error(
          "The email {} does not match the sender email configuration {}", emailFrom, senderEmails);
      throw new EmailException(
          ExceptionMap.ERR_VALID_MAIL_001, "Email sender does not match allowed configuration");
    }
  }
}
