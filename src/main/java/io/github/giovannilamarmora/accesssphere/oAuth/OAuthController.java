package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface OAuthController {

  @GetMapping(value = "/authorize")
  @Operation(
      description = "API to start OAuth 2.0 authorization",
      summary = "Start OAuth 2.0 Authorization",
      tags = OpenAPI.Tag.OAUTH)
  @ApiResponse(
      responseCode = "200",
      description = "Successful operation",
      content =
          @Content(
              schema = @Schema(implementation = String.class),
              examples = @ExampleObject(value = "https://google.auth/callback")))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Input",
      content =
          @Content(
              schema = @Schema(implementation = ExceptionResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content =
          @Content(
              schema = @Schema(implementation = ExceptionResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<?>> authorize(
      @RequestParam(value = "response_type")
          @Schema(
              description = OpenAPI.Params.Description.RESPONSE_TYPE,
              example = OpenAPI.Params.Example.RESPONSE_TYPE)
          String responseType,
      @RequestParam(value = "access_type")
          @Schema(
              description = OpenAPI.Params.Description.ACCESS_TYPE,
              example = OpenAPI.Params.Example.ACCESS_TYPE)
          String accessType,
      @RequestParam(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "redirect_uri")
          @Schema(
              description = OpenAPI.Params.Description.REDIRECT_URI,
              example = OpenAPI.Params.Example.REDIRECT_URI)
          String redirectUri,
      @RequestParam("scope")
          @Schema(
              description = OpenAPI.Params.Description.SCOPE,
              example = OpenAPI.Params.Example.SCOPE)
          String scope,
      @RequestParam(value = "registration_token", required = false)
          @Schema(
              description = OpenAPI.Params.Description.REGISTRATION_TOKEN,
              example = OpenAPI.Params.Example.REGISTRATION_TOKEN)
          String registration_token,
      @RequestParam(value = "state", required = false)
          @Schema(
              description = OpenAPI.Params.Description.STATE,
              example = OpenAPI.Params.Example.STATE)
          String state);

  @GetMapping(value = "/login/{client_id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      description = "API to perform OAuth 2.0 login",
      summary = "Perform OAuth 2.0 Login",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<?>> login(
      @PathVariable(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "scope", required = false)
          @Schema(
              description = OpenAPI.Params.Description.SCOPE,
              example = OpenAPI.Params.Example.SCOPE)
          String scope,
      @RequestParam(value = "code", required = false)
          @Schema(
              description = OpenAPI.Params.Description.CODE,
              example = OpenAPI.Params.Example.CODE)
          String code,
      @RequestParam(value = "prompt", required = false)
          @Schema(
              description = OpenAPI.Params.Description.PROMPT,
              example = OpenAPI.Params.Example.PROMPT)
          String prompt,
      @RequestParam(value = "include_user_info", required = false, defaultValue = "true")
          @Schema(
              description = OpenAPI.Params.Description.INCLUDE_USER_INFO,
              example = OpenAPI.Params.Example.INCLUDE_USER_INFO)
          boolean includeUserInfo,
      @RequestParam(value = "include_user_data", required = false, defaultValue = "true")
          @Schema(
              description = OpenAPI.Params.Description.INCLUDE_USER_DATA,
              example = OpenAPI.Params.Example.INCLUDE_USER_DATA)
          boolean includeUserData,
      ServerHttpRequest request);

  @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      description = "API to perform OAuth 2.0 login",
      summary = "Perform OAuth 2.0 Login",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<?>> token(
      @RequestParam(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "grant_type")
          @Schema(
              description = OpenAPI.Params.Description.GRANT_TYPE,
              example = OpenAPI.Params.Example.GRANT_TYPE)
          String grant_type,
      @RequestParam(value = "scope", required = false)
          @Schema(
              description = OpenAPI.Params.Description.SCOPE,
              example = OpenAPI.Params.Example.SCOPE)
          String scope,
      @RequestParam(value = "code", required = false)
          @Schema(
              description = OpenAPI.Params.Description.CODE,
              example = OpenAPI.Params.Example.CODE)
          String code,
      @RequestParam(value = "redirect_uri", required = false)
          @Schema(
              description = OpenAPI.Params.Description.REDIRECT_URI,
              example = OpenAPI.Params.Example.REDIRECT_URI)
          String redirectUri,
      @RequestParam(value = "prompt", required = false)
          @Schema(
              description = OpenAPI.Params.Description.PROMPT,
              example = OpenAPI.Params.Example.PROMPT)
          String prompt,
      @RequestParam(value = "include_user_info", required = false, defaultValue = "false")
          @Schema(
              description = OpenAPI.Params.Description.INCLUDE_USER_INFO,
              example = OpenAPI.Params.Example.INCLUDE_USER_INFO)
          boolean includeUserInfo,
      @RequestParam(value = "include_user_data", required = false, defaultValue = "false")
          @Schema(
              description = OpenAPI.Params.Description.INCLUDE_USER_DATA,
              example = OpenAPI.Params.Example.INCLUDE_USER_DATA)
          boolean includeUserData,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BASIC,
              example = OpenAPI.Params.Example.BASIC)
          String auth,
      ServerHttpRequest request);
}
