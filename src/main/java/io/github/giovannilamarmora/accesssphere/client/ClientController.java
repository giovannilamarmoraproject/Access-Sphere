package io.github.giovannilamarmora.accesssphere.client;

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
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.CLIENT, description = "API to manage clients")
public class ClientController {

  @Autowired private ClientService clientService;

  @GetMapping("/clients")
  @Operation(
      description = "API to get the Clients (Only Tech User)",
      summary = "Get Clients",
      tags = OpenAPI.Tag.CLIENT)
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
    return clientService.getClients();
  }
}
