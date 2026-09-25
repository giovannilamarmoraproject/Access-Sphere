package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientDeleteResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientListResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientResponse;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI per la gestione amministrativa dei Client OAuth 2.0.
 *
 * <p>Definisce le operazioni REST esposte da Access-Sphere per la registrazione, consultazione,
 * aggiornamento ed eliminazione dei Client OAuth2. Tutti gli endpoint richiedono l'autenticazione
 * dell'operatore tecnico/amministratore mediante Bearer token fornito nell'header HTTP
 * Authorization.
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Consumato dal controller di implementazione {@link OAuth2ClientControllerImpl}.
 *   <li>Inoltra la business logic a {@link OAuth2ClientService}.
 *   <li>Utilizza gli involucri di risposta tipizzati {@link OAuth2ClientResponse}, {@link
 *       OAuth2ClientListResponse} e {@link OAuth2ClientDeleteResponse} per garantire una resa
 *       dettagliata e completa dei modelli e degli esempi in Swagger UI.
 * </ul>
 */
@Tag(name = OpenAPI.Tag.CLIENTS, description = OpenAPI.Description.CLIENTS)
public interface OAuth2ClientController {

  /**
   * Recupera l'elenco completo di tutti i Client OAuth 2.0 registrati a catalogo.
   *
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link Response} e lista di {@link
   *     OAuth2Client}.
   */
  @GetMapping("/clients")
  @Operation(
      summary = "Get Clients",
      description =
          "API to get all registered OAuth 2.0 Clients (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of OAuth 2.0 clients retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientListResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No clients found in Database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientListResponse>> getOAuth2Clients(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Recupera i dettagli e le credenziali di un singolo Client OAuth 2.0 specificato tramite Client
   * ID.
   *
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param clientId Identificativo univoco del client desiderato.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link OAuth2ClientResponse}.
   */
  @GetMapping("/clients/{clientId}")
  @Operation(
      summary = "Get Client by ID",
      description =
          "API to retrieve details of a specific OAuth 2.0 Client by Client ID (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OAuth 2.0 client retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid client ID provided",
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
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Client not found in Database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientResponse>> getClientById(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "clientId")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      ServerHttpRequest request);

  /**
   * Registra e crea un nuovo Client OAuth 2.0 nel catalogo del sistema IAM.
   *
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param oAuth2Client DTO con la configurazione e le credenziali del nuovo client.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con stato 201 CREATED e {@link
   *     OAuth2ClientResponse}.
   */
  @PostMapping("/clients")
  @Operation(
      summary = "Create Client",
      description = "API to create a new OAuth 2.0 Client (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Client successfully created",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid client configuration or validation failure",
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
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientResponse>> createClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "Client payload cannot be null")
          OAuth2Client oAuth2Client,
      ServerHttpRequest request);

  /**
   * Aggiorna un Client OAuth 2.0 esistente identificato tramite il Client ID passato come path
   * variable.
   *
   * @param clientId Identificativo univoco del client da aggiornare.
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param oAuth2Client DTO contenente le nuove configurazioni del client.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link OAuth2ClientResponse}.
   */
  @PutMapping("/clients/{clientId}")
  @Operation(
      summary = "Update Client by ID",
      description =
          "API to update an existing OAuth 2.0 Client identified by path variable (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client successfully updated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid client data or payload",
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
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Client not found in Database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientResponse>> updateClientById(
      @PathVariable(value = "clientId")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "Client payload cannot be null")
          OAuth2Client oAuth2Client,
      ServerHttpRequest request);

  /**
   * Aggiorna un Client OAuth 2.0 esistente utilizzando il Client ID specificato all'interno del
   * body.
   *
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param oAuth2Client DTO contenente il client_id e le nuove configurazioni.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link OAuth2ClientResponse}.
   */
  @PutMapping("/clients")
  @Operation(
      summary = "Update Client",
      description =
          "API to update an existing OAuth 2.0 Client using the clientId in the request body (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client successfully updated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid client data or payload",
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
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Client not found in Database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientResponse>> updateClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid @NotNull(message = "Client payload cannot be null")
          OAuth2Client oAuth2Client,
      ServerHttpRequest request);

  /**
   * Elimina un Client OAuth 2.0 specificato tramite Client ID dal database del sistema IAM.
   *
   * @param bearer Token JWT Bearer dell'operatore tecnico autorizzato.
   * @param clientId Identificativo univoco del client da eliminare.
   * @param request Richiesta HTTP server reattiva.
   * @return {@link Mono} contenente {@link ResponseEntity} con {@link OAuth2ClientDeleteResponse}.
   */
  @DeleteMapping("/clients/{clientId}")
  @Operation(
      summary = "Delete Client",
      description =
          "API to delete an existing OAuth 2.0 Client (Reserved for Technical/Admin Operators)",
      tags = OpenAPI.Tag.CLIENTS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client successfully deleted",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2ClientDeleteResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid client ID",
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
            description = "Forbidden - Caller does not have sufficient technical permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Client not found in Database",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  Mono<ResponseEntity<OAuth2ClientDeleteResponse>> deleteClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "clientId")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      ServerHttpRequest request);
}
