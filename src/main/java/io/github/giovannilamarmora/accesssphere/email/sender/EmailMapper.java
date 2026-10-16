package io.github.giovannilamarmora.accesssphere.email.sender;

import io.github.giovannilamarmora.accesssphere.email.exception.EmailException;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Mapper per la conversione di {@link EmailContent} in {@link SimpleMailMessage} o {@link
 * MimeMessage} (UTF-8, HTML).
 */
@Component
public class EmailMapper {

  private static final Logger LOG = LoggerFilter.getLogger(EmailMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static SimpleMailMessage getSimpleMailMessage(EmailContent emailContent) {
    LOG.debug("Mapping EmailContent to SimpleMailMessage for recipient: {}", emailContent.getTo());
    SimpleMailMessage message = new SimpleMailMessage();

    if (!ObjectToolkit.isNullOrEmpty(emailContent.getBbc())) message.setBcc(emailContent.getBbc());
    if (!ObjectToolkit.isNullOrEmpty(emailContent.getCc())) message.setCc(emailContent.getCc());
    if (!ObjectToolkit.isNullOrEmpty(emailContent.getFrom()))
      message.setFrom(emailContent.getFrom());
    if (!ObjectToolkit.isNullOrEmpty(emailContent.getReplyTo()))
      message.setReplyTo(emailContent.getReplyTo());
    if (emailContent.getSentDate() != null) message.setSentDate(emailContent.getSentDate());

    message.setSubject(emailContent.getSubject());
    message.setText(emailContent.getText());
    message.setTo(emailContent.getTo());
    return message;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MimeMessage getMimeMessageHelper(
      EmailContent emailContent, Boolean htmlText, JavaMailSender sender) {
    LOG.debug("Mapping EmailContent to MimeMessage for recipient: {}", emailContent.getTo());
    MimeMessage message = sender.createMimeMessage();
    try {
      boolean isHtml = htmlText == null || htmlText;
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

      if (!ObjectToolkit.isNullOrEmpty(emailContent.getBbc())) helper.setBcc(emailContent.getBbc());
      if (!ObjectToolkit.isNullOrEmpty(emailContent.getCc())) helper.setCc(emailContent.getCc());
      if (!ObjectToolkit.isNullOrEmpty(emailContent.getFrom()))
        helper.setFrom(emailContent.getFrom());
      if (!ObjectToolkit.isNullOrEmpty(emailContent.getReplyTo()))
        helper.setReplyTo(emailContent.getReplyTo());
      if (emailContent.getSentDate() != null) helper.setSentDate(emailContent.getSentDate());

      helper.setSubject(emailContent.getSubject());
      helper.setTo(emailContent.getTo());
      helper.setText(emailContent.getText(), isHtml);
    } catch (MessagingException e) {
      LOG.error("❌ Error during MimeMessage preparation: {}", e.getMessage());
      throw new EmailException(
          ExceptionMap.ERR_MAIL_SEND_002,
          ExceptionMap.ERR_MAIL_SEND_002.getMessage(),
          e.getMessage());
    }
    return message;
  }
}
