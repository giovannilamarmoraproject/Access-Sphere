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

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendEmail(
      EmailTemplateDTO emailTemplateDTO, Map<String, String> params, EmailContent emailContent)
      throws UtilsException {
    String template = emailTemplateDTO.getTemplate();
    Map<String, String> finalParam =
        Utils.getFinalMapFromValue(params, emailTemplateDTO.getParams());
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
    return emailSenderClient
        .sendEmail(emailContent)
        .flatMap(
            emailResponseResponseEntity -> {
              if (ObjectUtils.isEmpty(emailResponseResponseEntity.getBody())) {
                LOG.error("Send email returned a empty object");
                throw new EmailException("Invalid Data Provided");
              }
              return Mono.just(emailResponseResponseEntity.getBody());
            });
  }
}
