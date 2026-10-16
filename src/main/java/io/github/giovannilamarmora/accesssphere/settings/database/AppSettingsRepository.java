package io.github.giovannilamarmora.accesssphere.settings.database;

import io.github.giovannilamarmora.accesssphere.settings.AppSettingsMapper;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.accesssphere.settings.model.AppSettings;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository di dominio per l'aggregato delle impostazioni applicative (AppSettings).
 * Incapsula l'accesso a {@link IAppSettingsDAO} ed espone esclusivamente il modello di dominio {@link AppSettings}.
 */
@Repository
public class AppSettingsRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IAppSettingsDAO appSettingsDAO;

  /**
   * Recupera tutti i record di configurazione mappati nel modello di dominio {@link AppSettings}.
   *
   * @return Optional con la lista dei modelli di dominio delle impostazioni applicative.
   */
  public Optional<List<AppSettings>> findAll() {
    LOG.debug("⚙️ Retrieving all Application Settings records from database");
    List<AppSettingsEntity> entities = appSettingsDAO.findAll();
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Application Settings not found in Database");
      return Optional.empty();
    }
    LOG.debug("⚖️ Found {} Application Settings in Database", entities.size());
    return Optional.of(AppSettingsMapper.toModelList(entities));
  }

  /**
   * Recupera l'impostazione applicativa primaria di sistema mappata nel modello di dominio.
   *
   * @return Optional contenente il modello {@link AppSettings}.
   */
  public Optional<AppSettings> findPrimary() {
    LOG.debug("⚙️ Retrieving primary Application Settings from database");
    List<AppSettingsEntity> entities = appSettingsDAO.findAll();
    if (ObjectToolkit.isNullOrEmpty(entities)) {
      LOG.warn("❌ Application Settings not found in Database");
      return Optional.empty();
    }
    return Optional.ofNullable(AppSettingsMapper.toModel(entities.getFirst()));
  }

  /**
   * Recupera un record di impostazioni per ID mappato nel modello di dominio.
   *
   * @param id identificativo univoco.
   * @return Optional contenente il modello {@link AppSettings}.
   */
  public Optional<AppSettings> findById(Long id) {
    LOG.debug("⚙️ Retrieving Application Settings record for ID: {}", id);
    Optional<AppSettingsEntity> entityOpt = appSettingsDAO.findById(id);
    if (entityOpt.isEmpty()) {
      LOG.warn("❌ Application Settings with ID: {} not found in Database", id);
      return Optional.empty();
    }
    return Optional.ofNullable(AppSettingsMapper.toModel(entityOpt.get()));
  }

  /**
   * Salva o aggiorna un modello di dominio {@link AppSettings} sul database.
   *
   * @param appSettings modello di dominio da persistere.
   * @return modello di dominio {@link AppSettings} aggiornato dopo la persistenza.
   */
  public AppSettings save(AppSettings appSettings) {
    LOG.debug("💾 Saving Application Settings domain model");
    AppSettingsEntity entity;
    if (appSettings.getId() != null) {
      Optional<AppSettingsEntity> existingOpt = appSettingsDAO.findById(appSettings.getId());
      if (existingOpt.isPresent()) {
        entity = existingOpt.get();
        AppSettingsMapper.updateEntity(entity, appSettings);
      } else {
        entity = AppSettingsMapper.toEntity(appSettings);
      }
    } else {
      entity = AppSettingsMapper.toEntity(appSettings);
    }

    AppSettingsEntity saved = appSettingsDAO.save(entity);
    appSettingsDAO.flush();
    LOG.debug("⚖️ Saved Application Settings in Database with ID: {}", saved.getId());
    return AppSettingsMapper.toModel(saved);
  }

  /**
   * Esegue il flush immediato delle modifiche sulla sessione di persistenza.
   */
  public void flush() {
    appSettingsDAO.flush();
  }
}
