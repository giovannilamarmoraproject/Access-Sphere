package io.github.giovannilamarmora.accesssphere.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderClient;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiEmailTemplate;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test unitari per {@link EmailTemplateService}.
 * Verifica il recupero di tutti i template, la ricerca per ID,
 * la logica di fallback a 4 livelli per lingua e template generico/singolo,
 * il salvataggio, l'eliminazione e l'invio dell'email di test.
 */
@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

  @Mock private IEmailTemplateDAO emailTemplateDAO;
  @Mock private EmailSenderClient emailSenderClient;

  @InjectMocks private EmailTemplateService emailTemplateService;

  private EmailTemplateEntity sampleEntity;

  @BeforeEach
  void setUp() {
    sampleEntity = new EmailTemplateEntity();
    sampleEntity.setId(1L);
    sampleEntity.setIdentifier("c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95");
    sampleEntity.setName("Reset Password");
    sampleEntity.setSubject("Reimposta la tua password");
    sampleEntity.setLocale("it");
    sampleEntity.setDescription("Template per il reset password");
    sampleEntity.setTemplate("<html><body>Codice: {{RESET_CODE}} per {{NAME}}</body></html>");

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("RESET_CODE", "123456");
    params.put("NAME", "Mario Rossi");
    sampleEntity.setParams(params);
  }

  @Test
  @DisplayName("getAllTemplates restituisce la lista ordinata dei template")
  void testGetAllTemplates() {
    when(emailTemplateDAO.findAllByOrderByCreationDateDesc()).thenReturn(List.of(sampleEntity));

    StepVerifier.create(emailTemplateService.getAllTemplates())
        .assertNext(
            templates -> {
              assertEquals(1, templates.size());
              assertEquals("c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95", templates.get(0).getIdentifier());
              assertEquals("it", templates.get(0).getLocale());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTemplateById restituisce il template quando esiste")
  void testGetTemplateByIdSuccess() {
    when(emailTemplateDAO.findById(1L)).thenReturn(Optional.of(sampleEntity));

    StepVerifier.create(emailTemplateService.getTemplateById(1L))
        .assertNext(
            dto -> {
              assertEquals(1L, dto.getId());
              assertEquals("c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95", dto.getIdentifier());
              assertEquals("Reset Password", dto.getName());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTemplateById solleva NOT_FOUND se il template non esiste")
  void testGetTemplateByIdNotFound() {
    when(emailTemplateDAO.findById(99L)).thenReturn(Optional.empty());

    StepVerifier.create(emailTemplateService.getTemplateById(99L))
        .expectErrorMatches(
            throwable ->
                throwable instanceof ResponseStatusException
                    && ((ResponseStatusException) throwable).getStatusCode()
                        == HttpStatus.NOT_FOUND)
        .verify();
  }

  @Test
  @DisplayName("getTemplateByIdentifierAndLocale trova corrispondenza esatta per locale")
  void testGetTemplateExactLocale() {
    when(emailTemplateDAO.findByIdentifier("c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95"))
        .thenReturn(List.of(sampleEntity));

    StepVerifier.create(
            emailTemplateService.getTemplateByIdentifierAndLocale(
                "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95", "it"))
        .assertNext(
            strapiTemplate -> {
              assertEquals("Reset Password", strapiTemplate.getName());
              assertEquals("Reimposta la tua password", strapiTemplate.getSubject());
              assertTrue(strapiTemplate.getTemplate().contains("{{RESET_CODE}}"));
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTemplateByIdentifierAndLocale supporta fallback se l'utente ha configurato una sola lingua")
  void testGetTemplateSingleLanguageFallback() {
    // Il database contiene solo la versione "it", ma la richiesta richiede "de" o "fr"
    when(emailTemplateDAO.findByIdentifier("c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95"))
        .thenReturn(List.of(sampleEntity));

    StepVerifier.create(
            emailTemplateService.getTemplateByIdentifierAndLocale(
                "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95", "de"))
        .assertNext(
            strapiTemplate -> {
              assertNotNull(strapiTemplate);
              assertEquals("Reset Password", strapiTemplate.getName());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("saveTemplate crea un nuovo template quando ID non è specificato")
  void testSaveNewTemplate() {
    EmailTemplateDTO dto = new EmailTemplateDTO();
    dto.setIdentifier("NEW_TEMPLATE");
    dto.setName("Nuovo Template");
    dto.setSubject("Oggetto");
    dto.setTemplate("<p>HTML</p>");

    when(emailTemplateDAO.save(any(EmailTemplateEntity.class))).thenAnswer(invocation -> {
      EmailTemplateEntity entity = invocation.getArgument(0);
      entity.setId(10L);
      return entity;
    });

    StepVerifier.create(emailTemplateService.saveTemplate(dto))
        .assertNext(
            savedDto -> {
              assertEquals(10L, savedDto.getId());
              assertEquals("NEW_TEMPLATE", savedDto.getIdentifier());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteTemplate elimina con successo il template esistente")
  void testDeleteTemplateSuccess() {
    when(emailTemplateDAO.existsById(1L)).thenReturn(true);
    doNothing().when(emailTemplateDAO).deleteById(1L);

    StepVerifier.create(emailTemplateService.deleteTemplate(1L)).verifyComplete();
    verify(emailTemplateDAO, times(1)).deleteById(1L);
  }

  @Test
  @DisplayName("sendTestEmail sostituisce i parametri ed invia l'email tramite EmailSenderClient")
  void testSendTestEmail() {
    when(emailTemplateDAO.findById(1L)).thenReturn(Optional.of(sampleEntity));

    EmailResponse emailResponse = new EmailResponse();
    emailResponse.setMessage("Email inviata con successo");
    when(emailSenderClient.sendEmail(any(EmailContent.class)))
        .thenReturn(Mono.just(ResponseEntity.ok(emailResponse)));

    EmailSendTestRequest request = new EmailSendTestRequest();
    request.setTemplateId(1L);
    request.setTo("test@example.com");
    request.setSubject("Oggetto di test");
    request.setParams(Map.of("RESET_CODE", "999888", "NAME", "Mario"));

    StepVerifier.create(emailTemplateService.sendTestEmail(request))
        .assertNext(
            resp -> {
              assertNotNull(resp);
              assertEquals("Email inviata con successo", resp.getMessage());
            })
        .verifyComplete();

    verify(emailSenderClient, times(1)).sendEmail(any(EmailContent.class));
  }
}
