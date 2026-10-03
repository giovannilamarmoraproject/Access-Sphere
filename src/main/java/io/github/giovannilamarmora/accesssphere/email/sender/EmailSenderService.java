package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Implementazione predefinita e nativa di {@link EmailSender} per Access-Sphere.
 *
 * <p>Caratteristiche architetturali:
 * <ul>
 *   <li>Gestisce la compilazione dinamica dei segnaposto {{KEY}} nei template e l'invio nativo SMTP.</li>
 *   <li>Esegue il dispatching I/O bloccante del server SMTP sul pool reattivo {@link Schedulers#boundedElastic()} per non intasare l'Event Loop Netty.</li>
 *   <li>Consuma {@link EmailDispatcher} per la validazione del mittente e l'invio fisico.</li>
 *   <li>Consuma {@link EmailMapper} per la costruzione di {@link SimpleMailMessage} o {@link MimeMessage} (UTF-8, HTML).</li>
 * </ul>
 */
@Service
@Logged
public class EmailSenderService implements EmailSender {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private EmailDispatcher dispatcher;
  @Autowired private JavaMailSender javaMailSender;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(EmailContent emailContent) {
    LOG.info("🚀 Starting native SMTP sendEmail to recipient: {}, from: {}", emailContent.getTo(), emailContent.getFrom());

    return Mono.fromCallable(() -> {
      // Se il testo dell'email contiene marcatori HTML oppure se è formattato, utilizziamo MimeMessageHelper con UTF-8
      boolean isHtml = emailContent.getText() != null
          && (emailContent.getText().contains("<html")
              || emailContent.getText().contains("<div")
              || emailContent.getText().contains("<p>")
              || emailContent.getText().contains("<br")
              || emailContent.getText().contains("<!DOCTYPE"));

      if (isHtml) {
        MimeMessage mimeMessage = EmailMapper.getMimeMessageHelper(emailContent, true, javaMailSender);
        dispatcher.sendMessage(null, mimeMessage);
      } else {
        SimpleMailMessage simpleMailMessage = EmailMapper.getSimpleMailMessage(emailContent);
        dispatcher.sendMessage(simpleMailMessage, null);
      }

      EmailResponse response = new EmailResponse();
      response.setMessage(
          String.format("Email to %s was successfully sent!", emailContent.getTo()));
      return response;
    })
    .subscribeOn(Schedulers.boundedElastic())
    .doOnSuccess(res -> LOG.info("🚀 Successfully sent email to: {}", emailContent.getTo()))
    .doOnError(err -> LOG.error("❌ Failed to send email to {}: {}", emailContent.getTo(), err.getMessage()));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateDTO emailTemplateDTO, Map<String, String> params, EmailContent emailContent)
      throws UtilsException {

    LOG.info("📧 Starting email template compilation for recipient: {}", emailContent.getTo());
    String template = emailTemplateDTO.getTemplate();
    Map<String, String> finalParam =
        Utils.getFinalMapFromValue(params, emailTemplateDTO.getParams());

    LOG.debug("Replacing placeholders in the template with provided parameters");
    for (String key : finalParam.keySet()) {
      template = template.replace("{{" + key + "}}", finalParam.get(key));
    }
    if (params != null) {
      for (String key : params.keySet()) {
        if (params.get(key) != null) {
          template = template.replace("{{" + key + "}}", params.get(key));
        }
      }
    }

    emailContent.setText(template);
    LOG.info("Email template successfully compiled, proceeding with sending");

    return sendEmail(emailContent);
  }
}
