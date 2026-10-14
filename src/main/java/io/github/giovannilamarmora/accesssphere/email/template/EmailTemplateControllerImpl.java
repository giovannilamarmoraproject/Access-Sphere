package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per la gestione e l'amministrazione dei Template Email.
 *
 * <p>Responsabilità architetturali ed interconnessioni:
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link EmailTemplateController}.</li>
 *   <li>Inoltra le richieste operative, le verifiche di autorizzazione basate su RBAC e la logica di business a {@link EmailTemplateService}.</li>
 *   <li>Costruisce e serializza le risposte standard di sistema {@link ResponseEntity}
 *       incapsulandole nel wrapper {@link Response} corredato di status code HTTP, messaggio descrittivo e distributed tracing (Span ID).</li>
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.EMAIL_TEMPLATE, description = OpenAPI.Description.EMAIL_TEMPLATE)
public class EmailTemplateControllerImpl implements EmailTemplateController {

  private static final Logger LOG = LoggerFilter.getLogger(EmailTemplateControllerImpl.class);

  @Autowired private EmailTemplateService emailTemplateService;

  /**
   * {@inheritDoc}
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getAllTemplates(
      String bearer, ServerHttpRequest request) {
    return emailTemplateService
        .getAllTemplates()
        .map(
            templates ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email Templates List",
                        TraceUtils.getSpanID(),
                        templates)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> createTemplate(
      String bearer, EmailTemplateResponse dto, ServerHttpRequest request) {
    return emailTemplateService
        .saveTemplate(dto)
        .map(
            saved ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Response(
                            HttpStatus.CREATED.value(),
                            "Email Template successfully created",
                            TraceUtils.getSpanID(),
                            saved)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateTemplate(
      String bearer, String identifier, EmailTemplateResponse dto, ServerHttpRequest request) {
    return emailTemplateService
        .saveTemplate(identifier, dto)
        .map(
            updated ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email Template successfully updated",
                        TraceUtils.getSpanID(),
                        updated)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteTemplate(
      String bearer, String identifier, ServerHttpRequest request) {
    return emailTemplateService
        .deleteTemplate(identifier)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(),
                    "Email Template successfully deleted",
                    TraceUtils.getSpanID(),
                    null)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> sendTestEmail(
      String bearer, EmailSendTestRequest testRequest, ServerHttpRequest request) {
    return emailTemplateService
        .sendTestEmail(testRequest)
        .map(
            response ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email di test inviata con successo",
                        TraceUtils.getSpanID(),
                        response)));
  }
}
