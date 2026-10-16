package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validator di dominio per i Template Email e le richieste di invio test.
 */
@Component
public class EmailTemplateValidator {

  private static final Logger LOG = LoggerFilter.getLogger(EmailTemplateValidator.class);

  public static void validateTemplate(EmailTemplate template) {
    if (ObjectToolkit.isNullOrEmpty(template)
        || ObjectToolkit.isNullOrEmpty(template.getName())
        || ObjectToolkit.isNullOrEmpty(template.getSubject())
        || ObjectToolkit.isNullOrEmpty(template.getTemplate())) {
      LOG.error("❌ Invalid Email Template configuration: name, subject and HTML template are required");
      throw new EmailException(
          ExceptionMap.ERR_VALID_MAIL_001,
          "Invalid Email Template: name, subject and template body are required.");
    }
  }

  public static void validateNoDuplicates(
      List<EmailTemplate> conflicts, String category, String clientId, String locale) {
    if (!ObjectToolkit.isNullOrEmpty(conflicts)) {
      String clientDesc =
          clientId != null ? ("Client [" + clientId + "]") : "Global (Access Sphere Default)";
      String localeDesc = locale != null ? ("Locale [" + locale + "]") : "Global / Fallback";
      String errorMsg =
          "An email template already exists for category [%s], %s and %s."
              .formatted(category, clientDesc, localeDesc);
      LOG.error("❌ Email template duplicate conflict: {}", errorMsg);
      throw new EmailException(ExceptionMap.ERR_VALID_MAIL_001, errorMsg);
    }
  }

  public static void validateTestRequest(EmailSendTestRequest request) {
    if (ObjectToolkit.isNullOrEmpty(request) || ObjectToolkit.isNullOrEmpty(request.getTo())) {
      LOG.error("❌ Invalid EmailSendTestRequest: recipient 'to' is required");
      throw new EmailException(ExceptionMap.ERR_VALID_MAIL_001, "Recipient 'to' is required");
    }
    if (ObjectToolkit.isNullOrEmpty(request.getCustomHtml())
        && ObjectToolkit.isNullOrEmpty(request.getIdentifier())
        && request.getTemplateId() == null) {
      LOG.error("❌ Invalid EmailSendTestRequest: an identifier or customHtml is required");
      throw new EmailException(
          ExceptionMap.ERR_VALID_MAIL_001, "An identifier or customHtml is required");
    }
  }
}
