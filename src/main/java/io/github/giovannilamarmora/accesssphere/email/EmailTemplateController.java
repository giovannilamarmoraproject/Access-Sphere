package io.github.giovannilamarmora.accesssphere.email;

import io.github.giovannilamarmora.accesssphere.email.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller REST per la gestione completa dei Template Email e per l'invio di email di test.
 */
@Logged
@RestController
@RequestMapping("/v1/email-templates")
@Tag(name = "Email Templates", description = "API per la gestione dei template email dinamici e test di invio")
public class EmailTemplateController {

  @Autowired private EmailTemplateService emailTemplateService;

  @GetMapping
  @Operation(summary = "Ottieni tutti i template email", description = "Restituisce la lista di tutti i template email configurati a database")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getAllTemplates(ServerHttpRequest request) {
    return emailTemplateService
        .getAllTemplates()
        .map(
            templates ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Lista Template Email",
                        TraceUtils.getSpanID(),
                        templates)));
  }

  @GetMapping("/categories")
  @Operation(summary = "Ottieni categorie template email", description = "Restituisce l'elenco delle categorie di template supportate con stato di attivazione e parametri di default")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getCategories(ServerHttpRequest request) {
    return Mono.just(
        ResponseEntity.ok(
            new Response(
                HttpStatus.OK.value(),
                "Categorie Template Email",
                TraceUtils.getSpanID(),
                java.util.Arrays.stream(io.github.giovannilamarmora.accesssphere.email.model.EmailTemplateCategory.values())
                    .map(
                        cat -> {
                          java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
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
  @Operation(summary = "Ottieni template per ID", description = "Recupera il template email corrispondente all'ID specificato")
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
                        "Dettaglio Template Email",
                        TraceUtils.getSpanID(),
                        template)));
  }

  @GetMapping("/identifier/{identifier}")
  @Operation(summary = "Ottieni template per identificatore e lingua", description = "Recupera il template email per identificatore con supporto fallback multilingua")
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
                        "Template Email per Identificatore",
                        TraceUtils.getSpanID(),
                        template)));
  }

  @PostMapping
  @Operation(summary = "Crea nuovo template email", description = "Salva un nuovo template email con parametri dinamici sul database")
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
                            "Template Email creato con successo",
                            TraceUtils.getSpanID(),
                            saved)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Aggiorna template email esistente", description = "Modifica il template email con l'ID specificato")
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
                        "Template Email aggiornato con successo",
                        TraceUtils.getSpanID(),
                        updated)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Elimina template email", description = "Cancella definitivamente il template email dal database")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteTemplate(
      @PathVariable("id") Long id, ServerHttpRequest request) {
    return emailTemplateService
        .deleteTemplate(id)
        .thenReturn(
            ResponseEntity.ok(
                new Response(
                    HttpStatus.OK.value(),
                    "Template Email eliminato con successo",
                    TraceUtils.getSpanID(),
                    null)));
  }

  @PostMapping("/send-test")
  @Operation(summary = "Invia email di prova", description = "Compila il template con i parametri specificati ed effettua l'invio tramite EmailSenderClient")
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
