package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface UserController {

  @GetMapping("/userInfo")
  @Operation(
      description = "Obtaining the Info of the current User",
      summary = "User Info",
      tags = OpenAPI.Tag.USERS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User info retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
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

  @GetMapping("/users/profile")
  @Operation(
      description = "Obtaining the profile of the current User",
      summary = "User Profile",
      tags = OpenAPI.Tag.USERS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
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
  Mono<ResponseEntity<Response>> profile(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @PostMapping("/users/register")
  @Operation(
      description = "Register a new user",
      summary = "User Registration",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "@BadRequest.json"))),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict, user already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> registerUser(
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestParam(value = "client_id")
          @Schema(
              description = OpenAPI.Params.Description.CLIENT_ID,
              example = OpenAPI.Params.Example.CLIENT_ID)
          String clientId,
      @RequestParam(value = "registration_token")
          @Schema(
              description = OpenAPI.Params.Description.REGISTRATION_TOKEN,
              example = OpenAPI.Params.Example.REGISTRATION_TOKEN)
          String registration_token);

  @PutMapping("/users/update")
  @Operation(
      description = "Update an existing user",
      summary = "User Update",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "@BadRequest.json")))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> updateUser(
      @RequestBody @Valid @NotNull(message = "User cannot be null") User user,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  @PostMapping("/users/change/password/request")
  @Operation(
      description = "Send Email to change the user password",
      summary = "Change Password Step 1",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Email successfully send!",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "@BadRequest.json")))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePasswordRequest(
      @RequestBody @Valid ChangePassword changePassword,
      @RequestParam(value = "locale", required = false, defaultValue = "en-GB") String locale);

  @PostMapping("/users/change/password")
  @Operation(
      description = "Change the user password",
      summary = "Change Password Step 2",
      tags = OpenAPI.Tag.USERS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Email successfully send!",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "@BadRequest.json")))
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  Mono<ResponseEntity<Response>> changePassword(@RequestBody @Valid ChangePassword changePassword);
}
