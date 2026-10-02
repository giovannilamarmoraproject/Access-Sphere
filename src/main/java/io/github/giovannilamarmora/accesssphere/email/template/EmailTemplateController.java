package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Controller REST per la gestione completa dei Template Email e per l'invio di email di test. */
@Logged
@RestController
@RequestMapping("/v1/email-templates")
@Tag(
    name = OpenAPI.Tag.EMAIL_TEMPLATE,
    description = OpenAPI.Description.EMAIL_TEMPLATE)
public class EmailTemplateController {

  @Autowired private EmailTemplateService emailTemplateService;

  @GetMapping
  @Operation(
      summary = "Get all email templates",
      description = "Returns the list of all configured email templates from the database")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getAllTemplates(ServerHttpRequest request) {
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

  @GetMapping("/categories")
  @Operation(
      summary = "Get email template categories",
      description =
          "Returns the list of supported template categories with their active status and default parameters")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getCategories(ServerHttpRequest request) {
    return Mono.just(
        ResponseEntity.ok(
            new Response(
                HttpStatus.OK.value(),
                "Email Template Categories",
                TraceUtils.getSpanID(),
                Arrays.stream(EmailTemplateCategory.values())
                    .map(
                        cat -> {
                          Map<String, Object> map = new java.util.LinkedHashMap<>();
                          map.put("code", cat.getCode());
                          map.put("defaultIdentifier", cat.getDefaultIdentifier());
                          map.put("defaultTitle", cat.getDefaultTitle());
                          map.put("defaultDescription", cat.getDefaultDescription());
                          map.put("activeInCode", cat.isActiveInCode());
                          map.put("defaultParams", cat.getDefaultParams());
                          return map;
                        })
                    .toList())));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get template by ID",
      description = "Retrieves the email template corresponding to the specified ID")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getTemplateById(
      @PathVariable("id") Long id, ServerHttpRequest request) {
    return emailTemplateService
        .getTemplateById(id)
        .map(
            template ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email Template Detail",
                        TraceUtils.getSpanID(),
                        template)));
  }

  @GetMapping("/identifier/{identifier}")
  @Operation(
      summary = "Get template by identifier and locale",
      description = "Retrieves the email template by identifier with multilingual fallback support")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getTemplateByIdentifier(
      @PathVariable("identifier") String identifier,
      @RequestParam(value = "locale", required = false) String locale,
      ServerHttpRequest request) {
    return emailTemplateService
        .getTemplateByIdentifierAndLocale(identifier, locale)
        .map(
            template ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email Template by Identifier",
                        TraceUtils.getSpanID(),
                        template)));
  }

  @PostMapping
  @Operation(
      summary = "Create new email template",
      description = "Saves a new email template with dynamic parameters to the database")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> createTemplate(
      @Valid @RequestBody EmailTemplateDTO dto, ServerHttpRequest request) {
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

  @PutMapping("/{id}")
  @Operation(
      summary = "Update existing email template",
      description = "Modifies the email template with the specified ID")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateTemplate(
      @PathVariable("id") Long id,
      @Valid @RequestBody EmailTemplateDTO dto,
      ServerHttpRequest request) {
    dto.setId(id);
    return emailTemplateService
        .saveTemplate(dto)
        .map(
            updated ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Email Template successfully updated",
                        TraceUtils.getSpanID(),
                        updated)));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete email template",
      description = "Permanently deletes the email template from the database")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteTemplate(
      @PathVariable("id") Long id, ServerHttpRequest request) {
    return emailTemplateService
        .deleteTemplate(id)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(),
                    "Email Template successfully deleted",
                    TraceUtils.getSpanID(),
                    null)));
  }

  @PostMapping("/send-test")
  @Operation(
      summary = "Send test email",
      description =
          "Compiles the template with the specified parameters and sends it via EmailSender")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> sendTestEmail(
      @Valid @RequestBody EmailSendTestRequest testRequest, ServerHttpRequest request) {
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
