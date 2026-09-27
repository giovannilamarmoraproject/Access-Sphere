package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserConfig;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.email.IEmailTemplateDAO;
import io.github.giovannilamarmora.accesssphere.email.entity.EmailTemplateEntity;
import io.github.giovannilamarmora.accesssphere.email.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.accesssphere.utilities.URLUtils;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
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
      AdminUserConfig.DEFAULT_RESET_PASSWORD_TEMPLATE_UUID;
  public static final String LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER =
      AdminUserConfig.LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER;

  @Value("${app.domain}")
  private String appDomain;

  @Autowired private AdminUserConfig adminUserConfig;
  @Autowired private IOAuth2ClientDAO clientDAO;
  @Autowired private IUserDAO userDAO;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;
  @Autowired private IAppSettingsDAO appSettingsDAO;
  @Autowired private IEmailTemplateDAO emailTemplateDAO;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    LOG.info("🚀 Checking Access Sphere Initial DB Seeding...");

    initTechnicalClient();
    initRoleDefinitions();
    initSuperAdminUser();
    initAppSettings();
    initEmailTemplates();

    LOG.info("🎉 Access Sphere System Initialization check completed successfully.");
  }

  /**
   * Risolve l'URL base dell'applicazione per la composizione dei redirect URI di default del client
   * tecnico a partire da 'app.domain'. Interconnesso con {@link URLUtils#resolveBaseUrl(String)}
   * per la gestione centralizzata dei protocolli.
   *
   * @return URL base normalizzato comprensivo di schema (http o https) e privo di trailing slash.
   */
  public String resolveBaseUrl() {
    return URLUtils.resolveBaseUrl(appDomain);
  }

  /**
   * Determina se l'host specificato rappresenta un ambiente locale o che richiede protocollo HTTP
   * in chiaro. Interconnesso con {@link URLUtils#isLocalOrInsecureHost(String)}.
   *
   * @param domain Dominio o stringa host:port da verificare
   * @return true se l'host e locale o utilizza la porta standard HTTP 80, false per domini remoti
   *     sicuri
   */
  public static boolean isLocalOrInsecureHost(String domain) {
    return URLUtils.isLocalOrInsecureHost(domain);
  }

  /**
   * Inizializzazione del client OAuth 2.0 tecnico all'avvio dell'applicazione. Utilizza {@link
   * AdminUserConfig} per i parametri del client tecnico ed {@link IOAuth2ClientDAO} per la
   * persistenza. Se il client non esiste nel database, lo crea configurando:
   *
   * <ul>
   *   <li>JWT Secret: letto da {@link AdminUserConfig} o generato a mano a 256-bit
   *   <li>Redirect URI: mappa serializzata in formato JSON con fallback_uri e redirect_uri generati
   *       di default
   * </ul>
   *
   * Se il client esiste gia, verifica e aggiorna eventuali secret mancanti o vecchi formati di
   * redirect URI.
   */
  private void initTechnicalClient() {
    String techClientId = adminUserConfig.getAdminClientId();

    // Risoluzione del JWT Secret: da TechUserConfig o generazione a mano
    String jwtSecret = resolveOrGenerateJwtSecret(techClientId);

    // Generazione automatica dei redirect URI di default basati sull'URL di dominio
    String baseUrl = resolveBaseUrl();
    String fallbackUri = baseUrl + "/app/login";
    String redirectUri = baseUrl + " " + baseUrl + "/app";

    // Mappa salvata su DB in formato JSON: {"fallback_uri": "...", "redirect_uri": "..."}
    Map<String, String> redirectMap = new LinkedHashMap<>();
    redirectMap.put(RedirectUris.FALLBACK_URI.url(), fallbackUri);
    redirectMap.put(RedirectUris.REDIRECT_URI.url(), redirectUri);
    String redirectUriJson = Mapper.writeObjectToString(redirectMap);

    OAuth2ClientEntity existing = clientDAO.findByClientId(techClientId);
    if (existing == null) {
      LOG.info("🔧 Seeding Technical OAuth Client: {}", techClientId);
      OAuth2ClientEntity client = createClient(techClientId, jwtSecret, redirectUriJson);
      clientDAO.save(client);
      LOG.info(
          "✅ Technical OAuth Client [{}] successfully seeded in DB with JWT Secret and redirect_uri JSON map",
          techClientId);
    } else {
      boolean needsUpdate = false;
      if (!StringUtils.hasText(existing.getJwtSecret())) {
        existing.setJwtSecret(jwtSecret);
        LOG.info(
            "🔑 Configured missing JWT Secret for existing Technical OAuth Client [{}]",
            techClientId);
        needsUpdate = true;
      }
      if (!StringUtils.hasText(existing.getRedirect_uri())
          || !existing.getRedirect_uri().trim().startsWith("{")) {
        existing.setRedirect_uri(redirectUriJson);
        LOG.info(
            "🔄 Migrated redirect_uri to JSON map for existing Technical OAuth Client [{}]",
            techClientId);
        needsUpdate = true;
      }
      if (needsUpdate) {
        clientDAO.save(existing);
        LOG.info("✅ Existing Technical OAuth Client [{}] successfully updated in DB", techClientId);
      } else {
        LOG.debug(
            "Technical OAuth Client [{}] already present in DB with valid settings", techClientId);
      }
    }
  }

  private static @NonNull OAuth2ClientEntity createClient(
      String techClientId, String jwtSecret, String redirectUriJson) {
    OAuth2ClientEntity client = new OAuth2ClientEntity();
    client.setClientId(techClientId);
    client.setExternalClientId(techClientId);
    client.setAccessType(AccessType.OFFLINE);
    client.setAuthType(OAuthType.BEARER);
    client.setTokenType(TokenType.BEARER_JWT);
    client.setScopes("openid profile email read write admin");
    client.setJwtSecret(jwtSecret);
    client.setRedirect_uri(redirectUriJson);
    client.setMfaEnabled(false);
    client.setAuthorize_redirect_status(HttpStatus.FOUND);
    client.setJwtExpiration(3600000L);
    client.setAccessToken(true);
    client.setIdToken(true);
    return client;
  }

  /**
   * Inizializza le definizioni dei ruoli applicativi di default nella tabella relazionale {@code
   * ROLE_DEFINITIONS}.
   *
   * <p>Interconnessioni:
   *
   * <ul>
   *   <li>Assicura la presenza e il corretto catalogo di permessi granulari {@link
   *       SystemPermission} per tutti i ruoli canonici definiti in {@link AppRoleType} ({@code
   *       SUPER_ADMIN}, {@code ADMIN}, {@code MANAGER}, {@code MODERATOR}, {@code USER}, {@code
   *       GUEST}) e per i ruoli tecnici ({@code ROLE_TECH}).
   *   <li>I ruoli censiti per il client tecnico governano l'accesso e la visibilità della console
   *       IAM, lo scoping multi-tenant e l'autorizzazione degli endpoint amministrativi.
   * </ul>
   */
  private void initRoleDefinitions() {
    LOG.info("🔐 Checking enterprise Role Definitions and Permissions seeding...");
    String techClientId = adminUserConfig.getAdminClientId();
    final String rolePrefix = "ACCESS-SPHERE_";

    // 1. Super Admin: Accesso globale e totale senza alcuna limitazione
    final String superAdminRoleName = rolePrefix + AppRoleType.SUPER_ADMIN.name();
    seedRoleDefinition(
        techClientId,
        superAdminRoleName,
        AppRoleType.SUPER_ADMIN.name(),
        "Super Administrator role for Access Sphere with unrestricted full access",
        List.of(SystemPermission.ALL.getValue()));

    // 2. Admin: Gestione completa dell'IAM (utenti, ruoli, client, email, impostazioni) eccetto il
    // backup di sistema
    final String adminRoleName = rolePrefix + AppRoleType.ADMIN.name();
    List<String> adminPermissions =
        List.of(
            SystemPermission.USERS_READ.getValue(),
            SystemPermission.USERS_WRITE.getValue(),
            SystemPermission.USERS_DELETE.getValue(),
            SystemPermission.USERS_LOCK.getValue(),
            SystemPermission.USERS_ROLES.getValue(),
            SystemPermission.CLIENTS_READ.getValue(),
            SystemPermission.CLIENTS_WRITE.getValue(),
            SystemPermission.CLIENTS_DELETE.getValue(),
            SystemPermission.SETTINGS_READ.getValue(),
            SystemPermission.SETTINGS_WRITE.getValue(),
            SystemPermission.EMAILS_MANAGE.getValue());
    seedRoleDefinition(
        techClientId,
        adminRoleName,
        AppRoleType.ADMIN.name(),
        "Administrator role for Access Sphere with full management except system backup",
        adminPermissions);

    // 3. Manager: Gestione utenti delegata, assegnazione ruoli e ispezione configurazioni
    final String managerRoleName = rolePrefix + AppRoleType.MANAGER.name();
    List<String> managerPermissions =
        List.of(
            SystemPermission.USERS_READ.getValue(),
            SystemPermission.USERS_WRITE.getValue(),
            SystemPermission.USERS_LOCK.getValue(),
            SystemPermission.USERS_ROLES.getValue(),
            SystemPermission.CLIENTS_READ.getValue(),
            SystemPermission.SETTINGS_READ.getValue());
    seedRoleDefinition(
        techClientId,
        managerRoleName,
        AppRoleType.MANAGER.name(),
        "Manager role with delegated user administration, role assignment, and client inspection capabilities",
        managerPermissions);

    // 4. Operator: Monitoraggio utenze, blocco account e sola consultazione
    final String operatorRoleName = rolePrefix + AppRoleType.OPERATOR.name();
    List<String> operatorPermissions =
        List.of(
            SystemPermission.USERS_READ.getValue(),
            SystemPermission.USERS_LOCK.getValue(),
            SystemPermission.CLIENTS_READ.getValue(),
            SystemPermission.SETTINGS_READ.getValue());
    seedRoleDefinition(
        techClientId,
        operatorRoleName,
        AppRoleType.OPERATOR.name(),
        "Operator role with user monitoring, account locking, and read-only inspection access",
        operatorPermissions);

    // 5. Auditor: Audit di conformità, reportistica e consultazione avanzata
    final String auditorRoleName = rolePrefix + AppRoleType.AUDITOR.name();
    List<String> auditorPermissions =
        List.of(
            SystemPermission.USERS_READ.getValue(),
            SystemPermission.CLIENTS_READ.getValue(),
            SystemPermission.SETTINGS_READ.getValue());
    seedRoleDefinition(
        techClientId,
        auditorRoleName,
        AppRoleType.AUDITOR.name(),
        "Auditor role for compliance, logging, and security inspection in read-only mode",
        auditorPermissions);

    // 6. User: Utente finale standard con abilitazioni self-service (nessun privilegio backoffice)
    final String userRoleName = rolePrefix + AppRoleType.USER.name();
    seedRoleDefinition(
        techClientId,
        userRoleName,
        AppRoleType.USER.name(),
        "Standard user role for regular authenticated users with self-service capabilities",
        List.of());

    // 7. Guest: Utente ospite con privilegi minimi di accesso e sola lettura
    final String guestRoleName = rolePrefix + AppRoleType.GUEST.name();
    seedRoleDefinition(
        techClientId,
        guestRoleName,
        AppRoleType.GUEST.name(),
        "Guest role with minimal read-only guest privileges and restricted access",
        List.of());

    // 8. Technical Service Role: Servizi M2M e chiamate interne infrastrutturali
    final String technicalRoleName = rolePrefix + AppRoleType.TECHNICAL.name();
    seedRoleDefinition(
        techClientId,
        technicalRoleName,
        AppRoleType.TECHNICAL.name(),
        "Technical service role for Access Sphere internal integrations and M2M operations",
        List.of(SystemPermission.ALL.getValue()));
  }

  /**
   * Registra una nuova definizione di ruolo per il client specificato solo se non già presente a
   * database.
   *
   * @param clientId Identificativo univoco del client OAuth2
   * @param roleName Nome canonico del ruolo (es. SUPER_ADMIN, ADMIN, USER)
   * @param roleType Tipologia funzionale del ruolo (es. SUPER_ADMIN, ADMIN, USER, OPERATOR)
   * @param description Descrizione esplicativa dei privilegi e dell'ambito del ruolo
   * @param permissions Lista di stringhe dei permessi 'risorsa:azione' o wildcard '*'
   */
  private void seedRoleDefinition(
      String clientId,
      String roleName,
      String roleType,
      String description,
      List<String> permissions) {
    Optional<RoleDefinitionEntity> existingOpt =
        roleDefinitionDAO.findByClientIdAndRole(clientId, roleName);
    if (existingOpt.isEmpty()) {
      roleDefinitionDAO.save(
          new RoleDefinitionEntity(clientId, roleName, roleType, description, permissions));
      LOG.info(
          "🔧 Default role [{}] seeded in ROLE_DEFINITIONS for client [{}]", roleName, clientId);
    }
  }

  /**
   * Risolve il secret JWT leggendolo da {@link AdminUserConfig} oppure generandolo a mano in modo
   * crittograficamente sicuro a 256-bit (stringa esadecimale da 64 caratteri).
   *
   * @param techClientId Identificativo del client tecnico
   * @return secret JWT a 256-bit esadecimale.
   */
  private String resolveOrGenerateJwtSecret(String techClientId) {
    if (StringUtils.hasText(adminUserConfig.getAdminJwtSecret())) {
      LOG.debug(
          "Using configured JWT Secret from TechUserConfig for Technical OAuth Client [{}]",
          techClientId);
      return adminUserConfig.getAdminJwtSecret().trim();
    }
    LOG.info(
        "🔑 Generating new secure 256-bit random JWT Secret for Technical OAuth Client [{}]",
        techClientId);
    return generateSecureJwtSecret();
  }

  /**
   * Genera un secret crittografico casuale a 256-bit (64 caratteri esadecimali) per la firma dei
   * token JWT. Utilizza {@link SecureRandom} per garantire l'entropia crittografica necessaria per
   * chiavi HMAC-SHA256.
   *
   * @return Stringa esadecimale di 64 caratteri.
   */
  public static String generateSecureJwtSecret() {
    byte[] randomBytes = new byte[32];
    new SecureRandom().nextBytes(randomBytes);
    StringBuilder sb = new StringBuilder(64);
    for (byte b : randomBytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /**
   * Inizializzazione dell'utente tecnico all'avvio dell'applicazione. Interconnesso con {@link
   * AdminUserConfig} per ottenere le credenziali predefinite e con {@link IUserDAO} per la
   * persistenza.
   */
  private void initSuperAdminUser() {
    String adminUsername =
        StringUtils.hasText(adminUserConfig.getAdminUsername())
            ? adminUserConfig.getAdminUsername()
            : "admin";
    String adminEmail =
        StringUtils.hasText(adminUserConfig.getAdminEmail())
            ? adminUserConfig.getAdminEmail()
            : adminUsername + "@accesssphere.local";
    String adminPassword =
        StringUtils.hasText(adminUserConfig.getAdminPassword())
            ? adminUserConfig.getAdminPassword()
            : "password";

    UserEntity existing = userDAO.findUserEntityByUsernameOrEmail(adminUsername, adminEmail);
    if (existing == null) {
      LOG.info("👤 Seeding Admin User: {} ({})", adminUsername, adminEmail);
      UserEntity user = new UserEntity();
      user.setIdentifier(UUID.randomUUID().toString());
      user.setUsername(adminUsername);
      user.setEmail(adminEmail);
      user.setName("Admin");
      user.setSurname("User");
      user.setPassword(bCryptPasswordEncoder.encode(adminPassword));
      userDAO.save(user);

      final String superAdminRoleName = "ACCESS-SPHERE_" + AppRoleType.SUPER_ADMIN.name();
      String techClientId = adminUserConfig.getAdminClientId();
      userClientRoleDAO.save(
          new UserClientRoleEntity(user.getIdentifier(), techClientId, superAdminRoleName));

      LOG.info(
          "✅ Admin User [{}] successfully seeded in DB with roles {} in USER_CLIENT_ROLE",
          superAdminRoleName,
          adminUsername);
    } else {
      LOG.debug("Admin User [{}] already present in DB", adminUsername);
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
   * {DEFAULT_RESET_PASSWORD_TEMPLATE_UUID} è già censito nella tabella 'EMAIL_TEMPLATES'. Se
   * assente, verifica la presenza del template con identificatore legacy
   * {LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER} ed esegue la migrazione automatica
   * all'identificatore UUID e al layout email responsive/bulletproof, collegandolo al client
   * tecnico {@code techClientId} e impostando i parametri segnaposto standard (SITE_URL, NAME,
   * RESET_CODE, SITE_SHORT).
   */
  private void initEmailTemplates() {
    String techClientId =
        StringUtils.hasText(adminUserConfig.getAdminClientId())
            ? adminUserConfig.getAdminClientId()
            : "ACCESS-SPHERE-AUTH";

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
