package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.email.IEmailTemplateDAO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public static final String DEFAULT_RESET_PASSWORD_TEMPLATE_UUID =
      "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95";
  public static final String LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER =
      "ACCESS_SPHERE_CODE_RESET_PASSWORD";

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
  @Autowired private IEmailTemplateDAO emailTemplateDAO;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    LOG.info("🚀 Checking Access Sphere Initial DB Seeding...");

    initTechnicalClient();
    initTechnicalUser();
    initAppSettings();
    initEmailTemplates();

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
      client.setRedirect_uri(
          "http://localhost:8081/app,http://localhost:8080/app,/app,http://localhost:3000/app");
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
      LOG.info(
          "✅ Technical User [{}] successfully seeded in DB with roles ROLE_ADMIN ROLE_TECH",
          techUsername);
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
      settings.setHideHomeButton(false);
      settings.setNavigationLayout("HEADER");
      appSettingsDAO.save(settings);
      LOG.info("✅ Default Application Settings successfully seeded in DB");
    } else {
      LOG.debug("Application Settings already present in DB");
    }
  }

  /**
   * Inizializzazione del template email predefinito per il reset della password con identificatore
   * UUID. Si interconnette con {@link IEmailTemplateDAO} per verificare se il template con UUID
   * {@value #DEFAULT_RESET_PASSWORD_TEMPLATE_UUID} è già censito nella tabella 'EMAIL_TEMPLATES'.
   * Se assente, verifica la presenza del template con identificatore legacy {@value
   * #LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER} ed esegue la migrazione automatica
   * all'identificatore UUID e al layout email responsive/bulletproof, collegandolo al client
   * tecnico {@code techClientId} e impostando i parametri segnaposto standard (SITE_URL, NAME,
   * RESET_CODE, SITE_SHORT).
   */
  private void initEmailTemplates() {
    List<EmailTemplateEntity> existing =
        emailTemplateDAO.findByIdentifier(DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
    List<EmailTemplateEntity> legacyExisting =
        emailTemplateDAO.findByIdentifier(LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER);

    String htmlTemplate =
        """
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Reset Password</title>
            <!--[if mso]>
            <noscript>
              <xml>
                <o:OfficeDocumentSettings>
                  <o:PixelsPerInch>96</o:PixelsPerInch>
                </o:OfficeDocumentSettings>
              </xml>
            </noscript>
            <![endif]-->
            <style type="text/css">
              @import url("https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap");
              body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
              table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
              img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
              body { margin: 0; padding: 0; width: 100% !important; background-color: #0c0d1e; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
              a:hover { opacity: 0.9 !important; text-decoration: none !important; }
            </style>
          </head>
          <body style="margin: 0; padding: 0; background-color: #0c0d1e; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
            <!-- 100% background wrapper table -->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #0c0d1e; min-width: 100%;">
              <tr>
                <td align="center" style="padding: 40px 16px;">
                  <!--[if (gte mso 9)|(IE)]>
                  <table align="center" border="0" cellpadding="0" cellspacing="0" width="600">
                  <tr>
                  <td>
                  <![endif]-->
                  <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; margin: 0 auto;">
                    <!-- Brand Logo Header -->
                    <tr>
                      <td align="center" style="padding-bottom: 24px;">
                        <a href="{{SITE_URL}}" target="_blank" title="Access Sphere" style="text-decoration: none; display: inline-block;">
                          <img src="https://access-sphere.giovannilamarmora.com/img/Access%20Sphere%20Transparent%20512x512.png" alt="Access Sphere Logo" width="72" height="72" style="display: block; width: 72px; height: 72px; border-radius: 50%; border: 1px solid rgba(168, 85, 247, 0.35); background-color: #16172e;" />
                        </a>
                      </td>
                    </tr>
                    <!-- Card Container -->
                    <tr>
                      <td align="center">
                        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #16172e; border-radius: 20px; border: 1px solid #292a4d; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35); overflow: hidden;">
                          <tr>
                            <td align="center" style="padding: 40px 32px;">
                              <!-- Salutation -->
                              <h2 style="color: #a78bfa; font-size: 16px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; margin: 0 0 12px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                                Hello {{NAME}},
                              </h2>
                              <!-- Main Title -->
                              <h1 style="color: #ffffff; font-size: 24px; font-weight: 700; line-height: 1.35; margin: 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                                You have requested to reset your password
                              </h1>
                              <!-- Gradient Line Separator -->
                              <div style="display: inline-block; vertical-align: middle; margin: 24px auto; border-bottom: 2px solid #7c3aed; width: 60px;"></div>
                              <!-- Instructional Text -->
                              <p style="color: #cbd5e1; font-size: 15px; line-height: 24px; margin: 0 0 24px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                                Use the following security code to complete your password reset. If you did not request a reset, please ignore this email.
                              </p>
                              <!-- Verification Code Highlight Block -->
                              <table border="0" cellpadding="0" cellspacing="0" align="center" style="margin: 20px auto;">
                                <tr>
                                  <td align="center" style="background-color: #201f3d; border: 1px dashed #a855f7; border-radius: 14px; padding: 16px 32px;">
                                    <span style="font-family: 'Courier New', Courier, monospace, 'Poppins', sans-serif; font-size: 32px; font-weight: 700; letter-spacing: 8px; color: #f5f3ff; display: inline-block;">
                                      {{RESET_CODE}}
                                    </span>
                                  </td>
                                </tr>
                              </table>
                              <!-- Expiration Warning -->
                              <p style="color: #94a3b8; font-size: 13px; line-height: 20px; margin: 16px 0 0 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                                <i style="display: inline-block;">&#9201;</i> The code is valid for <strong>15 minutes</strong>.
                              </p>
                              <!-- CTA Button to Platform -->
                              <table border="0" cellpadding="0" cellspacing="0" align="center" style="margin-top: 32px;">
                                <tr>
                                  <td align="center" style="border-radius: 50px; background-color: #7c3aed;">
                                    <a href="{{SITE_URL}}" target="_blank" style="font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; font-weight: 600; color: #ffffff; text-decoration: none; padding: 13px 32px; border-radius: 50px; display: inline-block; text-transform: uppercase; letter-spacing: 0.5px;">
                                      Reset Password
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <!-- Email Footer -->
                    <tr>
                      <td align="center" style="padding-top: 24px;">
                        <p style="font-size: 13px; color: #64748b; line-height: 18px; margin: 0 0 6px 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                          &copy; <strong>{{SITE_SHORT}}</strong> &bull; Identity &amp; Access Management
                        </p>
                        <p style="font-size: 12px; color: #475569; margin: 0; font-family: 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                          Automated security notification generated by <a href="{{SITE_URL}}" target="_blank" style="color: #a78bfa; text-decoration: none;">Access Sphere</a>.
                        </p>
                      </td>
                    </tr>
                  </table>
                  <!--[if (gte mso 9)|(IE)]>
                  </td>
                  </tr>
                  </table>
                  <![endif]-->
                </td>
              </tr>
            </table>
          </body>
        </html>
        """;

    if (existing.isEmpty() && legacyExisting.isEmpty()) {
      LOG.info(
          "📧 Seeding Default Email Template with UUID: {}", DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
      EmailTemplateEntity template = new EmailTemplateEntity();
      template.setIdentifier(DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
      template.setCategory(EmailTemplateCategory.RESET_PASSWORD.getCode());
      template.setClientId(
          techClientId); // Collegato al client applicativo predefinito configurato all'avvio
      template.setName("Reset Password");
      template.setSubject("Reset Password");
      template.setDescription("Template for sending password reset code");
      template.setLocale("en");

      // Mappa parametri predefiniti associati al template
      Map<String, String> params = new LinkedHashMap<>();
      params.put("SITE_URL", "https://accesssphere.io");
      params.put("NAME", "John Doe");
      params.put("RESET_CODE", "123456");
      params.put("SITE_SHORT", "Access Sphere");
      template.setParams(params);
      template.setTemplate(htmlTemplate);

      // Persistenza template a database
      emailTemplateDAO.save(template);
      LOG.info(
          "✅ Default Email Template [{}] successfully seeded in DB with clientId [{}]",
          DEFAULT_RESET_PASSWORD_TEMPLATE_UUID,
          techClientId);
    } else if (existing.isEmpty() && !legacyExisting.isEmpty()) {
      // Migrazione automatica da identificatore legacy ad identificatore UUID
      EmailTemplateEntity legacyTpl = legacyExisting.get(0);
      legacyTpl.setIdentifier(DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
      legacyTpl.setCategory(EmailTemplateCategory.RESET_PASSWORD.getCode());
      legacyTpl.setLocale("en");
      legacyTpl.setSubject("Reset Password");
      legacyTpl.setDescription("Template for sending password reset code");
      if (legacyTpl.getClientId() == null) {
        legacyTpl.setClientId(techClientId);
      }
      legacyTpl.setTemplate(htmlTemplate);
      emailTemplateDAO.save(legacyTpl);
      LOG.info(
          "🔄 Migrated Legacy Email Template [{}] to UUID [{}] and refreshed English email-safe template with clientId [{}]",
          LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER,
          DEFAULT_RESET_PASSWORD_TEMPLATE_UUID,
          legacyTpl.getClientId());
    } else {
      EmailTemplateEntity defaultTpl = existing.get(0);
      boolean updated = false;
      if (defaultTpl.getClientId() == null) {
        defaultTpl.setClientId(techClientId);
        updated = true;
      }
      if (defaultTpl.getTemplate() == null
          || defaultTpl.getTemplate().contains("Ciao")
          || !defaultTpl.getTemplate().contains("Poppins")) {
        defaultTpl.setTemplate(htmlTemplate);
        defaultTpl.setLocale("en");
        defaultTpl.setSubject("Reset Password");
        defaultTpl.setDescription("Template for sending password reset code");
        updated = true;
      }
      if (updated) {
        emailTemplateDAO.save(defaultTpl);
        LOG.info(
            "🔄 Updated Default Email Template [{}] with technical client [{}] and modern English email-safe template",
            defaultTpl.getIdentifier(),
            techClientId);
      } else {
        LOG.debug(
            "Email Template [{}] already present in DB", DEFAULT_RESET_PASSWORD_TEMPLATE_UUID);
      }
    }
  }
}
