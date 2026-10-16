package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.EmailUtils;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDataResponse;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
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
 * Servizio per la compilazione dei template e l'invio di email via SMTP. Esegue il dispatching I/O
 * su {@link Schedulers#boundedElastic()} delegando a {@link EmailMapper} e {@link EmailDispatcher}.
 */
@Service
@Logged
public class EmailSenderService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private EmailDispatcher dispatcher;
  @Autowired private JavaMailSender javaMailSender;

  /**
   * Invia un'email a partire da un payload {@link EmailContent} gia compilato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(EmailContent emailContent) {
    LOG.info(
        "🚀 Starting SMTP sendEmail to recipient: {}, from: {}",
        emailContent.getTo(),
        emailContent.getFrom());

    return Mono.fromCallable(
            () -> {
              if (EmailUtils.isHtmlContent(emailContent.getText())) {
                MimeMessage mimeMessage =
                    EmailMapper.getMimeMessageHelper(emailContent, true, javaMailSender);
                dispatcher.sendMessage(null, mimeMessage);
              } else {
                SimpleMailMessage simpleMailMessage =
                    EmailMapper.getSimpleMailMessage(emailContent);
                dispatcher.sendMessage(simpleMailMessage, null);
              }

              EmailResponse response = new EmailResponse();
              response.setMessage("Email to %s was successfully sent!".formatted(emailContent.getTo()));
              return response;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(res -> LOG.info("✅ Successfully sent email to: {}", emailContent.getTo()))
        .doOnError(
            err ->
                LOG.error(
                    "❌ Failed to send email to {}: {}", emailContent.getTo(), err.getMessage()));
  }

  /**
   * Compila un {@link EmailTemplate} di dominio con i parametri forniti e invia l'email risultante.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplate emailTemplate, Map<String, String> params, EmailContent emailContent) {
    LOG.info("📧 Compiling email template for recipient: {}", emailContent.getTo());
    emailContent.setText(
        EmailUtils.renderTemplate(
            emailTemplate.getTemplate(), emailTemplate.getParams(), params));
    return sendEmail(emailContent);
  }

  /**
   * Compila un {@link EmailTemplateDataResponse} con i parametri forniti e invia l'email
   * risultante.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateDataResponse emailTemplateResponse,
      Map<String, String> params,
      EmailContent emailContent) {
    LOG.info("📧 Compiling email template DTO for recipient: {}", emailContent.getTo());
    emailContent.setText(
        EmailUtils.renderTemplate(
            emailTemplateResponse.getTemplate(), emailTemplateResponse.getParams(), params));
    return sendEmail(emailContent);
  }
}
