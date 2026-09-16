package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.client.IClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.email.IEmailTemplateDAO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
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
      settings.setHideHomeButton(false);
      settings.setNavigationLayout("HEADER");
      appSettingsDAO.save(settings);
      LOG.info("✅ Default Application Settings successfully seeded in DB");
    } else {
      LOG.debug("Application Settings already present in DB");
    }
  }

  /**
   * Inizializzazione del template email predefinito per il reset della password.
   * Si interconnette con {@link IEmailTemplateDAO} per verificare se il template
   * 'ACCESS_SPHERE_CODE_RESET_PASSWORD' è già censito nella tabella 'EMAIL_TEMPLATES'.
   * Se assente, viene inserito il template HTML reattivo con i relativi parametri segnaposto
   * (SITE_URL, NAME, RESET_CODE, SITE_SHORT) utilizzati poi da {@link io.github.giovannilamarmora.accesssphere.email.EmailTemplateService}
   * e {@link io.github.giovannilamarmora.accesssphere.data.user.UserService#changePasswordRequest}.
   */
  private void initEmailTemplates() {
    List<EmailTemplateEntity> existing =
        emailTemplateDAO.findByIdentifier("ACCESS_SPHERE_CODE_RESET_PASSWORD");
    if (existing.isEmpty()) {
      LOG.info("📧 Seeding Default Email Template: ACCESS_SPHERE_CODE_RESET_PASSWORD");
      EmailTemplateEntity template = new EmailTemplateEntity();
      template.setIdentifier("ACCESS_SPHERE_CODE_RESET_PASSWORD");
      template.setCategory("RESET_PASSWORD");
      template.setClientId(techClientId); // Collegato al client applicativo predefinito configurato all'avvio
      template.setName("ACCESS_SPHERE_CODE_RESET_PASSWORD");
      template.setSubject("Reset Password");
      template.setDescription("Template per invio codice recupero password");
      template.setLocale("it");

      // Mappa parametri predefiniti associati al template
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("SITE_URL", "https://accesssphere.io");
      params.put("NAME", "Mario Rossi");
      params.put("RESET_CODE", "123456");
      params.put("SITE_SHORT", "Access Sphere");
      template.setParams(params);

      // Markup HTML reattivo con glassmorphism per email di reimpostazione password
      String htmlTemplate =
          """
          <!DOCTYPE html>
          <html lang="en-GB">
            <head>
              <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
              <title>Reset Password</title>
              <meta content="Reset Password Email Template." name="description" />
              <style type="text/css">
                a:hover {
                  text-decoration: underline !important;
                }
              </style>
            </head>

            <body
              marginheight="0"
              topmargin="0"
              marginwidth="0"
              style="
                margin: 0px;
                background-color: #f2f3f8;
                background: linear-gradient(135deg, #101022 0%, #05050f 100%);
              "
              leftmargin="0"
            >
              <!--100% body table-->
              <table
                cellspacing="0"
                border="0"
                cellpadding="0"
                width="100%"
                bgcolor="#f2f3f8"
                style="
                  @import url(https://fonts.googleapis.com/css?family=Rubik:300,400,500,700|Open+Sans:300,400,600,700);
                  font-family: 'Open Sans', sans-serif;
                  background: linear-gradient(135deg, #101022 0%, #05050f 100%);
                "
              >
                <tr>
                  <td>
                    <table
                      style="background-color: #f2f3f8; max-width: 670px; margin: 0 auto; background: linear-gradient(135deg, #101022 0%, #05050f 100%);"
                      width="100%"
                      border="0"
                      align="center"
                      cellpadding="0"
                      cellspacing="0"
                    >
                      <tr>
                        <td style="height: 80px">&nbsp;</td>
                      </tr>
                      <tr>
                        <td style="text-align: center">
                          <a href="{{SITE_URL}}" title="logo" target="_blank">
                            <img
                              style="border-radius: 50%"
                              width="100"
                              src="https://raw.githubusercontent.com/giovannilamarmora/giovannilamarmora/main/assets/brand/app-logo.png"
                              title="logo"
                              alt="logo"
                            />
                          </a>
                        </td>
                      </tr>
                      <tr>
                        <td style="height: 20px">&nbsp;</td>
                      </tr>
                      <tr>
                        <td>
                          <table
                            width="95%"
                            border="0"
                            align="center"
                            cellpadding="0"
                            cellspacing="0"
                            style="
                              max-width: 670px;
                              background: #191933;
                              border-radius: 3px;
                              text-align: center;
                              -webkit-box-shadow: 0 6px 18px 0 rgba(0, 0, 0, 0.06);
                              -moz-box-shadow: 0 6px 18px 0 rgba(0, 0, 0, 0.06);
                              box-shadow: 0 6px 18px 0 rgba(0, 0, 0, 0.06);
                            "
                          >
                            <tr>
                              <td style="height: 40px">&nbsp;</td>
                            </tr>
                            <tr>
                              <td style="padding: 0 35px">
                                <h1
                                  style="
                                    color: #ffffffe6;
                                    font-weight: 500;
                                    margin: 0;
                                    font-size: 32px;
                                    font-family: 'Rubik', sans-serif;
                                  "
                                >
                                  Hai richiesto il reset della password
                                </h1>
                                <span
                                  style="
                                    display: inline-block;
                                    vertical-align: middle;
                                    margin: 29px 0 26px;
                                    border-bottom: 1px solid #cecece;
                                    width: 100px;
                                  "
                                ></span>
                                <p
                                  style="
                                    color: #ffffff99;
                                    font-size: 15px;
                                    line-height: 24px;
                                    margin: 0;
                                  "
                                >
                                  Ciao {{NAME}}, non possiamo semplicemente inviarti la tua vecchia password.
                                  E' stato generato un codice temporaneo per reimpostare la tua password.
                                  Per reimpostare la password, usa il seguente codice sul sito.
                                </p>
                                <p
                                  style="
                                    background: #ff5722;
                                    text-decoration: none !important;
                                    font-weight: 500;
                                    margin-top: 35px;
                                    color: #fff;
                                    text-transform: uppercase;
                                    font-size: 18px;
                                    padding: 10px 24px;
                                    display: inline-block;
                                    border-radius: 50px;
                                  "
                                >
                                  {{RESET_CODE}}
                                </p>
                              </td>
                            </tr>
                            <tr>
                              <td style="height: 40px">&nbsp;</td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="height: 20px">&nbsp;</td>
                      </tr>
                      <tr>
                        <td style="text-align: center">
                          <p
                            style="
                              font-size: 14px;
                              color: rgba(69, 80, 86, 0.7411764705882353);
                              line-height: 18px;
                              margin: 0 0 0;
                            "
                          >
                            &copy; <strong>{{SITE_SHORT}}</strong>
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="height: 80px">&nbsp;</td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
              <!--/100% body table-->
            </body>
          </html>
          """;
      template.setTemplate(htmlTemplate);

      // Persistenza template a database
      emailTemplateDAO.save(template);
      LOG.info(
          "✅ Default Email Template [ACCESS_SPHERE_CODE_RESET_PASSWORD] successfully seeded in DB with clientId [{}]", techClientId);
    } else {
      EmailTemplateEntity defaultTpl = existing.get(0);
      if (defaultTpl.getClientId() == null) {
        defaultTpl.setClientId(techClientId);
        emailTemplateDAO.save(defaultTpl);
        LOG.info("🔄 Migrated Default Email Template [{}] to technical client [{}]", defaultTpl.getIdentifier(), techClientId);
      } else {
        LOG.debug("Email Template [ACCESS_SPHERE_CODE_RESET_PASSWORD] already present in DB");
      }
    }
  }
}
