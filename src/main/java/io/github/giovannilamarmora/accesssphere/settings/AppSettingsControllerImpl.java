package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsResponse;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataResponse;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupRestoreResponse;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller reattivo REST per l'amministrazione delle impostazioni applicative e delle operazioni di backup/restore.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Implementa il contratto OpenAPI {@link AppSettingsController}.</li>
 *   <li>Delega la validazione dei permessi RBAC e la logica di dominio a {@link AppSettingsService}.</li>
 *   <li>Costruisce risposte HTTP {@link ResponseEntity} incapsulando i dati nei wrapper fortemente tipizzati
 *       {@link AppSettingsResponse}, {@link BackupDataResponse} e {@link BackupRestoreResponse} corredati di Span ID.</li>
 * </ul>
 */
@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.SETTINGS, description = OpenAPI.Description.SETTINGS)
public class AppSettingsControllerImpl implements AppSettingsController {

  private static final Logger LOG = LoggerFilter.getLogger(AppSettingsControllerImpl.class);

  @Autowired private AppSettingsService appSettingsService;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<AppSettingsResponse>> getSettings(
      String bearer, ServerHttpRequest request) {
    return appSettingsService
        .getSettings()
        .map(
            settings ->
                ResponseEntity.ok(
                    new AppSettingsResponse(
                        HttpStatus.OK.value(),
                        "Application Settings retrieved successfully",
                        TraceUtils.getSpanID(),
                        settings)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<AppSettingsResponse>> updateSettings(
      String bearer, AppSettingsDTO appSettingsDTO, ServerHttpRequest request) {
    return appSettingsService
        .updateSettings(appSettingsDTO)
        .map(
            settings ->
                ResponseEntity.ok(
                    new AppSettingsResponse(
                        HttpStatus.OK.value(),
                        "Application Settings updated successfully",
                        TraceUtils.getSpanID(),
                        settings)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<BackupDataResponse>> createBackup(
      String bearer, ServerHttpRequest request) {
    return appSettingsService
        .createBackup("ADMIN_CONSOLE")
        .map(
            backup ->
                ResponseEntity.ok(
                    new BackupDataResponse(
                        HttpStatus.OK.value(),
                        "Application Backup generated successfully",
                        TraceUtils.getSpanID(),
                        backup)));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<BackupRestoreResponse>> restoreBackup(
      String bearer, BackupDataDTO backupData, ServerHttpRequest request) {
    return appSettingsService
        .restoreBackup(backupData)
        .map(
            msg ->
                ResponseEntity.ok(
                    new BackupRestoreResponse(
                        HttpStatus.OK.value(),
                        "Application Backup restored successfully",
                        TraceUtils.getSpanID(),
                        msg)));
  }
}
