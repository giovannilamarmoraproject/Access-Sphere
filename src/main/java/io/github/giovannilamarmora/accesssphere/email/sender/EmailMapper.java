package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Componente di mappatura e trasformazione dati per i messaggi email in uscita da Access-Sphere.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Consumato da {@link EmailSenderService} per convertire {@link EmailContent} nei messaggi Spring Mail standard.</li>
 *   <li>Supporta la generazione di {@link SimpleMailMessage} per messaggi puramente testuali.</li>
 *   <li>Supporta la generazione di {@link MimeMessage} con codifica UTF-8 e formattazione HTML per template transazionali.</li>
 * </ul>
 */
@Component
public class EmailMapper {

  private static final Logger LOG = LoggerFilter.getLogger(EmailMapper.class);

  /**
   * Trasforma un oggetto {@link EmailContent} in un {@link SimpleMailMessage} testuale leggero.
   *
   * @param emailContent DTO contenente i dati dell'email.
   * @return Istanza compilata di {@link SimpleMailMessage}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static SimpleMailMessage getSimpleMailMessage(EmailContent emailContent) {
    LOG.debug("Mapping EmailContent to SimpleMailMessage for recipient: {}", emailContent.getTo());
    SimpleMailMessage message = new SimpleMailMessage();

    if (emailContent.getBbc() != null && !emailContent.getBbc().isBlank()) {
      message.setBcc(emailContent.getBbc());
    }
    if (emailContent.getCc() != null && !emailContent.getCc().isBlank()) {
      message.setCc(emailContent.getCc());
    }
    if (emailContent.getFrom() != null && !emailContent.getFrom().isBlank()) {
      message.setFrom(emailContent.getFrom());
    }
    if (emailContent.getReplyTo() != null && !emailContent.getReplyTo().isBlank()) {
      message.setReplyTo(emailContent.getReplyTo());
    }
    if (emailContent.getSentDate() != null) {
      message.setSentDate(emailContent.getSentDate());
    }
    message.setSubject(emailContent.getSubject());
    message.setText(emailContent.getText());
    message.setTo(emailContent.getTo());
    return message;
  }

  /**
   * Costruisce un {@link MimeMessage} MIME con supporto a formattazione HTML e codifica UTF-8.
   *
   * @param emailContent Contenuto dell'email.
   * @param htmlText Flag che dichiara se il corpo del messaggio deve essere interpretato come HTML.
   * @param sender Istanza di {@link JavaMailSender} da cui istanziare il messaggio MIME.
   * @return {@link MimeMessage} completamente assemblato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MimeMessage getMimeMessageHelper(
      EmailContent emailContent,
      Boolean htmlText,
      JavaMailSender sender) {
    LOG.debug("Mapping EmailContent to MimeMessage for recipient: {}", emailContent.getTo());
    MimeMessage message = sender.createMimeMessage();
    try {
      boolean isHtml = htmlText != null ? htmlText : true;
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

      if (emailContent.getBbc() != null && !emailContent.getBbc().isBlank()) {
        helper.setBcc(emailContent.getBbc());
      }
      if (emailContent.getCc() != null && !emailContent.getCc().isBlank()) {
        helper.setCc(emailContent.getCc());
      }
      if (emailContent.getFrom() != null && !emailContent.getFrom().isBlank()) {
        helper.setFrom(emailContent.getFrom());
      }
      if (emailContent.getReplyTo() != null && !emailContent.getReplyTo().isBlank()) {
        helper.setReplyTo(emailContent.getReplyTo());
      }
      if (emailContent.getSentDate() != null) {
        helper.setSentDate(emailContent.getSentDate());
      }

      helper.setSubject(emailContent.getSubject());
      helper.setTo(emailContent.getTo());
      helper.setText(emailContent.getText(), isHtml);
    } catch (MessagingException e) {
      LOG.error("An error occurred during MimeMessage preparation: {}", e.getMessage());
      throw new EmailException(
          ExceptionMap.ERR_MAIL_SEND_002,
          ExceptionMap.ERR_MAIL_SEND_002.getMessage(),
          e.getMessage());
    }
    return message;
  }
}
