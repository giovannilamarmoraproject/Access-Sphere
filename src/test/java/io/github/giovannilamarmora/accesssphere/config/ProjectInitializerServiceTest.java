package io.github.giovannilamarmora.accesssphere.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectInitializerServiceTest {

  @Mock private IClientDAO clientDAO;
  @Mock private IUserDAO userDAO;
  @Mock private IAppSettingsDAO appSettingsDAO;

  @InjectMocks private ProjectInitializerService projectInitializerService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(projectInitializerService, "techUsername", "tech_admin");
    ReflectionTestUtils.setField(projectInitializerService, "techPassword", "tech_password");
    ReflectionTestUtils.setField(projectInitializerService, "techClientId", "ACCESS-SPHERE-TECH");
    ReflectionTestUtils.setField(projectInitializerService, "techEmail", "tech_admin@accesssphere.local");
  }

  @Test
  @DisplayName("run seeds client, user, and settings when database is empty")
  void testRunSeedsWhenEmpty() {
    when(clientDAO.findByClientId("ACCESS-SPHERE-TECH")).thenReturn(null);
    when(userDAO.findUserEntityByUsernameOrEmail("tech_admin", "tech_admin@accesssphere.local"))
        .thenReturn(null);
    when(appSettingsDAO.findAll()).thenReturn(Collections.emptyList());

    projectInitializerService.run(null);

    // Verify Client Seed
    ArgumentCaptor<ClientCredentialEntity> clientCaptor =
        ArgumentCaptor.forClass(ClientCredentialEntity.class);
    verify(clientDAO, times(1)).save(clientCaptor.capture());
    ClientCredentialEntity savedClient = clientCaptor.getValue();
    assertEquals("ACCESS-SPHERE-TECH", savedClient.getClientId());
    assertNotNull(savedClient.getAppRoles());
    assertTrue(savedClient.getAppRoles().contains("ROLE_ADMIN"));
    assertTrue(savedClient.getAppRoles().contains("ROLE_TECH"));

    // Verify User Seed
    ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userDAO, times(1)).save(userCaptor.capture());
    UserEntity savedUser = userCaptor.getValue();
    assertEquals("tech_admin", savedUser.getUsername());
    assertEquals("tech_admin@accesssphere.local", savedUser.getEmail());
    assertEquals("ROLE_ADMIN ROLE_TECH", savedUser.getRoles());
    assertNotNull(savedUser.getPassword());

    // Verify Settings Seed
    ArgumentCaptor<AppSettingsEntity> settingsCaptor =
        ArgumentCaptor.forClass(AppSettingsEntity.class);
    verify(appSettingsDAO, times(1)).save(settingsCaptor.capture());
    AppSettingsEntity savedSettings = settingsCaptor.getValue();
    assertEquals("Access Sphere", savedSettings.getAppName());
    assertEquals("cosmic-purple", savedSettings.getActiveTheme());
    assertEquals("SHOWCASE", savedSettings.getLoginMode());
  }

  @Test
  @DisplayName("run does not seed when client, user, and settings already exist")
  void testRunSkipsWhenAlreadyPresent() {
    when(clientDAO.findByClientId("ACCESS-SPHERE-TECH"))
        .thenReturn(new ClientCredentialEntity());
    when(userDAO.findUserEntityByUsernameOrEmail("tech_admin", "tech_admin@accesssphere.local"))
        .thenReturn(new UserEntity());
    when(appSettingsDAO.findAll()).thenReturn(List.of(new AppSettingsEntity()));

    projectInitializerService.run(null);

    verify(clientDAO, never()).save(any());
    verify(userDAO, never()).save(any());
    verify(appSettingsDAO, never()).save(any());
  }
}
