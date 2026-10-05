package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.client.database.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserConfig;
import io.github.giovannilamarmora.accesssphere.email.template.database.EmailRepository;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.settings.IAppSettingsDAO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.accesssphere.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.utilities.URLUtils;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
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
  @Autowired private EmailRepository emailRepository;

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
            SystemPermission.ROLES_MANAGE.getValue(),
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
  /**
   * Inizializzazione di tutti i template email predefiniti a sistema per ciascuna categoria applicativa
   * definita in {@link EmailTemplateCategory} (Reset Password, Welcome User, Email Verification,
   * MFA OTP, Account Locked).
   * <p>
   * Si interconnette con {@link #seedEmailTemplateCategory(EmailTemplateCategory, String)} per ciascuna
   * categoria, caricando l'HTML associato dalle risorse di sistema ed eseguendo il seeding o aggiornamento
   * nella tabella 'EMAIL_TEMPLATES' tramite {@link EmailRepository}.
   */
  private void initEmailTemplates() {
    // Risoluzione del Client ID tecnico associato alle risorse di sistema
    String techClientId =
        StringUtils.hasText(adminUserConfig.getAdminClientId())
            ? adminUserConfig.getAdminClientId()
            : "ACCESS-SPHERE-AUTH";

    // Ciclo di scansione e seeding su tutte le categorie di template censite
    for (EmailTemplateCategory cat : EmailTemplateCategory.values()) {
      seedEmailTemplateCategory(cat, techClientId);
    }
  }

  /**
   * Effettua la verifica di persistenza, la migrazione legacy o il seeding a database di una specifica
   * categoria di template email.
   * <p>
   * Flusso logico ed interconnessioni:
   * 1. Richiama {@link #loadTemplateFromResource(String)} per prelevare il file HTML responsive da classpath.
   * 2. Interroga {@link EmailRepository#findByIdentifier(String)} con l'identificatore UUID della categoria.
   * 3. Se assente, verifica se esiste un record con l'identificatore legacy ed esegue la migrazione a UUID.
   * 4. Se assente del tutto, crea un nuovo modello {@link EmailTemplate} collegato a {@code techClientId}
   *    con parametri predefiniti e lo persiste su DB tramite {@link EmailRepository#save(EmailTemplate)}.
   * 5. Se già presente, aggiorna client ID o corpo HTML qualora obsoleti.
   *
   * @param cat Categoria applicativa del template da verificare e persistere
   * @param techClientId Identificativo del client tecnico di sistema
   */
  private void seedEmailTemplateCategory(EmailTemplateCategory cat, String techClientId) {
    String identifier = cat.getDefaultIdentifier();
    String legacyIdentifier = cat.getLegacyIdentifier();
    // Caricamento del template HTML dalla cartella risorse
    String htmlTemplate = loadTemplateFromResource(cat.getTemplateFileName());

    // Ricerca template esistente per UUID o per identificatore legacy
    List<EmailTemplate> existing = emailRepository.findByIdentifier(identifier).orElse(List.of());
    List<EmailTemplate> legacyExisting =
        StringUtils.hasText(legacyIdentifier)
            ? emailRepository.findByIdentifier(legacyIdentifier).orElse(List.of())
            : List.of();

    if (existing.isEmpty() && legacyExisting.isEmpty()) {
      // 1. Caso nuovo template: seeding iniziale a database
      LOG.info("📧 Seeding Default Email Template [{}] for category [{}]", identifier, cat.getCode());
      EmailTemplate template = new EmailTemplate();
      template.setIdentifier(identifier);
      template.setCategory(cat.getCode());
      template.setClientId(techClientId);
      template.setName(cat.getEnglishName());
      template.setSubject(cat.getDefaultSubject());
      template.setDescription(cat.getEnglishDescription());
      template.setLocale("en");
      template.setParams(cat.getDefaultParamsAsStringMap());
      template.setTemplate(htmlTemplate);

      emailRepository.save(template);
      LOG.info(
          "✅ Email Template [{}] for category [{}] successfully seeded in DB with clientId [{}]",
          identifier,
          cat.getCode(),
          techClientId);
    } else if (existing.isEmpty() && !legacyExisting.isEmpty()) {
      // 2. Caso migrazione automatica da identificatore legacy ad identificatore UUID
      EmailTemplate legacyTpl = legacyExisting.get(0);
      legacyTpl.setIdentifier(identifier);
      legacyTpl.setCategory(cat.getCode());
      legacyTpl.setLocale("en");
      legacyTpl.setName(cat.getEnglishName());
      legacyTpl.setSubject(cat.getDefaultSubject());
      legacyTpl.setDescription(cat.getEnglishDescription());
      if (legacyTpl.getClientId() == null) {
        legacyTpl.setClientId(techClientId);
      }
      legacyTpl.setTemplate(htmlTemplate);
      emailRepository.save(legacyTpl);
      LOG.info(
          "🔄 Migrated Legacy Email Template [{}] to UUID [{}] and refreshed template with clientId [{}]",
          legacyIdentifier,
          identifier,
          legacyTpl.getClientId());
    } else {
      // 3. Caso template già presente: verifica aggiornamento client ID e corpo HTML
      EmailTemplate defaultTpl = existing.get(0);
      boolean updated = false;
      if (defaultTpl.getClientId() == null) {
        defaultTpl.setClientId(techClientId);
        updated = true;
      }
      // Rinfresca il template se privo di contenuto, appartenente a versioni legacy non aggiornate o con segnaposto obsoleti
      if (defaultTpl.getTemplate() == null
          || defaultTpl.getTemplate().contains("Ciao")
          || !defaultTpl.getTemplate().contains("Poppins")
          || (cat == EmailTemplateCategory.EMAIL_VERIFICATION
              && defaultTpl.getTemplate().contains("VERIFICATION_CODE"))) {
        defaultTpl.setTemplate(htmlTemplate);
        defaultTpl.setLocale("en");
        defaultTpl.setName(cat.getEnglishName());
        defaultTpl.setSubject(cat.getDefaultSubject());
        defaultTpl.setDescription(cat.getEnglishDescription());
        defaultTpl.setParams(cat.getDefaultParamsAsStringMap());
        updated = true;
      }
      if (updated) {
        emailRepository.save(defaultTpl);
        LOG.info(
            "🔄 Updated Email Template [{}] ({}) with technical client [{}] and modern template",
            defaultTpl.getIdentifier(),
            cat.getCode(),
            techClientId);
      } else {
        LOG.debug("Email Template [{}] ({}) already present and up to date in DB", identifier, cat.getCode());
      }
    }
  }

  /**
   * Carica il contenuto HTML di un file template da classpath.
   * Verifica sequenzialmente i percorsi 'template/<templateFileName>' (standard applicativo)
   * e 'templates/<templateFileName>' (fallback per convenzione Spring Boot).
   *
   * @param templateFileName Nome del file template HTML presente nella cartella risorse (es. 'reset-password.html')
   * @return Stringa contenente il markup HTML letto con codifica UTF-8
   */
  private String loadTemplateFromResource(String templateFileName) {
    try {
      ClassPathResource resource = new ClassPathResource("template/" + templateFileName);
      if (!resource.exists()) {
        resource = new ClassPathResource("templates/" + templateFileName);
      }
      if (!resource.exists()) {
        LOG.error("❌ Email template resource file not found on classpath: template/{}", templateFileName);
        throw new IllegalStateException("Email template resource not found: " + templateFileName);
      }
      try (InputStream is = resource.getInputStream()) {
        return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      LOG.error("❌ Failed to read email template resource: {}", templateFileName, e);
      throw new IllegalStateException("Failed to read email template resource: " + templateFileName, e);
    }
  }
}
