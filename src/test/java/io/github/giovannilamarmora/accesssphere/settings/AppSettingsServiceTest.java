package io.github.giovannilamarmora.accesssphere.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BackupDataDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PublicAppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AppSettingsServiceTest {

  @Mock private IAppSettingsDAO appSettingsDAO;
  @Mock private IClientDAO clientDAO;
  @Mock private IUserDAO userDAO;

  @InjectMocks private AppSettingsService appSettingsService;

  private AppSettingsEntity sampleEntity;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(appSettingsService, "appVersion", "2.0.0");
    appSettingsService.invalidateCache();

    sampleEntity = new AppSettingsEntity();
    sampleEntity.setId(1L);
    sampleEntity.setAppName("Access Sphere Test");
    sampleEntity.setAppTagline("Test Tagline");
    sampleEntity.setActiveTheme("cosmic-purple");
    sampleEntity.setDefaultLanguage("it");
    sampleEntity.setLoginMode("SHOWCASE");
    sampleEntity.setFooterCopyright("© 2026 Test");
  }

  @Test
  @DisplayName("getPublicSettings returns public DTO and caches result")
  void testGetPublicSettingsCaching() {
    when(appSettingsDAO.findAll()).thenReturn(List.of(sampleEntity));

    StepVerifier.create(appSettingsService.getPublicSettings())
        .assertNext(
            dto -> {
              assertNotNull(dto);
              assertEquals("Access Sphere Test", dto.getAppName());
              assertEquals("cosmic-purple", dto.getActiveTheme());
              assertEquals("it", dto.getDefaultLanguage());
              assertEquals("SHOWCASE", dto.getLoginMode());
            })
        .verifyComplete();

    StepVerifier.create(appSettingsService.getPublicSettings())
        .assertNext(
            dto -> {
              assertEquals("Access Sphere Test", dto.getAppName());
            })
        .verifyComplete();

    verify(appSettingsDAO, times(1)).findAll();
  }

  @Test
  @DisplayName("getSettings returns full DTO")
  void testGetSettings() {
    when(appSettingsDAO.findAll()).thenReturn(List.of(sampleEntity));

    StepVerifier.create(appSettingsService.getSettings())
        .assertNext(
            dto -> {
              assertNotNull(dto);
              assertEquals(1L, dto.getId());
              assertEquals("Access Sphere Test", dto.getAppName());
              assertEquals("© 2026 Test", dto.getFooterCopyright());
            })
        .verifyComplete();

    verify(appSettingsDAO, times(1)).findAll();
  }

  @Test
  @DisplayName("updateSettings modifies existing entity, saves, and refreshes cache")
  void testUpdateSettings() {
    when(appSettingsDAO.findAll()).thenReturn(List.of(sampleEntity));
    when(appSettingsDAO.save(any(AppSettingsEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppSettingsDTO updateDTO = new AppSettingsDTO();
    updateDTO.setAppName("Updated App Name");
    updateDTO.setActiveTheme("emerald-matrix");
    updateDTO.setDefaultLanguage("en");
    updateDTO.setLoginMode("CUSTOM_BG");
    updateDTO.setLoginBgUrl("data:image/png;base64,sample");
    updateDTO.setLoginBgOpacity(75);

    StepVerifier.create(appSettingsService.updateSettings(updateDTO))
        .assertNext(
            result -> {
              assertNotNull(result);
              assertEquals("Updated App Name", result.getAppName());
              assertEquals("emerald-matrix", result.getActiveTheme());
              assertEquals("en", result.getDefaultLanguage());
              assertEquals("CUSTOM_BG", result.getLoginMode());
              assertEquals(75, result.getLoginBgOpacity());
            })
        .verifyComplete();

    verify(appSettingsDAO, times(1)).save(any(AppSettingsEntity.class));

    StepVerifier.create(appSettingsService.getPublicSettings())
        .assertNext(
            pub -> {
              assertEquals("Updated App Name", pub.getAppName());
              assertEquals("emerald-matrix", pub.getActiveTheme());
            })
        .verifyComplete();

    verify(appSettingsDAO, times(1)).findAll();
  }

  @Test
  @DisplayName("createBackup exports application snapshot")
  void testCreateBackup() {
    ClientCredentialEntity client = new ClientCredentialEntity();
    client.setClientId("TEST-CLIENT");

    UserEntity user = new UserEntity();
    user.setUsername("testuser");

    when(appSettingsDAO.findAll()).thenReturn(List.of(sampleEntity));
    when(clientDAO.findAll()).thenReturn(List.of(client));
    when(userDAO.findAll()).thenReturn(List.of(user));

    StepVerifier.create(appSettingsService.createBackup("ADMIN_TEST"))
        .assertNext(
            backup -> {
              assertNotNull(backup);
              assertEquals("2.0.0", backup.getVersion());
              assertEquals("ADMIN_TEST", backup.getExportedBy());
              assertNotNull(backup.getTimestamp());
              assertNotNull(backup.getSettings());
              assertEquals("Access Sphere Test", backup.getSettings().getAppName());
              assertEquals(1, backup.getClients().size());
              assertEquals("TEST-CLIENT", backup.getClients().get(0).getClientId());
              assertEquals(1, backup.getUsers().size());
              assertEquals("testuser", backup.getUsers().get(0).getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("restoreBackup successfully restores settings, clients, and users")
  void testRestoreBackup() {
    when(appSettingsDAO.findAll()).thenReturn(List.of(sampleEntity));

    ClientCredentialEntity existingClient = new ClientCredentialEntity();
    existingClient.setId(10L);
    existingClient.setClientId("EXISTING-CLIENT");
    when(clientDAO.findByClientId("EXISTING-CLIENT")).thenReturn(existingClient);

    UserEntity existingUser = new UserEntity();
    existingUser.setId(20L);
    existingUser.setUsername("existinguser");
    when(userDAO.findUserEntityByUsernameOrEmail("existinguser", "test@domain.com"))
        .thenReturn(existingUser);

    BackupDataDTO backup = new BackupDataDTO();
    backup.setVersion("2.0.0");
    backup.setTimestamp("2026-09-12 12:00:00");

    AppSettingsEntity restoreSettings = new AppSettingsEntity();
    restoreSettings.setAppName("Restored App");
    backup.setSettings(restoreSettings);

    ClientCredentialEntity restoreClient = new ClientCredentialEntity();
    restoreClient.setClientId("EXISTING-CLIENT");
    backup.setClients(List.of(restoreClient));

    UserEntity restoreUser = new UserEntity();
    restoreUser.setUsername("existinguser");
    restoreUser.setEmail("test@domain.com");
    backup.setUsers(List.of(restoreUser));

    StepVerifier.create(appSettingsService.restoreBackup(backup))
        .assertNext(
            msg -> {
              assertNotNull(msg);
              assertTrue(msg.contains("successo"));
            })
        .verifyComplete();

    verify(appSettingsDAO, times(1)).save(restoreSettings);
    assertEquals(1L, restoreSettings.getId());

    verify(clientDAO, times(1)).save(restoreClient);
    assertEquals(10L, restoreClient.getId());

    verify(userDAO, times(1)).save(restoreUser);
    assertEquals(20L, restoreUser.getId());
  }
}
