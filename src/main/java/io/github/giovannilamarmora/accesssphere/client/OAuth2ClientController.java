package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.CLIENTS, description = OpenAPI.Description.CLIENTS)
public class OAuth2ClientController {

  @Autowired private OAuth2ClientService oAuth2ClientService;

  @GetMapping("/clients")
  @Operation(
      description = "API to get the Clients (Only Tech User)",
      summary = "Get Clients",
      tags = OpenAPI.Tag.CLIENTS)
  @ApiResponse(
      responseCode = "200",
      description = "Successful operation",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ExceptionResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ExceptionResponse.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getClientCredentials(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      ServerHttpRequest request) {
    return oAuth2ClientService.getClients();
  }

  @PostMapping("/clients")
  @Operation(
      description = "API to create a new Client (Only Tech User)",
      summary = "Create Client",
      tags = OpenAPI.Tag.CLIENTS)
  @ApiResponse(
      responseCode = "201",
      description = "Client successfully created",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> createClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @RequestBody @Valid OAuth2Client oAuth2Client,
      ServerHttpRequest request) {
    oAuth2ClientService.addClientToDatabase(oAuth2Client);
    Response response =
        new Response(
            HttpStatus.CREATED.value(),
            "Client created",
            io.github.giovannilamarmora.utils.context.TraceUtils.getSpanID(),
            oAuth2Client);
    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @Deprecated(since = "2.0.0")
  @PostMapping("/client")
  @Operation(
      description =
          "Deprecated: use POST /v1/clients instead. API to create a new Client (Only Tech User)",
      summary = "Create Client (Deprecated)",
      tags = OpenAPI.Tag.CLIENTS,
      deprecated = true)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully created (Legacy)",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> createClientLegacy(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @RequestBody @Valid OAuth2Client oAuth2Client,
      ServerHttpRequest request) {
    return createClient(bearer, oAuth2Client, request)
        .map(entity -> ResponseEntity.ok(entity.getBody()));
  }

  @PutMapping("/clients/{clientId}")
  @Operation(
      description = "API to update an existing Client by Client ID (Only Tech User)",
      summary = "Update Client by ID",
      tags = OpenAPI.Tag.CLIENTS)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully updated",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateClientById(
      @PathVariable String clientId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @RequestBody @Valid OAuth2Client oAuth2Client,
      ServerHttpRequest request) {
    if (oAuth2Client.getClientId() == null) {
      oAuth2Client.setClientId(clientId);
    }
    oAuth2ClientService.updateClientInDatabase(oAuth2Client);
    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Client updated",
            io.github.giovannilamarmora.utils.context.TraceUtils.getSpanID(),
            oAuth2Client);
    return Mono.just(ResponseEntity.ok(response));
  }

  @PutMapping("/clients")
  @Operation(
      description = "API to update an existing Client (Only Tech User)",
      summary = "Update Client",
      tags = OpenAPI.Tag.CLIENTS)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully updated",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @RequestBody @Valid OAuth2Client oAuth2Client,
      ServerHttpRequest request) {
    oAuth2ClientService.updateClientInDatabase(oAuth2Client);
    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Client updated",
            io.github.giovannilamarmora.utils.context.TraceUtils.getSpanID(),
            oAuth2Client);
    return Mono.just(ResponseEntity.ok(response));
  }

  @Deprecated(since = "2.0.0")
  @PutMapping("/client")
  @Operation(
      description =
          "Deprecated: use PUT /v1/clients or PUT /v1/clients/{clientId} instead. API to update an existing Client (Only Tech User)",
      summary = "Update Client (Deprecated)",
      tags = OpenAPI.Tag.CLIENTS,
      deprecated = true)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully updated",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateClientLegacy(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @RequestBody @Valid OAuth2Client oAuth2Client,
      ServerHttpRequest request) {
    return updateClient(bearer, oAuth2Client, request);
  }

  @DeleteMapping("/clients/{clientId}")
  @Operation(
      description = "API to delete an existing Client (Only Tech User)",
      summary = "Delete Client",
      tags = OpenAPI.Tag.CLIENTS)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully deleted",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteClient(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @PathVariable String clientId,
      ServerHttpRequest request) {
    OAuth2Client cred = new OAuth2Client();
    cred.setClientId(clientId);
    oAuth2ClientService.deleteClientFromDatabase(cred);
    Response response =
        new Response(
            HttpStatus.OK.value(),
            "Client deleted",
            io.github.giovannilamarmora.utils.context.TraceUtils.getSpanID(),
            null);
    return Mono.just(ResponseEntity.ok(response));
  }

  @Deprecated(since = "2.0.0")
  @DeleteMapping("/client/{clientId}")
  @Operation(
      description =
          "Deprecated: use DELETE /v1/clients/{clientId} instead. API to delete an existing Client (Only Tech User)",
      summary = "Delete Client (Deprecated)",
      tags = OpenAPI.Tag.CLIENTS,
      deprecated = true)
  @ApiResponse(
      responseCode = "200",
      description = "Client successfully deleted",
      content = @Content(schema = @Schema(implementation = Response.class)))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> deleteClientLegacy(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      @PathVariable String clientId,
      ServerHttpRequest request) {
    return deleteClient(bearer, clientId, request);
  }
}
