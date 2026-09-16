package io.github.giovannilamarmora.accesssphere.settings.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import jakarta.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "APP_SETTINGS")
public class AppSettingsEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "ACTIVE_THEME", nullable = false)
  private String activeTheme = "cosmic-purple";

  @Column(name = "DEFAULT_LANGUAGE", nullable = false)
  private String defaultLanguage = "auto";

  /**
   * Modalità di layout della navigazione per le pagine interne autenticate: "HEADER" (Navigazione
   * orizzontale superiore predefinita) oppure "SIDEBAR" (Barra di navigazione laterale
   * espandibile/collassabile M3).
   */
  @Column(name = "NAVIGATION_LAYOUT")
  private String navigationLayout = "HEADER";

  /**
   * Colonna mappa persistita sul DB per raggruppare le impostazioni della categoria "Personalizzazione Login".
   * Include: loginMode, loginBgUrl, loginBgOpacity, loginHeroTitle, loginHeroSubtitle, hideHomeButton.
   * Migliora la pulizia e leggibilità delle colonne a livello di Database.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "LOGIN_SETTINGS", columnDefinition = "LONGTEXT")
  private Map<String, Object> loginSettings = new LinkedHashMap<>();

  /**
   * Colonna mappa persistita sul DB per raggruppare le impostazioni della categoria "Branding & Identità".
   * Include: appName, appTagline, logoUrl, faviconUrl, footerCopyright, supportEmail.
   * Migliora la pulizia e leggibilità delle colonne a livello di Database.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "BRANDING_SETTINGS", columnDefinition = "LONGTEXT")
  private Map<String, Object> brandingSettings = new LinkedHashMap<>();

  /**
   * Colonna mappa persistita sul DB per raggruppare le impostazioni della categoria "Conformità & Politiche".
   * Include: privacyPolicyUrl, cookiePolicyUrl.
   * Migliora la pulizia e leggibilità delle colonne a livello di Database.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "POLICY_SETTINGS", columnDefinition = "TEXT")
  private Map<String, Object> policySettings = new LinkedHashMap<>();

  // =========================================================================
  // Proprietà transitorie (Non persistite come colonne singole sul DB,
  // raggruppate all'interno delle mappe serializzate JSON soprastanti:
  // BRANDING_SETTINGS, LOGIN_SETTINGS, POLICY_SETTINGS).
  // =========================================================================

  @Transient
  private String appName = "Access Sphere";

  @Transient
  private String appTagline = "Gestione Identità & Accessi";

  @Transient
  private String logoUrl = "/img/logo-minimal.svg";

  @Transient
  private String faviconUrl = "/img/logo-minimal.svg";

  @Transient
  private String loginMode = "SHOWCASE"; // "SHOWCASE" or "CUSTOM_BACKGROUND"

  @Transient
  private String loginBgUrl;

  @Transient
  private Integer loginBgOpacity = 50; // Dimming overlay percentage (0-100)

  @Transient
  private String loginHeroTitle = "Autenticazione Sicura & Modulare";

  @Transient
  private String loginHeroSubtitle =
      "Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID Connect e protezione avanzata con crittografia JWT e JWE.";

  @Transient
  private String footerCopyright = "© 2026 Access Sphere";

  @Transient
  private String privacyPolicyUrl = "/privacy-policy";

  @Transient
  private String cookiePolicyUrl = "/cookie-policy";

  @Transient
  private String supportEmail = "support@accesssphere.io";

  @Transient
  private Boolean hideHomeButton = false;

  public AppSettingsEntity() {
    syncCategoryMapsBeforeSave();
  }

  public void setAppName(String appName) {
    this.appName = appName;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("appName", appName);
    }
  }

  public void setAppTagline(String appTagline) {
    this.appTagline = appTagline;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("appTagline", appTagline);
    }
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("logoUrl", logoUrl);
    }
  }

  public void setFaviconUrl(String faviconUrl) {
    this.faviconUrl = faviconUrl;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("faviconUrl", faviconUrl);
    }
  }

  public void setFooterCopyright(String footerCopyright) {
    this.footerCopyright = footerCopyright;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("footerCopyright", footerCopyright);
    }
  }

  public void setSupportEmail(String supportEmail) {
    this.supportEmail = supportEmail;
    if (this.brandingSettings != null) {
      this.brandingSettings.put("supportEmail", supportEmail);
    }
  }

  public void setLoginMode(String loginMode) {
    this.loginMode = loginMode;
    if (this.loginSettings != null) {
      this.loginSettings.put("loginMode", loginMode);
    }
  }

  public void setLoginBgUrl(String loginBgUrl) {
    this.loginBgUrl = loginBgUrl;
    if (this.loginSettings != null) {
      this.loginSettings.put("loginBgUrl", loginBgUrl);
    }
  }

  public void setLoginBgOpacity(Integer loginBgOpacity) {
    this.loginBgOpacity = loginBgOpacity;
    if (this.loginSettings != null) {
      this.loginSettings.put("loginBgOpacity", loginBgOpacity);
    }
  }

  public void setLoginHeroTitle(String loginHeroTitle) {
    this.loginHeroTitle = loginHeroTitle;
    if (this.loginSettings != null) {
      this.loginSettings.put("loginHeroTitle", loginHeroTitle);
    }
  }

  public void setLoginHeroSubtitle(String loginHeroSubtitle) {
    this.loginHeroSubtitle = loginHeroSubtitle;
    if (this.loginSettings != null) {
      this.loginSettings.put("loginHeroSubtitle", loginHeroSubtitle);
    }
  }

  public void setHideHomeButton(Boolean hideHomeButton) {
    this.hideHomeButton = hideHomeButton;
    if (this.loginSettings != null) {
      this.loginSettings.put("hideHomeButton", hideHomeButton);
    }
  }

  public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
    this.privacyPolicyUrl = privacyPolicyUrl;
    if (this.policySettings != null) {
      this.policySettings.put("privacyPolicyUrl", privacyPolicyUrl);
    }
  }

  public void setCookiePolicyUrl(String cookiePolicyUrl) {
    this.cookiePolicyUrl = cookiePolicyUrl;
    if (this.policySettings != null) {
      this.policySettings.put("cookiePolicyUrl", cookiePolicyUrl);
    }
  }

  /**
   * Sincronizza le mappe di categoria prima di ogni operazione di INSERT o UPDATE sul database.
   * Popola le tre mappe raggruppate (LOGIN_SETTINGS, BRANDING_SETTINGS, POLICY_SETTINGS) creando
   * nuove istanze di mappa per garantire che Hibernate rilevi la modifica (dirty-check).
   */
  @PrePersist
  @PreUpdate
  public void syncCategoryMapsBeforeSave() {
    syncLoginSettingsMap();
    syncBrandingSettingsMap();
    syncPolicySettingsMap();
  }

  public void syncLoginSettingsMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("loginMode", this.loginMode != null ? this.loginMode : "SHOWCASE");
    map.put("loginBgUrl", this.loginBgUrl);
    map.put("loginBgOpacity", this.loginBgOpacity != null ? this.loginBgOpacity : 50);
    map.put("loginHeroTitle", this.loginHeroTitle != null ? this.loginHeroTitle : "Autenticazione Sicura & Modulare");
    map.put("loginHeroSubtitle", this.loginHeroSubtitle);
    map.put("hideHomeButton", this.hideHomeButton != null ? this.hideHomeButton : false);
    this.loginSettings = map;
  }

  public void syncBrandingSettingsMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("appName", this.appName != null ? this.appName : "Access Sphere");
    map.put("appTagline", this.appTagline != null ? this.appTagline : "Gestione Identità & Accessi");
    map.put("logoUrl", this.logoUrl != null ? this.logoUrl : "/img/logo-minimal.svg");
    map.put("faviconUrl", this.faviconUrl != null ? this.faviconUrl : "/img/logo-minimal.svg");
    map.put("footerCopyright", this.footerCopyright != null ? this.footerCopyright : "© 2026 Access Sphere");
    map.put("supportEmail", this.supportEmail != null ? this.supportEmail : "support@accesssphere.io");
    this.brandingSettings = map;
  }

  public void syncPolicySettingsMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("privacyPolicyUrl", this.privacyPolicyUrl != null ? this.privacyPolicyUrl : "/privacy-policy");
    map.put("cookiePolicyUrl", this.cookiePolicyUrl != null ? this.cookiePolicyUrl : "/cookie-policy");
    this.policySettings = map;
  }

  /**
   * Sincronizza i valori delle proprietà dopo il caricamento dal database (@PostLoad).
   * Estrae i valori salvati nelle mappe serializzate JSON per valorizzare i campi transitori.
   */
  @PostLoad
  public void syncCategoryMapsAfterLoad() {
    if (this.loginSettings != null && !this.loginSettings.isEmpty()) {
      if (this.loginSettings.containsKey("loginMode") && this.loginSettings.get("loginMode") != null)
        this.loginMode = this.loginSettings.get("loginMode").toString();
      if (this.loginSettings.containsKey("loginBgUrl") && this.loginSettings.get("loginBgUrl") != null)
        this.loginBgUrl = this.loginSettings.get("loginBgUrl").toString();
      if (this.loginSettings.containsKey("loginBgOpacity")) {
        Object val = this.loginSettings.get("loginBgOpacity");
        if (val instanceof Number num) this.loginBgOpacity = num.intValue();
      }
      if (this.loginSettings.containsKey("loginHeroTitle"))
        this.loginHeroTitle = this.loginSettings.get("loginHeroTitle") != null ? this.loginSettings.get("loginHeroTitle").toString() : null;
      if (this.loginSettings.containsKey("loginHeroSubtitle"))
        this.loginHeroSubtitle = this.loginSettings.get("loginHeroSubtitle") != null ? this.loginSettings.get("loginHeroSubtitle").toString() : null;
      if (this.loginSettings.containsKey("hideHomeButton")) {
        Object val = this.loginSettings.get("hideHomeButton");
        if (val instanceof Boolean b) this.hideHomeButton = b;
      }
    }
    syncLoginSettingsMap();

    if (this.brandingSettings != null && !this.brandingSettings.isEmpty()) {
      if (this.brandingSettings.containsKey("appName") && this.brandingSettings.get("appName") != null)
        this.appName = this.brandingSettings.get("appName").toString();
      if (this.brandingSettings.containsKey("appTagline"))
        this.appTagline = this.brandingSettings.get("appTagline") != null ? this.brandingSettings.get("appTagline").toString() : null;
      if (this.brandingSettings.containsKey("logoUrl") && this.brandingSettings.get("logoUrl") != null)
        this.logoUrl = this.brandingSettings.get("logoUrl").toString();
      if (this.brandingSettings.containsKey("faviconUrl") && this.brandingSettings.get("faviconUrl") != null)
        this.faviconUrl = this.brandingSettings.get("faviconUrl").toString();
      if (this.brandingSettings.containsKey("footerCopyright"))
        this.footerCopyright = this.brandingSettings.get("footerCopyright") != null ? this.brandingSettings.get("footerCopyright").toString() : null;
      if (this.brandingSettings.containsKey("supportEmail"))
        this.supportEmail = this.brandingSettings.get("supportEmail") != null ? this.brandingSettings.get("supportEmail").toString() : null;
    }
    syncBrandingSettingsMap();

    if (this.policySettings != null && !this.policySettings.isEmpty()) {
      if (this.policySettings.containsKey("privacyPolicyUrl"))
        this.privacyPolicyUrl = this.policySettings.get("privacyPolicyUrl") != null ? this.policySettings.get("privacyPolicyUrl").toString() : null;
      if (this.policySettings.containsKey("cookiePolicyUrl"))
        this.cookiePolicyUrl = this.policySettings.get("cookiePolicyUrl") != null ? this.policySettings.get("cookiePolicyUrl").toString() : null;
    }
    syncPolicySettingsMap();
  }

  /** Ritorna la mappa deserializzata della categoria Login. */
  public Map<String, Object> getLoginSettingsMap() {
    if (this.loginSettings == null || this.loginSettings.isEmpty()) {
      syncLoginSettingsMap();
    }
    return this.loginSettings;
  }

  /** Ritorna la mappa deserializzata della categoria Branding. */
  public Map<String, Object> getBrandingSettingsMap() {
    if (this.brandingSettings == null || this.brandingSettings.isEmpty()) {
      syncBrandingSettingsMap();
    }
    return this.brandingSettings;
  }

  /** Ritorna la mappa deserializzata della categoria Policy. */
  public Map<String, Object> getPolicySettingsMap() {
    if (this.policySettings == null || this.policySettings.isEmpty()) {
      syncPolicySettingsMap();
    }
    return this.policySettings;
  }
}
