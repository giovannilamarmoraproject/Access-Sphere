package io.github.giovannilamarmora.accesssphere.settings.database;

import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository di persistenza per l'aggregato delle impostazioni applicative (AppSettings).
 * Incapsula l'accesso a {@link IAppSettingsDAO} e centralizza il logging e la gestione del ciclo di vita.
 */
@Repository
public class AppSettingsRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IAppSettingsDAO appSettingsDAO;

  public List<AppSettingsEntity> findAll() {
    LOG.debug("⚙️ Retrieving all Application Settings records from database");
    return appSettingsDAO.findAll();
  }

  public Optional<AppSettingsEntity> findById(Long id) {
    LOG.debug("⚙️ Retrieving Application Settings record for ID: {}", id);
    return appSettingsDAO.findById(id);
  }

  public AppSettingsEntity save(AppSettingsEntity entity) {
    LOG.debug("💾 Saving Application Settings record");
    return appSettingsDAO.save(entity);
  }

  public void flush() {
    appSettingsDAO.flush();
  }
}
