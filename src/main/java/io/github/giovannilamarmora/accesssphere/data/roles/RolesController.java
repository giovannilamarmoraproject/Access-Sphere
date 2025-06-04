package io.github.giovannilamarmora.accesssphere.data.roles;

import io.github.giovannilamarmora.accesssphere.data.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface RolesController {

  @PutMapping("/users/{identifier}/roles")
  @Operation(
      description = "Change user roles, only Tech User",
      summary = "Change user roles",
      tags = OpenAPI.Tag.ROLES)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User unlocked successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
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
                    schema = @Schema(implementation = ExceptionResponse.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changeRoles(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @PathVariable(value = "identifier")
          @Schema(
              description = OpenAPI.Params.Description.IDENTIFIER,
              example = OpenAPI.Params.Example.IDENTIFIER)
          String identifier,
      @RequestBody @Valid @NotNull(message = "Roles are required") UserRoles roles);
}
