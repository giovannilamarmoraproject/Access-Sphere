package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.settings.database.AppSettingsRepository;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.accesssphere.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Application Service per la gestione centralizzata delle impostazioni applicative e delle operazioni di backup/restore.
 * <p>
 * Responsabilità:
 * <ul>
 *   <li>Funge da Gatekeeper di sicurezza verificando i permessi RBAC (settings:read, settings:write, backup:manage) tramite {@link PermissionRoleManager}.</li>
 *   <li>Mantiene una cache atomica in memoria per le letture frequenti di branding e layout.</li>
 *   <li>Delega la persistenza e il tracciamento a {@link AppSettingsRepository}.</li>
 *   <li>Coordina la generazione e il ripristino atomico degli snapshot completi del sistema.</li>
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class AppSettingsService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value("${app.version:2.0.0}")
  private String appVersion;

  @Autowired private AppSettingsRepository appSettingsRepository;
  @Autowired private IOAuth2ClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private PermissionRoleManager permissionRoleManager;

  private final AtomicReference<AppSettingsDTO> publicCache = new AtomicReference<>();

  /**
   * Recupera le impostazioni applicative correnti dalla cache o da database.
   * Utilizzabile sia da contesti autenticati (backoffice) sia pubblici (login/branding).
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<AppSettingsDTO> getSettings() {
    AppSettingsDTO cached = publicCache.get();
    if (cached != null) {
      return Mono.just(cached);
    }
    return Mono.fromCallable(
        () -> {
          LOG.debug("⚙️ Loading application settings from repository");
          List<AppSettingsEntity> list = appSettingsRepository.findAll();
          AppSettingsEntity entity;
          if (list.isEmpty()) {
            AppSettingsEntity defaultEntity = new AppSettingsEntity();
            entity = appSettingsRepository.save(defaultEntity);
            appSettingsRepository.flush();
          } else {
            entity = list.getFirst();
          }
          AppSettingsDTO dto = AppSettingsMapper.toDTO(entity);
          publicCache.set(dto);
          return dto;
        });
  }

  /**
   * Aggiorna le impostazioni applicative verificando il permesso 'settings:write'.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<AppSettingsDTO> updateSettings(AppSettingsDTO dto) {
    permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.SETTINGS_WRITE);
    AppSettingsValidator.validateSettings(dto);

    return Mono.fromCallable(
        () -> {
          LOG.info("⚙️ Updating application settings");
          List<AppSettingsEntity> list = appSettingsRepository.findAll();
          AppSettingsEntity entity;
          if (list.isEmpty()) {
            entity = AppSettingsMapper.toEntity(dto);
          } else {
            entity = list.getFirst();
            AppSettingsMapper.updateEntity(entity, dto);
          }
          AppSettingsEntity saved = appSettingsRepository.save(entity);
          appSettingsRepository.flush();
          AppSettingsDTO savedDto = AppSettingsMapper.toDTO(saved);
          publicCache.set(savedDto);
          LOG.info(
              "✅ Application settings updated successfully: appName='{}', theme='{}', lang='{}'",
              saved.getAppName(),
              saved.getActiveTheme(),
              saved.getDefaultLanguage());
          return savedDto;
        });
  }

  /**
   * Genera uno snapshot di backup completo verificando il permesso 'backup:manage'.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<BackupDataDTO> createBackup(String exportedBy) {
    permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.BACKUP_MANAGE);

    return Mono.fromCallable(
        () -> {
          LOG.info("📦 Creating complete application backup initiated by {}", exportedBy);
          List<AppSettingsEntity> settingsList = appSettingsRepository.findAll();
          AppSettingsEntity settings = settingsList.isEmpty() ? null : settingsList.getFirst();
          List<OAuth2ClientEntity> clients = clientDAO.findAll();
          List<UserEntity> users = userDAO.findAll();
          List<RoleDefinitionEntity> roles = roleDefinitionDAO.findAll();
          List<UserClientRoleEntity> userClientRoles = userClientRoleDAO.findAll();

          String timestamp =
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

          return BackupDataDTO.builder()
              .version(appVersion)
              .timestamp(timestamp)
              .exportedBy(exportedBy != null ? exportedBy : "SYSTEM_ADMIN")
              .settings(settings)
              .clients(clients)
              .users(users)
              .roles(roles)
              .userClientRoles(userClientRoles)
              .build();
        });
  }

  /**
   * Esegue il ripristino di uno snapshot di backup verificando il permesso 'backup:manage'.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<String> restoreBackup(BackupDataDTO backupData) {
    permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.BACKUP_MANAGE);
    AppSettingsValidator.validateBackupData(backupData);

    return Mono.fromCallable(
        () -> {
          LOG.info(
              "🔄 Restoring application backup from version {} timestamp {}",
              backupData.getVersion(),
              backupData.getTimestamp());

          if (backupData.getSettings() != null) {
            AppSettingsEntity importedSettings = backupData.getSettings();
            List<AppSettingsEntity> current = appSettingsRepository.findAll();
            if (!current.isEmpty()) {
              importedSettings.setId(current.getFirst().getId());
            } else {
              importedSettings.setId(null);
            }
            importedSettings.syncCategoryMapsBeforeSave();
            AppSettingsEntity saved = appSettingsRepository.save(importedSettings);
            appSettingsRepository.flush();
            publicCache.set(AppSettingsMapper.toDTO(saved));
          }

          if (backupData.getClients() != null && !backupData.getClients().isEmpty()) {
            for (OAuth2ClientEntity client : backupData.getClients()) {
              OAuth2ClientEntity existing = clientDAO.findByClientId(client.getClientId());
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
              userDAO.save(user);
            }
          }

          if (backupData.getRoles() != null && !backupData.getRoles().isEmpty()) {
            for (RoleDefinitionEntity role : backupData.getRoles()) {
              Optional<RoleDefinitionEntity> existing =
                  roleDefinitionDAO.findByClientIdAndRole(role.getClientId(), role.getRole());
              if (existing.isPresent()) {
                role.setId(existing.get().getId());
              } else {
                role.setId(null);
              }
              roleDefinitionDAO.save(role);
            }
          }

          if (backupData.getUserClientRoles() != null
              && !backupData.getUserClientRoles().isEmpty()) {
            for (UserClientRoleEntity ucr : backupData.getUserClientRoles()) {
              boolean exists =
                  userClientRoleDAO.existsByUserIdentifierAndClientIdAndRole(
                       ucr.getUserIdentifier(), ucr.getClientId(), ucr.getRole());
              if (!exists) {
                ucr.setId(null);
                userClientRoleDAO.save(ucr);
              }
            }
          }

          LOG.info("🎉 Application restore completed successfully!");
          return "Ripristino completato con successo!";
        });
  }

  public void invalidateCache() {
    publicCache.set(null);
  }
}
