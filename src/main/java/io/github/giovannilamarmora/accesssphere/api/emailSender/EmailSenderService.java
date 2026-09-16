package io.github.giovannilamarmora.accesssphere.api.emailSender;

import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Logged
@Component
public class EmailSenderService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private EmailSenderClient emailSenderClient;

  /**
   * Compiles the email template with the provided parameters and sends the email using the EmailSenderClient.
   *
   * @param emailTemplateDTO the DTO containing the template text and default parameters
   * @param params           dynamic parameters to inject into the template
   * @param emailContent     the email context (e.g., sender, receiver, subject)
   * @return a {@link Mono} emitting an {@link EmailResponse} with the result of the sending operation
   * @throws UtilsException if generic utility processing fails
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateDTO emailTemplateDTO, Map<String, String> params, EmailContent emailContent)
      throws UtilsException {
    
    LOG.info("Starting email preparation for recipient: {}", emailContent.getTo());
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
    LOG.info("Email template successfully compiled, proceeding with sending via EmailSenderClient");
    
    return emailSenderClient
        .sendEmail(emailContent)
        .flatMap(
            emailResponseResponseEntity -> {
              if (ObjectUtils.isEmpty(emailResponseResponseEntity.getBody())) {
                LOG.error("Email sending returned an empty response from the client");
                return Mono.error(new EmailException("Invalid Data Provided"));
              }
              LOG.info("Email successfully sent to recipient: {}", emailContent.getTo());
              return Mono.just(emailResponseResponseEntity.getBody());
            });
  }
}
