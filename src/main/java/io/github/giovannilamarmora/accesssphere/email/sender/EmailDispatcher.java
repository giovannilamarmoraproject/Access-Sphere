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
 * Componente core di dispatching SMTP incaricato della validazione dei mittenti e dell'invio fisico
 * dei messaggi tramite {@link JavaMailSender}.
 */
@Component
@Logged
public class EmailDispatcher {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private JavaMailSender emailSender;

  @Value(value = "${spring.mail.sender-emails:}")
  private List<String> senderEmails;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void sendMessage(SimpleMailMessage simpleMailMessage, MimeMessage mimeMessage) {
    validateEmails(simpleMailMessage, mimeMessage);
    try {
      if (simpleMailMessage != null) {
        LOG.info("📤 Sending simple email with Subject: {}", simpleMailMessage.getSubject());
        emailSender.send(simpleMailMessage);
      } else if (mimeMessage != null) {
        LOG.info("📤 Sending MIME email with Subject: {}", mimeMessage.getSubject());
        emailSender.send(mimeMessage);
      }
    } catch (MailException | MessagingException exception) {
      LOG.error("❌ Error during email dispatch: {}", exception.getMessage());
      throw new EmailException(
          ExceptionMap.ERR_MAIL_SEND_002,
          "An error occurred during send email",
          exception.getMessage());
    }
  }

  private void validateEmails(SimpleMailMessage simpleMailMessage, MimeMessage mimeMessage) {
    boolean hasSenderEmails = !ObjectToolkit.isNullOrEmpty(senderEmails);
    String defaultSender = hasSenderEmails ? senderEmails.getFirst() : null;
    String emailFrom;

    if (!ObjectToolkit.isNullOrEmpty(simpleMailMessage)) {
      emailFrom =
          ObjectToolkit.isNullOrEmpty(simpleMailMessage.getFrom())
              ? defaultSender
              : simpleMailMessage.getFrom();
      if (emailFrom != null) simpleMailMessage.setFrom(emailFrom);
    } else if (mimeMessage != null) {
      try {
        emailFrom =
            (mimeMessage.getFrom() == null
                    || mimeMessage.getFrom().length == 0
                    || ObjectToolkit.isNullOrEmpty(mimeMessage.getFrom()[0]))
                ? defaultSender
                : mimeMessage.getFrom()[0].toString();
        if (emailFrom != null) mimeMessage.setFrom(emailFrom);
      } catch (MessagingException e) {
        throw new EmailException(
            ExceptionMap.ERR_MAIL_SEND_002, "An error occurred during send email", e.getMessage());
      }
    } else {
      return;
    }

    if (hasSenderEmails && emailFrom != null && !senderEmails.contains(emailFrom)) {
      LOG.error(
          "❌ Email sender {} does not match allowed configuration {}", emailFrom, senderEmails);
      throw new EmailException(
          ExceptionMap.ERR_VALID_MAIL_001, "Email sender does not match allowed configuration");
    }
  }
}
