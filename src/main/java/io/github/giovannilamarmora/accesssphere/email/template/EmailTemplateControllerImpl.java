package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDeleteResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateListResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per l'amministrazione dei Template Email.
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.EMAIL_TEMPLATE, description = OpenAPI.Description.EMAIL_TEMPLATE)
public class EmailTemplateControllerImpl implements EmailTemplateController {

  @Autowired private EmailTemplateService emailTemplateService;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<EmailTemplateListResponse>> getAllTemplates(
      String bearer, ServerHttpRequest request) {
    return emailTemplateService
        .getAllTemplates()
        .map(
            templates ->
                ResponseEntity.ok(
                    new EmailTemplateListResponse(
                        HttpStatus.OK.value(),
                        "Email Templates List",
                        TraceUtils.getSpanID(),
                        templates)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<EmailTemplateResponse>> createTemplate(
      String bearer, EmailTemplateRequest dto, ServerHttpRequest request) {
    return emailTemplateService
        .saveTemplate(dto)
        .map(
            saved ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new EmailTemplateResponse(
                            HttpStatus.CREATED.value(),
                            "Email Template successfully created",
                            TraceUtils.getSpanID(),
                            saved)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<EmailTemplateResponse>> updateTemplate(
      String bearer, String identifier, EmailTemplateRequest dto, ServerHttpRequest request) {
    return emailTemplateService
        .updateTemplate(identifier, dto)
        .map(
            updated ->
                ResponseEntity.ok(
                    new EmailTemplateResponse(
                        HttpStatus.OK.value(),
                        "Email Template successfully updated",
                        TraceUtils.getSpanID(),
                        updated)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<EmailTemplateDeleteResponse>> deleteTemplate(
      String bearer, String identifier, ServerHttpRequest request) {
    return emailTemplateService
        .deleteTemplate(identifier)
        .thenReturn(
            ResponseEntity.ok(
                new EmailTemplateDeleteResponse(
                    HttpStatus.OK.value(),
                    "Email Template successfully deleted",
                    TraceUtils.getSpanID(),
                    null)));
  }

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
                        "Test email sent successfully",
                        TraceUtils.getSpanID(),
                        response)));
  }
}
