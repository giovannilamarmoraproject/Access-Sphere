package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import jakarta.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
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
 *
 * <ul>
 *   <li>Gestisce la compilazione dinamica dei segnaposto {{KEY}} nei template e l'invio nativo
 *       SMTP.
 *   <li>Esegue il dispatching I/O bloccante del server SMTP sul pool reattivo {@link
 *       Schedulers#boundedElastic()} per non intasare l'Event Loop Netty.
 *   <li>Consuma {@link EmailDispatcher} per la validazione del mittente e l'invio fisico.
 *   <li>Consuma {@link EmailMapper} per la costruzione di {@link SimpleMailMessage} o {@link
 *       MimeMessage} (UTF-8, HTML).
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
    LOG.info(
        "🚀 Starting native SMTP sendEmail to recipient: {}, from: {}",
        emailContent.getTo(),
        emailContent.getFrom());

    return Mono.fromCallable(
            () -> {
              // Se il testo dell'email contiene marcatori HTML oppure se è formattato, utilizziamo
              // MimeMessageHelper con UTF-8
              boolean isHtml =
                  emailContent.getText() != null
                      && (emailContent.getText().contains("<html")
                          || emailContent.getText().contains("<div")
                          || emailContent.getText().contains("<p>")
                          || emailContent.getText().contains("<br")
                          || emailContent.getText().contains("<!DOCTYPE"));

              if (isHtml) {
                MimeMessage mimeMessage =
                    EmailMapper.getMimeMessageHelper(emailContent, true, javaMailSender);
                dispatcher.sendMessage(null, mimeMessage);
              } else {
                SimpleMailMessage simpleMailMessage =
                    EmailMapper.getSimpleMailMessage(emailContent);
                dispatcher.sendMessage(simpleMailMessage, null);
              }

              EmailResponse response = new EmailResponse();
              response.setMessage(
                  String.format("Email to %s was successfully sent!", emailContent.getTo()));
              return response;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(res -> LOG.info("🚀 Successfully sent email to: {}", emailContent.getTo()))
        .doOnError(
            err ->
                LOG.error(
                    "❌ Failed to send email to {}: {}", emailContent.getTo(), err.getMessage()));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateResponse emailTemplateResponse,
      Map<String, String> params,
      EmailContent emailContent)
      throws UtilsException {

    LOG.info("📧 Starting email template compilation for recipient: {}", emailContent.getTo());
    String template = emailTemplateResponse.getTemplate();

    // 1. Risoluzione dei parametri dinamici in ingresso (USER, APP, AUTH, CLIENT)
    Map<String, String> resolvedInputParams = EmailParameterResolver.resolve(params);

    // 2. Integrazione con i parametri definiti a livello di template
    Map<String, String> mergedParams = new LinkedHashMap<>(resolvedInputParams);
    if (emailTemplateResponse.getParams() != null && !emailTemplateResponse.getParams().isEmpty()) {
      Map<String, String> templateParams =
          Utils.getFinalMapFromValue(resolvedInputParams, emailTemplateResponse.getParams());
      for (Map.Entry<String, String> entry : templateParams.entrySet()) {
        if (entry.getValue() != null
            && (!entry.getValue().startsWith("{{") || !entry.getValue().endsWith("}}"))) {
          mergedParams.put(entry.getKey(), entry.getValue());
        }
      }
    }

    // 3. Normalizzazione ed espansione finale
    Map<String, String> finalParam = EmailParameterResolver.resolve(mergedParams);

    LOG.debug("Replacing placeholders in the template with resolved namespaced parameters");
    for (Map.Entry<String, String> entry : finalParam.entrySet()) {
      if (entry.getValue() != null
          && (!entry.getValue().startsWith("{{") || !entry.getValue().endsWith("}}"))) {
        template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
      }
    }

    emailContent.setText(template);
    LOG.info("Email template successfully compiled, proceeding with sending");

    return sendEmail(emailContent);
  }
}
