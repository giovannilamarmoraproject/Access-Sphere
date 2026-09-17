package io.github.giovannilamarmora.accesssphere.oAuth.oidc;

import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI per l'endpoint OIDC UserInfo conforme allo standard OpenID Connect Core 1.0
 * (Section 5.3).
 *
 * <p>Supporta l'endpoint principale {@code /userInfo} e gli alias standard OIDC {@code /userinfo} e
 * {@code /oauth2/userinfo}.
 */
public interface OIDCController {

  @GetMapping(value = {"/userinfo", "/oauth2/userinfo"})
  @Operation(
      description =
          "Obtaining the Info of the current User according to OIDC Core 1.0 specifications",
      summary = "User Info",
      tags = OpenAPI.Tag.OIDC,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User info retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = JWTData.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> userInfo(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @GetMapping(value = "/userInfo")
  @Operation(
      description =
          "Obtaining the Info of the current User according to OIDC Core 1.0 specifications",
      summary = "User Info",
      tags = OpenAPI.Tag.OIDC,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User info retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = JWTData.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> userInfoDeprecated(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);
}
