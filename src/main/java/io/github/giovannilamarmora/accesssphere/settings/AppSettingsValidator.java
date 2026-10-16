package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validatore di dominio per i payload di configurazione e backup applicativo.
 */
@Component
public class AppSettingsValidator {

  private static final Logger LOG = LoggerFilter.getLogger(AppSettingsValidator.class);

  public static void validateSettings(AppSettingsDTO dto) {
    if (ObjectToolkit.isNullOrEmpty(dto)) {
      LOG.error("❌ Application Settings payload cannot be null or empty");
      throw new UtilsException(ExceptionMap.ERR_OAUTH_400, "Application settings payload cannot be null or empty");
    }
  }

  public static void validateBackupData(BackupDataDTO backupData) {
    if (ObjectToolkit.isNullOrEmpty(backupData)) {
      LOG.error("❌ Backup data payload cannot be null or empty");
      throw new UtilsException(ExceptionMap.ERR_OAUTH_400, "Backup data payload cannot be null or empty");
    }
    if (ObjectToolkit.isNullOrEmpty(backupData.getVersion())) {
      LOG.error("❌ Backup data version is required");
      throw new UtilsException(ExceptionMap.ERR_OAUTH_400, "Backup data version is required");
    }
  }
}
