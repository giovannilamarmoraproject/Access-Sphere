package io.github.giovannilamarmora.accesssphere.email.sender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDTO;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EmailSenderModuleTest {

  @Mock private JavaMailSender javaMailSender;
  @Mock private EmailDispatcher emailDispatcher;

  @InjectMocks private EmailSenderService emailSenderService;

  private EmailDispatcher realDispatcher;

  @BeforeEach
  void setUp() {
    realDispatcher = new EmailDispatcher();
    ReflectionTestUtils.setField(realDispatcher, "emailSender", javaMailSender);
    ReflectionTestUtils.setField(
        realDispatcher, "senderEmails", List.of("noreply@access-sphere.com", "admin@access-sphere.com"));
  }

  @Test
  void testEmailMapperSimpleMessage() {
    EmailContent content =
        EmailContent.builder()
            .to("test@example.com")
            .from("noreply@access-sphere.com")
            .subject("Benvenuto")
            .text("Corpo testo semplice")
            .sentDate(new Date())
            .build();

    SimpleMailMessage message = EmailMapper.getSimpleMailMessage(content);

    assertNotNull(message);
    assertEquals("test@example.com", message.getTo()[0]);
    assertEquals("noreply@access-sphere.com", message.getFrom());
    assertEquals("Benvenuto", message.getSubject());
    assertEquals("Corpo testo semplice", message.getText());
  }

  @Test
  void testEmailMapperMimeMessage() {
    Session session = Session.getInstance(new Properties());
    MimeMessage mimeMessage = new MimeMessage(session);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    EmailContent content =
        EmailContent.builder()
            .to("test@example.com")
            .subject("Richiesta")
            .text("<h1>Titolo HTML</h1>")
            .build();

    MimeMessage result =
        EmailMapper.getMimeMessageHelper(content, true, javaMailSender);

    assertNotNull(result);
    verify(javaMailSender, times(1)).createMimeMessage();
  }

  @Test
  void testDispatcherSendsSimpleEmail() {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo("test@example.com");
    message.setSubject("Test");
    message.setText("Contenuto");

    doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

    assertDoesNotThrow(() -> realDispatcher.sendMessage(message, null));
    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    assertEquals("noreply@access-sphere.com", message.getFrom());
  }

  @Test
  void testEmailSenderServiceSendWithTemplate() {
    EmailTemplateDTO templateDTO =
        EmailTemplateDTO.builder()
            .template("<h1>Ciao {{username}}</h1><p>Codice: {{code}}</p>")
            .params(Map.of("username", "{{username}}", "code", "{{code}}"))
            .build();

    EmailContent emailContent =
        EmailContent.builder().to("user@example.com").subject("Reset Password").build();

    Session session = Session.getInstance(new Properties());
    when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(session));

    StepVerifier.create(
            emailSenderService.sendEmail(
                templateDTO,
                Map.of("username", "Mario", "code", "123456"),
                emailContent))
        .assertNext(
            (EmailResponse res) -> {
              assertNotNull(res);
              assertTrue(res.getMessage().contains("user@example.com"));
              assertEquals("<h1>Ciao Mario</h1><p>Codice: 123456</p>", emailContent.getText());
            })
        .verifyComplete();
  }
}
