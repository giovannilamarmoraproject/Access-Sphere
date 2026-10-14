package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI / Swagger per la gestione completa dei Template Email transazionali e di sistema.
 *
 * <p>Definisce le operazioni REST esposte da Access-Sphere per il ciclo di vita dei template email:
 * <ul>
 *   <li>Consultazione dei template registrati a catalogo (con supporto multi-tenant scoping).</li>
 *   <li>Creazione di nuovi template email con binding di categoria, client e lingua.</li>
 *   <li>Aggiornamento delle proprietà, del markup HTML e dei parametri dinamici {{KEY}} via identificatore univoco.</li>
 *   <li>Eliminazione permanente o rimozione mirata di varianti linguistiche.</li>
 *   <li>Collaudo e invio di email di test tramite integrazione SMTP reattiva.</li>
 * </ul>
 *
 * <p>Interconnessioni logiche:
 * <ul>
 *   <li>Implementato dalla classe controller {@link EmailTemplateControllerImpl}.</li>
 *   <li>Inoltra le richieste operative al service applicativo {@link EmailTemplateService}.</li>
 *   <li>I payload di richiesta e risposta utilizzano i DTO {@link EmailTemplateResponse} e {@link EmailSendTestRequest}.</li>
 *   <li>Tutte le risposte sono incapsulate nel wrapper standard di sistema {@link Response}.</li>
 * </ul>
 */
@Tag(name = OpenAPI.Tag.EMAIL_TEMPLATE, description = OpenAPI.Description.EMAIL_TEMPLATE)
public interface EmailTemplateController {

  /**
   * Recupera l'elenco completo di tutti i template email configurati nel sistema IAM.
   *
   * <p>Gli utenti con ruolo di amministratore globale (Super Admin) visualizzano tutti i template,
   * mentre per i manager delegati l'elenco è limitato ai soli client di competenza autorizzati.
   *
   * @param bearer Token JWT Bearer dell'operatore autorizzato fornito nell'header di autorizzazione.
   * @param request Richiesta HTTP server reattiva per il tracciamento del contesto.
   * @return {@link Mono} contenente la {@link ResponseEntity} con il wrapper {@link Response} e la lista dei template.
   */
  @GetMapping("/email-templates")
  @Operation(
      summary = "Get all email templates",
      description =
          "Retrieves the full list of registered email templates from the database (Scoped by client permissions for delegated operators)",
      tags = OpenAPI.Tag.EMAIL_TEMPLATE,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email templates list retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing, expired or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description =
                "Forbidden - Caller does not possess sufficient privileges to view email templates",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Unexpected failure during template retrieval",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> getAllTemplates(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Registra un nuovo template email con metadati, markup HTML e parametri dinamici {{KEY}}.
   *
   * <p>Verifica rigorosamente l'assenza di collisioni di unicità sulla tripla (Categoria, ClientId, Lingua).
   *
   * @param bearer Token JWT Bearer dell'operatore autorizzato.
   * @param dto DTO contenente identificatore, categoria, client target, locale, nome, oggetto e markup del template.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con stato HTTP 201 CREATED e il template memorizzato.
   */
  @PostMapping("/email-templates")
  @Operation(
      summary = "Create new email template",
      description =
          "Registers and stores a new transactional email template with dynamic parameters, unique identifier and localization",
      tags = OpenAPI.Tag.EMAIL_TEMPLATE,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Email template created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Bad Request - Validation failure or duplicate template conflict for (Category, Client, Locale)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description =
                "Forbidden - Caller lacks permissions to manage email templates for this client",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Database persistence failure",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> createTemplate(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @Valid
          @RequestBody
          @Parameter(
              description =
                  "DTO containing information, parameters and HTML markup of the template to register",
              required = true)
          EmailTemplateResponse dto,
      ServerHttpRequest request);

  /**
   * Aggiorna un template email esistente identificato dal suo UUID o codice univoco applicativo.
   *
   * @param bearer Token JWT Bearer dell'operatore autorizzato.
   * @param identifier Identificatore univoco del template (da path variable REST).
   * @param dto DTO con i campi aggiornati (oggetto, nome, lingua, descrizione, HTML, parametri).
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con stato HTTP 200 OK e il template aggiornato.
   */
  @PutMapping("/email-templates/{identifier}")
  @Operation(
      summary = "Update existing email template",
      description =
          "Modifies the email template matching the specified identifier from path variable, updating HTML, subject, parameters and metadata",
      tags = OpenAPI.Tag.EMAIL_TEMPLATE,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email template updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Bad Request - Invalid payload or duplicate conflict with another existing template",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description =
                "Forbidden - Caller does not possess rights to modify this client's templates",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - No email template found matching the provided identifier",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error - Unexpected error while saving template updates",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> updateTemplate(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable("identifier")
          @Schema(
              description =
                  "Unique application identifier or UUID of the email template to update",
              example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
          String identifier,
      @Valid
          @RequestBody
          @Parameter(
              description =
                  "DTO containing updated template properties, parameters and HTML markup",
              required = true)
          EmailTemplateResponse dto,
      ServerHttpRequest request);

  /**
   * Elimina definitivamente un template email dal database tramite il suo identificatore univoco.
   *
   * @param bearer Token JWT Bearer dell'operatore autorizzato.
   * @param identifier Identificatore univoco del template da eliminare.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con conferma di avvenuta rimozione.
   */
  @DeleteMapping("/email-templates/{identifier}")
  @Operation(
      summary = "Delete email template",
      description =
          "Permanently removes an email template from the database matching the provided unique identifier",
      tags = OpenAPI.Tag.EMAIL_TEMPLATE,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email template successfully deleted",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller lacks permissions to delete email templates",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - No template matches the specified identifier",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Database deletion error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> deleteTemplate(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable("identifier")
          @Schema(
              description =
                  "Unique application identifier or UUID of the email template to delete",
              example = "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95")
          String identifier,
      ServerHttpRequest request);

  /**
   * Esegue l'invio di un'email di collaudo compilata con i parametri dinamici specificati.
   *
   * @param bearer Token JWT Bearer dell'operatore autorizzato.
   * @param testRequest DTO contenente indirizzo destinatario, template ID / identifier, oggetto e mappa parametri.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con il resoconto dell'invio via {@link io.github.giovannilamarmora.accesssphere.email.sender.EmailSender}.
   */
  @PostMapping("/email-templates/send-test")
  @Operation(
      summary = "Send test email",
      description =
          "Compiles the email template with test parameters, renders the final HTML and delivers it to the recipient via SMTP service",
      tags = OpenAPI.Tag.EMAIL_TEMPLATE,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Test email delivered successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Missing recipient or invalid parameter format",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller lacks permissions to test email dispatching",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - Specified template identifier not found in database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error - SMTP server unreachable or transmission failed",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> sendTestEmail(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @Valid
          @RequestBody
          @Parameter(
              description =
                  "DTO containing test email dispatch parameters (recipient, template identifier and dynamic key-value replacements)",
              required = true)
          EmailSendTestRequest testRequest,
      ServerHttpRequest request);
}
