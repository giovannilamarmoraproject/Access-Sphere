package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsResponse;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataResponse;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupRestoreResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

/**
 * Contratto OpenAPI per la gestione delle impostazioni applicative e delle procedure di backup/restore.
 *
 * <p>Definisce le operazioni REST esposte da Access-Sphere per la consultazione del branding,
 * l'aggiornamento dei temi e delle policy legali, nonché l'esportazione e l'importazione di snapshot
 * completi di sistema.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Consumato dal controller di implementazione {@link AppSettingsControllerImpl}.</li>
 *   <li>Inoltra la business logic a {@link AppSettingsService}.</li>
 *   <li>Utilizza gli involucri di risposta tipizzati {@link AppSettingsResponse}, {@link BackupDataResponse}
 *       e {@link BackupRestoreResponse} per una documentazione OpenAPI dettagliata e interattiva.</li>
 * </ul>
 */
@Tag(name = OpenAPI.Tag.SETTINGS, description = OpenAPI.Description.SETTINGS)
public interface AppSettingsController {

  /**
   * Recupera le impostazioni applicative correnti (branding, temi, login mode, policy legali).
   *
   * @param bearer Token Bearer opzionale dell'operatore autenticato.
   * @param request Richiesta HTTP server.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link AppSettingsResponse}.
   */
  @GetMapping("/app/settings")
  @Operation(
      summary = "Get Application Settings",
      description = "Retrieve full application settings including branding, themes, login modes, and legal policies",
      tags = OpenAPI.Tag.SETTINGS)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application settings retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AppSettingsResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller does not have sufficient permissions",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  Mono<ResponseEntity<AppSettingsResponse>> getSettings(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Aggiorna le impostazioni applicative globali (richiede permesso 'settings:write').
   *
   * @param bearer Token Bearer di autenticazione dell'operatore.
   * @param appSettingsDTO Payload contenente le nuove configurazioni.
   * @param request Richiesta HTTP server.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link AppSettingsResponse}.
   */
  @PutMapping("/app/settings")
  @Operation(
      summary = "Update Application Settings",
      description = "Update full application settings (Requires SETTINGS_WRITE permission)",
      tags = OpenAPI.Tag.SETTINGS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application settings updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AppSettingsResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid payload structure or missing required fields",
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
            description = "Forbidden - Caller does not have SETTINGS_WRITE permission",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  Mono<ResponseEntity<AppSettingsResponse>> updateSettings(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid AppSettingsDTO appSettingsDTO,
      ServerHttpRequest request);

  /**
   * Genera ed esporta uno snapshot completo di backup dell'applicazione (richiede permesso 'backup:manage').
   *
   * @param bearer Token Bearer di autenticazione.
   * @param request Richiesta HTTP server.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link BackupDataResponse}.
   */
  @GetMapping("/app/settings/backup")
  @Operation(
      summary = "Create Application Backup",
      description = "Exports a complete snapshot of all settings, clients, users, roles, and user-client mappings (Requires BACKUP_MANAGE permission)",
      tags = OpenAPI.Tag.SETTINGS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application backup generated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BackupDataResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid Bearer token",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Caller does not have BACKUP_MANAGE permission",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  Mono<ResponseEntity<BackupDataResponse>> createBackup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      ServerHttpRequest request);

  /**
   * Ripristina uno snapshot di backup dell'applicazione (richiede permesso 'backup:manage').
   *
   * @param bearer Token Bearer di autenticazione.
   * @param backupData Payload contenente lo snapshot completo da importare.
   * @param request Richiesta HTTP server.
   * @return {@link Mono} con {@link ResponseEntity} contenente {@link BackupRestoreResponse}.
   */
  @PostMapping("/app/settings/restore")
  @Operation(
      summary = "Restore Application Backup",
      description = "Imports a complete snapshot of settings, clients, users, and roles (Requires BACKUP_MANAGE permission)",
      tags = OpenAPI.Tag.SETTINGS,
      security = @SecurityRequirement(name = HttpHeaders.AUTHORIZATION))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application backup restored successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BackupRestoreResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid backup structure or missing version",
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
            description = "Forbidden - Caller does not have BACKUP_MANAGE permission",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class))),
      })
  Mono<ResponseEntity<BackupRestoreResponse>> restoreBackup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer,
      @RequestBody @Valid BackupDataDTO backupData,
      ServerHttpRequest request);
}
