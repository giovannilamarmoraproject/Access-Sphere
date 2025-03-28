package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
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
@CrossOrigin("*")
@Tag(name = OpenAPI.Tag.CLIENT, description = "API to manage clients")
public class ClientController {

  @Autowired private ClientService clientService;

  // @Autowired private TechUserService techUserService;

  @GetMapping("/clients")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getClientCredentials(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      ServerHttpRequest request) {
    return clientService.getClients();
  }

  /*@GetMapping("/clients/{client_id}")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getClientCredential(
      @PathVariable(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) @Valid String bearer,
      ServerHttpRequest request) {
    return clientService
        .getClientCredentialByClientID(clientId)
        .map(
            clientCredential -> {
              techUserService.validateTechClient(List.of(clientCredential));
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Client credential data for " + clientCredential.getClientId(),
                      TraceUtils.getSpanID(),
                      clientCredential);
              return ResponseEntity.ok(response);
            });
  }*/
}
