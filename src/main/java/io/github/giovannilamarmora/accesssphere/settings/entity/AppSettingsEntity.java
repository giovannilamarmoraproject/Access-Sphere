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
  // raggruppate all'interno delle mappe serializzate JSON soprastanti)
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

  /**
   * Sincronizza le mappe di categoria prima di ogni operazione di INSERT o UPDATE sul database.
   * Popola le tre mappe raggruppate (LOGIN_SETTINGS, BRANDING_SETTINGS, POLICY_SETTINGS).
   */
  @PrePersist
  @PreUpdate
  public void syncCategoryMapsBeforeSave() {
    if (this.loginSettings == null) this.loginSettings = new LinkedHashMap<>();
    this.loginSettings.put("loginMode", this.loginMode != null ? this.loginMode : "SHOWCASE");
    this.loginSettings.put("loginBgUrl", this.loginBgUrl);
    this.loginSettings.put("loginBgOpacity", this.loginBgOpacity != null ? this.loginBgOpacity : 50);
    this.loginSettings.put("loginHeroTitle", this.loginHeroTitle != null ? this.loginHeroTitle : "Autenticazione Sicura & Modulare");
    this.loginSettings.put("loginHeroSubtitle", this.loginHeroSubtitle);
    this.loginSettings.put("hideHomeButton", this.hideHomeButton != null ? this.hideHomeButton : false);

    if (this.brandingSettings == null) this.brandingSettings = new LinkedHashMap<>();
    this.brandingSettings.put("appName", this.appName != null ? this.appName : "Access Sphere");
    this.brandingSettings.put("appTagline", this.appTagline != null ? this.appTagline : "Gestione Identità & Accessi");
    this.brandingSettings.put("logoUrl", this.logoUrl != null ? this.logoUrl : "/img/logo-minimal.svg");
    this.brandingSettings.put("faviconUrl", this.faviconUrl != null ? this.faviconUrl : "/img/logo-minimal.svg");
    this.brandingSettings.put("footerCopyright", this.footerCopyright != null ? this.footerCopyright : "© 2026 Access Sphere");
    this.brandingSettings.put("supportEmail", this.supportEmail != null ? this.supportEmail : "support@accesssphere.io");

    if (this.policySettings == null) this.policySettings = new LinkedHashMap<>();
    this.policySettings.put("privacyPolicyUrl", this.privacyPolicyUrl != null ? this.privacyPolicyUrl : "/privacy-policy");
    this.policySettings.put("cookiePolicyUrl", this.cookiePolicyUrl != null ? this.cookiePolicyUrl : "/cookie-policy");
  }

  /**
   * Sincronizza i valori delle proprietà dopo il caricamento dal database (@PostLoad).
   * Estrae i valori salvati nelle mappe serializzate JSON per valorizzare i campi transitori.
   */
  @PostLoad
  public void syncCategoryMapsAfterLoad() {
    if (this.loginSettings != null) {
      if (this.loginSettings.containsKey("loginMode")) this.loginMode = (String) this.loginSettings.get("loginMode");
      if (this.loginSettings.containsKey("loginBgUrl")) this.loginBgUrl = (String) this.loginSettings.get("loginBgUrl");
      if (this.loginSettings.containsKey("loginBgOpacity")) {
        Object val = this.loginSettings.get("loginBgOpacity");
        if (val instanceof Number num) this.loginBgOpacity = num.intValue();
      }
      if (this.loginSettings.containsKey("loginHeroTitle")) this.loginHeroTitle = (String) this.loginSettings.get("loginHeroTitle");
      if (this.loginSettings.containsKey("loginHeroSubtitle")) this.loginHeroSubtitle = (String) this.loginSettings.get("loginHeroSubtitle");
      if (this.loginSettings.containsKey("hideHomeButton")) {
        Object val = this.loginSettings.get("hideHomeButton");
        if (val instanceof Boolean b) this.hideHomeButton = b;
      }
    }
    if (this.brandingSettings != null) {
      if (this.brandingSettings.containsKey("appName")) this.appName = (String) this.brandingSettings.get("appName");
      if (this.brandingSettings.containsKey("appTagline")) this.appTagline = (String) this.brandingSettings.get("appTagline");
      if (this.brandingSettings.containsKey("logoUrl")) this.logoUrl = (String) this.brandingSettings.get("logoUrl");
      if (this.brandingSettings.containsKey("faviconUrl")) this.faviconUrl = (String) this.brandingSettings.get("faviconUrl");
      if (this.brandingSettings.containsKey("footerCopyright")) this.footerCopyright = (String) this.brandingSettings.get("footerCopyright");
      if (this.brandingSettings.containsKey("supportEmail")) this.supportEmail = (String) this.brandingSettings.get("supportEmail");
    }
    if (this.policySettings != null) {
      if (this.policySettings.containsKey("privacyPolicyUrl")) this.privacyPolicyUrl = (String) this.policySettings.get("privacyPolicyUrl");
      if (this.policySettings.containsKey("cookiePolicyUrl")) this.cookiePolicyUrl = (String) this.policySettings.get("cookiePolicyUrl");
    }
  }

  /** Ritorna la mappa deserializzata della categoria Login. */
  public Map<String, Object> getLoginSettingsMap() {
    if (this.loginSettings == null || this.loginSettings.isEmpty()) {
      syncCategoryMapsBeforeSave();
    }
    return this.loginSettings;
  }

  /** Ritorna la mappa deserializzata della categoria Branding. */
  public Map<String, Object> getBrandingSettingsMap() {
    if (this.brandingSettings == null || this.brandingSettings.isEmpty()) {
      syncCategoryMapsBeforeSave();
    }
    return this.brandingSettings;
  }

  /** Ritorna la mappa deserializzata della categoria Policy. */
  public Map<String, Object> getPolicySettingsMap() {
    if (this.policySettings == null || this.policySettings.isEmpty()) {
      syncCategoryMapsBeforeSave();
    }
    return this.policySettings;
  }
}
