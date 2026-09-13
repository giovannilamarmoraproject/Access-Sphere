package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProjectInitializerService implements ApplicationRunner {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  @Value("${app.tech-user.username:tech_admin}")
  private String techUsername;

  @Value("${app.tech-user.password:tech_password}")
  private String techPassword;

  @Value("${app.tech-user.client-id:ACCESS-SPHERE-TECH}")
  private String techClientId;

  @Value("${app.tech-user.email:admin@accesssphere.local}")
  private String techEmail;

  @Autowired private IClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;
  @Autowired private IAppSettingsDAO appSettingsDAO;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    LOG.info("🚀 Checking Access Sphere Initial DB Seeding...");

    initTechnicalClient();
    initTechnicalUser();
    initAppSettings();

    LOG.info("🎉 Access Sphere System Initialization check completed successfully.");
  }

  private void initTechnicalClient() {
    if (!StringUtils.hasText(techClientId)) {
      techClientId = "ACCESS-SPHERE-TECH";
    }

    ClientCredentialEntity existing = clientDAO.findByClientId(techClientId);
    if (existing == null) {
      LOG.info("🔧 Seeding Technical OAuth Client: {}", techClientId);
      ClientCredentialEntity client = new ClientCredentialEntity();
      client.setClientId(techClientId);
      client.setExternalClientId(techClientId);
      client.setAccessType(AccessType.OFFLINE);
      client.setAuthType(OAuthType.ALL_TYPE);
      client.setTokenType(TokenType.BEARER_JWT);
      client.setScopes("openid profile email read write admin");
      client.setRedirect_uri("http://localhost:8081/app,http://localhost:8080/app,/app,http://localhost:3000/app");
      client.setAppRoles("[{\"role\":\"ROLE_ADMIN\"},{\"role\":\"ROLE_TECH\"}]");
      client.setMfaEnabled(false);
      client.setAuthorize_redirect_status(HttpStatus.FOUND);
      client.setJwtExpiration(86400L);
      client.setAccessToken(true);
      client.setIdToken(true);
      clientDAO.save(client);
      LOG.info("✅ Technical OAuth Client [{}] successfully seeded in DB", techClientId);
    } else {
      LOG.debug("Technical OAuth Client [{}] already present in DB", techClientId);
    }
  }

  private void initTechnicalUser() {
    if (!StringUtils.hasText(techUsername)) {
      techUsername = "tech_admin";
    }
    if (!StringUtils.hasText(techEmail)) {
      techEmail = techUsername + "@accesssphere.local";
    }

    UserEntity existing = userDAO.findUserEntityByUsernameOrEmail(techUsername, techEmail);
    if (existing == null) {
      LOG.info("👤 Seeding Technical User: {} ({})", techUsername, techEmail);
      UserEntity user = new UserEntity();
      user.setIdentifier(UUID.randomUUID().toString());
      user.setStrapiId(0L);
      user.setUsername(techUsername);
      user.setEmail(techEmail);
      user.setName("Technical");
      user.setSurname("Admin");
      user.setPassword(bCryptPasswordEncoder.encode(techPassword));
      user.setRoles("ROLE_ADMIN ROLE_TECH");
      userDAO.save(user);
      LOG.info("✅ Technical User [{}] successfully seeded in DB with roles ROLE_ADMIN ROLE_TECH", techUsername);
    } else {
      LOG.debug("Technical User [{}] already present in DB", techUsername);
    }
  }

  private void initAppSettings() {
    List<AppSettingsEntity> settingsList = appSettingsDAO.findAll();
    if (settingsList.isEmpty()) {
      LOG.info("⚙️ Seeding Default Application Settings...");
      AppSettingsEntity settings = new AppSettingsEntity();
      settings.setAppName("Access Sphere");
      settings.setAppTagline("Gestione Identità & Accessi");
      settings.setLogoUrl("/img/logo-minimal.svg");
      settings.setFaviconUrl("/img/logo-minimal.svg");
      settings.setActiveTheme("cosmic-purple");
      settings.setDefaultLanguage("auto");
      settings.setLoginMode("SHOWCASE");
      settings.setLoginBgOpacity(50);
      settings.setLoginHeroTitle("Autenticazione Sicura & Modulare");
      settings.setLoginHeroSubtitle(
          "Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID Connect e protezione avanzata con crittografia JWT e JWE.");
      settings.setFooterCopyright("© 2026 Access Sphere");
      settings.setPrivacyPolicyUrl("/privacy-policy");
      settings.setCookiePolicyUrl("/cookie-policy");
      settings.setSupportEmail("support@accesssphere.io");
      appSettingsDAO.save(settings);
      LOG.info("✅ Default Application Settings successfully seeded in DB");
    } else {
      LOG.debug("Application Settings already present in DB");
    }
  }
}
