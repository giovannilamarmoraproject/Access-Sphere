package io.github.giovannilamarmora.accesssphere.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserConfig;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.email.IEmailTemplateDAO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  @Mock private IOAuth2ClientDAO clientDAO;
  @Mock private IUserDAO userDAO;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO
      userClientRoleDAO;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO
      roleDefinitionDAO;

  @Mock private IAppSettingsDAO appSettingsDAO;
  @Mock private IEmailTemplateDAO emailTemplateDAO;

  @InjectMocks private ProjectInitializerService projectInitializerService;

  @BeforeEach
  void setUp() {
    TechUserConfig techUserConfig = new TechUserConfig();
    techUserConfig.setTechUsername("tech_admin");
    techUserConfig.setTechPassword("tech_password");
    techUserConfig.setTechClientId("ACCESS-SPHERE-TECH");
    techUserConfig.setTechEmail("tech_admin@accesssphere.local");
    ReflectionTestUtils.setField(projectInitializerService, "techUserConfig", techUserConfig);
    ReflectionTestUtils.setField(projectInitializerService, "userClientRoleDAO", userClientRoleDAO);
    ReflectionTestUtils.setField(projectInitializerService, "roleDefinitionDAO", roleDefinitionDAO);
    ReflectionTestUtils.setField(
        projectInitializerService, "appDomain", "access-sphere.giovannilamarmora.com");
  }

  @Test
  @DisplayName("run seeds client, user, and settings when database is empty")
  void testRunSeedsWhenEmpty() {
    when(clientDAO.findByClientId("ACCESS-SPHERE-TECH")).thenReturn(null);
    when(userDAO.findUserEntityByUsernameOrEmail("tech_admin", "tech_admin@accesssphere.local"))
        .thenReturn(null);
    when(appSettingsDAO.findAll()).thenReturn(Collections.emptyList());
    when(emailTemplateDAO.findByIdentifier(
            ProjectInitializerService.DEFAULT_RESET_PASSWORD_TEMPLATE_UUID))
        .thenReturn(Collections.emptyList());
    when(emailTemplateDAO.findByIdentifier(
            ProjectInitializerService.LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER))
        .thenReturn(Collections.emptyList());

    projectInitializerService.run(null);

    // Verify Client Seed
    ArgumentCaptor<OAuth2ClientEntity> clientCaptor =
        ArgumentCaptor.forClass(OAuth2ClientEntity.class);
    verify(clientDAO, times(1)).save(clientCaptor.capture());
    OAuth2ClientEntity savedClient = clientCaptor.getValue();
    assertEquals("ACCESS-SPHERE-TECH", savedClient.getClientId());
    verify(roleDefinitionDAO, times(9)).save(any());

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

    // Verify Email Template Seed
    ArgumentCaptor<EmailTemplateEntity> templateCaptor =
        ArgumentCaptor.forClass(EmailTemplateEntity.class);
    verify(emailTemplateDAO, times(1)).save(templateCaptor.capture());
    EmailTemplateEntity savedTemplate = templateCaptor.getValue();
    assertEquals(
        ProjectInitializerService.DEFAULT_RESET_PASSWORD_TEMPLATE_UUID,
        savedTemplate.getIdentifier());
    assertEquals("Reset Password", savedTemplate.getSubject());
    assertNotNull(savedTemplate.getTemplate());
    assertTrue(savedTemplate.getTemplate().contains("Poppins"));
    assertNotNull(savedTemplate.getParams());
  }

  @Test
  @DisplayName("run does not seed when client, user, settings, and email templates already exist")
  void testRunSkipsWhenAlreadyPresent() {
    OAuth2ClientEntity existingClient = new OAuth2ClientEntity();
    existingClient.setJwtSecret("existing_jwt_secret_value");
    existingClient.setRedirect_uri(
        "{\"fallback_uri\":\"http://localhost/app/login\",\"redirect_uri\":\"http://localhost/app\"}");
    when(clientDAO.findByClientId("ACCESS-SPHERE-TECH")).thenReturn(existingClient);
    when(userDAO.findUserEntityByUsernameOrEmail("tech_admin", "tech_admin@accesssphere.local"))
        .thenReturn(new UserEntity());
    when(appSettingsDAO.findAll()).thenReturn(List.of(new AppSettingsEntity()));
    EmailTemplateEntity existingTpl = new EmailTemplateEntity();
    existingTpl.setIdentifier(ProjectInitializerService.DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
    existingTpl.setClientId("ACCESS-SPHERE-TECH");
    existingTpl.setTemplate("<html>Poppins</html>");
    when(emailTemplateDAO.findByIdentifier(
            ProjectInitializerService.DEFAULT_RESET_PASSWORD_TEMPLATE_UUID))
        .thenReturn(List.of(existingTpl));
    when(emailTemplateDAO.findByIdentifier(
            ProjectInitializerService.LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER))
        .thenReturn(Collections.emptyList());
    io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity existingRole =
        new io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity(
            "ACCESS-SPHERE-TECH", "ROLE_ADMIN", "ADMIN", "desc", List.of("*"));
    when(roleDefinitionDAO.findByClientIdAndRole(eq("ACCESS-SPHERE-TECH"), any()))
        .thenReturn(Optional.of(existingRole));
    when(roleDefinitionDAO.findAll()).thenReturn(List.of(existingRole));

    projectInitializerService.run(null);

    verify(clientDAO, never()).save(any());
    verify(userDAO, never()).save(any());
    verify(appSettingsDAO, never()).save(any());
    verify(emailTemplateDAO, never()).save(any());
  }

  @Test
  @DisplayName(
      "resolveBaseUrl handles custom domain without scheme, with http, and with trailing slashes")
  void testResolveBaseUrlFormats() {
    // Caso 1: dominio remoto senza schema e con slash finali -> assegna https:// e rimuove trailing
    // slash
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "my-domain.com///");
    assertEquals("https://my-domain.com", projectInitializerService.resolveBaseUrl());

    // Caso 2: localhost senza schema con porta -> assegna automaticamente http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "localhost:8081");
    assertEquals("http://localhost:8081", projectInitializerService.resolveBaseUrl());

    // Caso 3: localhost senza schema e senza porta -> assegna automaticamente http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "localhost");
    assertEquals("http://localhost", projectInitializerService.resolveBaseUrl());

    // Caso 4: 127.0.0.1 senza schema -> assegna automaticamente http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "127.0.0.1:8080");
    assertEquals("http://127.0.0.1:8080", projectInitializerService.resolveBaseUrl());

    // Caso 5: dominio locale .local o IP privato senza schema -> assegna http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "auth-service.local:3000");
    assertEquals("http://auth-service.local:3000", projectInitializerService.resolveBaseUrl());

    // Caso 6: porta 80 senza schema su dominio esterno -> assegna http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "example.com:80");
    assertEquals("http://example.com:80", projectInitializerService.resolveBaseUrl());

    // Caso 7: dominio con http esplicito -> mantiene http://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "http://localhost:8080");
    assertEquals("http://localhost:8080", projectInitializerService.resolveBaseUrl());

    // Caso 8: dominio con https esplicito -> mantiene https://
    ReflectionTestUtils.setField(
        projectInitializerService, "appDomain", "https://auth.example.org/");
    assertEquals("https://auth.example.org", projectInitializerService.resolveBaseUrl());

    // Caso 9: https esplicito su localhost -> mantiene https://
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "https://localhost:8443");
    assertEquals("https://localhost:8443", projectInitializerService.resolveBaseUrl());

    // Caso 10: dominio vuoto o null -> lancia IllegalArgumentException perche il dominio e
    // obbligatorio
    ReflectionTestUtils.setField(projectInitializerService, "appDomain", "");
    assertThrows(IllegalArgumentException.class, () -> projectInitializerService.resolveBaseUrl());

    ReflectionTestUtils.setField(projectInitializerService, "appDomain", null);
    assertThrows(IllegalArgumentException.class, () -> projectInitializerService.resolveBaseUrl());
  }
}
