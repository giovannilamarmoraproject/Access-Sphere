package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.email.sender.client.EmailSenderClient;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Implementazione predefinita di {@link EmailSender} per Access-Sphere. Gestisce la compilazione
 * dei segnaposto {{KEY}} nei template e delega il trasporto effettivo a {@link EmailSenderClient}
 * (WebClient verso il microservizio email-sender).
 *
 * <p>Predisposto per accogliere o essere affiancato in futuro da un'implementazione nativa SMTP
 * (es. SmtpEmailSenderService) senza alterare i servizi consumatori.
 */
@Service
@Logged
public class EmailSenderService implements EmailSender {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private EmailSenderClient emailSenderClient;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(EmailContent emailContent) {
    LOG.info("📧 Sending email directly to recipient: {}", emailContent.getTo());
    return emailSenderClient
        .sendEmail(emailContent)
        .flatMap(
            responseEntity -> {
              if (ObjectUtils.isEmpty(responseEntity.getBody())) {
                LOG.error("❌ Email sending returned an empty response from transport client");
                return Mono.error(new EmailException("Invalid Data Provided"));
              }
              LOG.info("✅ Email successfully sent to recipient: {}", emailContent.getTo());
              return Mono.just(responseEntity.getBody());
            });
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateDTO emailTemplateDTO, Map<String, String> params, EmailContent emailContent)
      throws UtilsException {

    LOG.info("📧 Starting email preparation for recipient: {}", emailContent.getTo());
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
