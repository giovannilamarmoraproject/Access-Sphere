package io.github.giovannilamarmora.accesssphere.email.sender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDataResponse;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.user.model.User;
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
        realDispatcher,
        "senderEmails",
        List.of("noreply@access-sphere.com", "admin@access-sphere.com"));
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

    MimeMessage result = EmailMapper.getMimeMessageHelper(content, true, javaMailSender);

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
    EmailTemplateDataResponse templateDTO =
        EmailTemplateDataResponse.builder()
            .template("<h1>Ciao {{username}}</h1><p>Codice: {{code}}</p>")
            .params(Map.of("username", "{{username}}", "code", "{{code}}"))
            .build();

    EmailContent emailContent =
        EmailContent.builder().to("user@example.com").subject("Reset Password").build();

    Session session = Session.getInstance(new Properties());
    when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(session));

    StepVerifier.create(
            emailSenderService.sendEmail(
                templateDTO, Map.of("username", "Mario", "code", "123456"), emailContent))
        .assertNext(
            (EmailResponse res) -> {
              assertNotNull(res);
              assertTrue(res.getMessage().contains("user@example.com"));
              assertEquals("<h1>Ciao Mario</h1><p>Codice: 123456</p>", emailContent.getText());
            })
        .verifyComplete();
  }

  @Test
  void testEmailParameterResolverWithUserAndClient() {
    User user = new User();
    user.setName("Mario");
    user.setSurname("Rossi");
    user.setUsername("mario.rossi");
    user.setEmail("mario.rossi@example.com");
    user.setTokenReset("TOKEN-1234");

    OAuth2Client client = new OAuth2Client();
    client.setClientId("portal-client");

    Map<String, String> resolved = EmailParameterResolver.resolve(user, client, null);

    assertNotNull(resolved);
    assertEquals("Mario", resolved.get("USER.NAME"));
    assertEquals("Rossi", resolved.get("USER.SURNAME"));
    assertEquals("Mario Rossi", resolved.get("USER.FULL_NAME"));
    assertEquals("mario.rossi", resolved.get("USER.USERNAME"));
    assertEquals("mario.rossi@example.com", resolved.get("USER.EMAIL"));
    assertEquals("TOKEN-1234", resolved.get("USER.RESET_TOKEN"));
    assertEquals("TOKEN-1234", resolved.get("AUTH.RESET_CODE"));
    assertEquals("portal-client", resolved.get("CLIENT.ID"));
    assertEquals("Access Sphere", resolved.get("APP.NAME"));
    assertEquals("https://accesssphere.io", resolved.get("APP.URL"));

    // Verifica retrocompatibilità bidirezionale con chiavi legacy flat
    assertEquals("Mario", resolved.get("NAME"));
    assertEquals("Rossi", resolved.get("SURNAME"));
    assertEquals("mario.rossi", resolved.get("USERNAME"));
    assertEquals("mario.rossi@example.com", resolved.get("EMAIL"));
    assertEquals("TOKEN-1234", resolved.get("RESET_CODE"));
    assertEquals("Access Sphere", resolved.get("SITE_SHORT"));
    assertEquals("https://accesssphere.io", resolved.get("SITE_URL"));
  }

  @Test
  void testEmailSenderServiceWithDynamicMappedTemplate() {
    EmailTemplateDataResponse templateDTO =
        EmailTemplateDataResponse.builder()
            .template(
                "<p>Hi {{NAME}}! Code: {{RESET_CODE}} on {{SITE_SHORT}} ({{SITE_URL}})</p>")
            .params(
                Map.of(
                    "NAME", "USER.NAME",
                    "RESET_CODE", "AUTH.RESET_CODE",
                    "SITE_SHORT", "APP.NAME",
                    "SITE_URL", "APP.URL"))
            .build();

    EmailContent emailContent =
        EmailContent.builder().to("mario@example.com").subject("Reset").build();

    Session session = Session.getInstance(new Properties());
    when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(session));

    Map<String, String> rawParams =
        Map.of("USER.NAME", "Luigi", "AUTH.RESET_CODE", "999888");

    StepVerifier.create(emailSenderService.sendEmail(templateDTO, rawParams, emailContent))
        .assertNext(
            (EmailResponse res) -> {
              assertNotNull(res);
              assertTrue(res.getMessage().contains("mario@example.com"));
              assertEquals(
                  "<p>Hi Luigi! Code: 999888 on Access Sphere (https://accesssphere.io)</p>",
                  emailContent.getText());
            })
        .verifyComplete();
  }

  @Test
  void testEmailSenderServiceWithNamespacedTemplate() {
    EmailTemplateDataResponse templateDTO =
        EmailTemplateDataResponse.builder()
            .template(
                "<p>Hi {{USER.NAME}}! Code: {{AUTH.RESET_CODE}} on {{APP.NAME}} ({{APP.URL}})</p>")
            .params(
                Map.of(
                    "USER.NAME", "USER.NAME",
                    "AUTH.RESET_CODE", "AUTH.RESET_CODE",
                    "APP.NAME", "APP.NAME",
                    "APP.URL", "APP.URL"))
            .build();

    EmailContent emailContent =
        EmailContent.builder().to("mario@example.com").subject("Reset").build();

    Session session = Session.getInstance(new Properties());
    when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(session));

    Map<String, String> rawParams =
        Map.of("USER.NAME", "Luigi", "AUTH.RESET_CODE", "999888");

    StepVerifier.create(emailSenderService.sendEmail(templateDTO, rawParams, emailContent))
        .assertNext(
            (EmailResponse res) -> {
              assertNotNull(res);
              assertTrue(res.getMessage().contains("mario@example.com"));
              assertEquals(
                  "<p>Hi Luigi! Code: 999888 on Access Sphere (https://accesssphere.io)</p>",
                  emailContent.getText());
            })
        .verifyComplete();
  }
}
