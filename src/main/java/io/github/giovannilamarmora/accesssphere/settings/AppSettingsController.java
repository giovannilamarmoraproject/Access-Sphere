package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PublicAppSettingsDTO;
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

@Logged
@RestController
@RequestMapping("/v1/app/settings")
@Tag(name = "Application Settings", description = "API to manage application settings, branding, themes, backup and restore")
public class AppSettingsController {

  @Autowired private AppSettingsService appSettingsService;

  @GetMapping("/public")
  @Operation(summary = "Get Public Application Settings", description = "Retrieve public branding, themes, and login settings")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getPublicSettings(ServerHttpRequest request) {
    return appSettingsService
        .getPublicSettings()
        .map(
            settings ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Public Application Settings",
                        TraceUtils.getSpanID(),
                        settings)));
  }

  @GetMapping
  @Operation(summary = "Get Application Settings", description = "Retrieve full application settings (Admin / Tech User)")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> getSettings(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
      ServerHttpRequest request) {
    return appSettingsService
        .getSettings()
        .map(
            settings ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Application Settings",
                        TraceUtils.getSpanID(),
                        settings)));
  }

  @PutMapping
  @Operation(summary = "Update Application Settings", description = "Update full application settings (Admin / Tech User)")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateSettings(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
      @RequestBody @Valid AppSettingsDTO appSettingsDTO,
      ServerHttpRequest request) {
    return appSettingsService
        .updateSettings(appSettingsDTO)
        .map(
            settings ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Application Settings updated successfully",
                        TraceUtils.getSpanID(),
                        settings)));
  }

  @GetMapping("/backup")
  @Operation(summary = "Create Application Backup", description = "Exports a complete snapshot of all settings, clients, and users")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> createBackup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
      ServerHttpRequest request) {
    return appSettingsService
        .createBackup("ADMIN_CONSOLE")
        .map(
            backup ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        "Application Backup generated successfully",
                        TraceUtils.getSpanID(),
                        backup)));
  }

  @PostMapping("/restore")
  @Operation(summary = "Restore Application Backup", description = "Imports a complete snapshot of settings, clients, and users")
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> restoreBackup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
      @RequestBody @Valid BackupDataDTO backupData,
      ServerHttpRequest request) {
    return appSettingsService
        .restoreBackup(backupData)
        .map(
            msg ->
                ResponseEntity.ok(
                    new Response(
                        HttpStatus.OK.value(),
                        msg,
                        TraceUtils.getSpanID(),
                        null)));
  }
}
