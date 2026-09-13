package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PublicAppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Logged
@RequiredArgsConstructor
public class AppSettingsService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value("${app.version:2.0.0}")
  private String appVersion;

  @Autowired private IAppSettingsDAO appSettingsDAO;
  @Autowired private IClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;

  private final AtomicReference<PublicAppSettingsDTO> publicCache = new AtomicReference<>();

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<PublicAppSettingsDTO> getPublicSettings() {
    PublicAppSettingsDTO cached = publicCache.get();
    if (cached != null) {
      return Mono.just(cached);
    }
    return Mono.fromCallable(
        () -> {
          List<AppSettingsEntity> list = appSettingsDAO.findAll();
          AppSettingsEntity entity = list.isEmpty() ? null : list.get(0);
          PublicAppSettingsDTO dto = AppSettingsMapper.toPublicDTO(entity);
          publicCache.set(dto);
          return dto;
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<AppSettingsDTO> getSettings() {
    return Mono.fromCallable(
        () -> {
          List<AppSettingsEntity> list = appSettingsDAO.findAll();
          AppSettingsEntity entity = list.isEmpty() ? new AppSettingsEntity() : list.get(0);
          return AppSettingsMapper.toDTO(entity);
        });
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<AppSettingsDTO> updateSettings(AppSettingsDTO dto) {
    return Mono.fromCallable(
        () -> {
          List<AppSettingsEntity> list = appSettingsDAO.findAll();
          AppSettingsEntity entity;
          if (list.isEmpty()) {
            entity = AppSettingsMapper.toEntity(dto);
          } else {
            entity = list.get(0);
            AppSettingsMapper.updateEntity(entity, dto);
          }
          AppSettingsEntity saved = appSettingsDAO.save(entity);
          PublicAppSettingsDTO pubDto = AppSettingsMapper.toPublicDTO(saved);
          publicCache.set(pubDto);
          LOG.info("Application settings updated successfully");
          return AppSettingsMapper.toDTO(saved);
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<BackupDataDTO> createBackup(String exportedBy) {
    return Mono.fromCallable(
        () -> {
          LOG.info("Creating complete application backup initiated by {}", exportedBy);
          List<AppSettingsEntity> settingsList = appSettingsDAO.findAll();
          AppSettingsEntity settings = settingsList.isEmpty() ? null : settingsList.get(0);
          List<ClientCredentialEntity> clients = clientDAO.findAll();
          List<UserEntity> users = userDAO.findAll();

          String timestamp =
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

          return BackupDataDTO.builder()
              .version(appVersion)
              .timestamp(timestamp)
              .exportedBy(exportedBy != null ? exportedBy : "SYSTEM_ADMIN")
              .settings(settings)
              .clients(clients)
              .users(users)
              .build();
        });
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<String> restoreBackup(BackupDataDTO backupData) {
    return Mono.fromCallable(
        () -> {
          LOG.info(
              "Restoring application backup from version {} timestamp {}",
              backupData.getVersion(),
              backupData.getTimestamp());

          if (backupData.getSettings() != null) {
            AppSettingsEntity importedSettings = backupData.getSettings();
            List<AppSettingsEntity> current = appSettingsDAO.findAll();
            if (!current.isEmpty()) {
              importedSettings.setId(current.get(0).getId());
            } else {
              importedSettings.setId(null);
            }
            appSettingsDAO.save(importedSettings);
            publicCache.set(AppSettingsMapper.toPublicDTO(importedSettings));
          }

          if (backupData.getClients() != null && !backupData.getClients().isEmpty()) {
            for (ClientCredentialEntity client : backupData.getClients()) {
              ClientCredentialEntity existing = clientDAO.findByClientId(client.getClientId());
              if (existing != null) {
                client.setId(existing.getId());
              } else {
                client.setId(null);
              }
              clientDAO.save(client);
            }
          }

          if (backupData.getUsers() != null && !backupData.getUsers().isEmpty()) {
            for (UserEntity user : backupData.getUsers()) {
              UserEntity existing =
                  userDAO.findUserEntityByUsernameOrEmail(user.getUsername(), user.getEmail());
              if (existing != null) {
                user.setId(existing.getId());
              } else {
                user.setId(null);
              }
              if (user.getStrapiId() == null) {
                user.setStrapiId(0L);
              }
              userDAO.save(user);
            }
          }

          LOG.info("Application restore completed successfully!");
          return "Ripristino completato con successo!";
        });
  }

  public void invalidateCache() {
    publicCache.set(null);
  }
}
