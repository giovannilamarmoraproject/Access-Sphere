package io.github.giovannilamarmora.accesssphere.settings.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "LOGIN_SETTINGS", columnDefinition = "LONGTEXT")
  private Map<String, Object> loginSettings = defaultLoginSettings();

  /**
   * Colonna mappa persistita sul DB per raggruppare le impostazioni della categoria "Branding & Identità".
   * Include: appName, appTagline, logoUrl, faviconUrl, footerCopyright, supportEmail.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "BRANDING_SETTINGS", columnDefinition = "LONGTEXT")
  private Map<String, Object> brandingSettings = defaultBrandingSettings();

  /**
   * Colonna mappa persistita sul DB per raggruppare le impostazioni della categoria "Conformità & Politiche".
   * Include: privacyPolicyUrl, cookiePolicyUrl.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "POLICY_SETTINGS", columnDefinition = "TEXT")
  private Map<String, Object> policySettings = defaultPolicySettings();

  public AppSettingsEntity() {
    this.loginSettings = defaultLoginSettings();
    this.brandingSettings = defaultBrandingSettings();
    this.policySettings = defaultPolicySettings();
  }

  // =========================================================================
  // Valori predefiniti per le mappe di categoria
  // =========================================================================

  public static Map<String, Object> defaultBrandingSettings() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("appName", "Access Sphere");
    map.put("appTagline", "Gestione Identità & Accessi");
    map.put("logoUrl", "/img/logo-minimal.svg");
    map.put("faviconUrl", "/img/logo-minimal.svg");
    map.put("footerCopyright", "© 2026 Access Sphere");
    map.put("supportEmail", "support@accesssphere.io");
    return map;
  }

  public static Map<String, Object> defaultLoginSettings() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("loginMode", "SHOWCASE");
    map.put("loginBgUrl", null);
    map.put("loginBgOpacity", 50);
    map.put("loginHeroTitle", "Autenticazione Sicura & Modulare");
    map.put(
        "loginHeroSubtitle",
        "Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID"
            + " Connect e protezione avanzata con crittografia JWT e JWE.");
    map.put("hideHomeButton", false);
    return map;
  }

  public static Map<String, Object> defaultPolicySettings() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("privacyPolicyUrl", "/privacy-policy");
    map.put("cookiePolicyUrl", "/cookie-policy");
    return map;
  }

  // =========================================================================
  // Setters dedicati per le mappe per garantire creazione di nuove istanze (dirty checking Hibernate)
  // =========================================================================

  public void setBrandingSettings(Map<String, Object> brandingSettings) {
    this.brandingSettings =
        brandingSettings != null
            ? new LinkedHashMap<>(brandingSettings)
            : defaultBrandingSettings();
  }

  public void setLoginSettings(Map<String, Object> loginSettings) {
    this.loginSettings =
        loginSettings != null ? new LinkedHashMap<>(loginSettings) : defaultLoginSettings();
  }

  public void setPolicySettings(Map<String, Object> policySettings) {
    this.policySettings =
        policySettings != null ? new LinkedHashMap<>(policySettings) : defaultPolicySettings();
  }

  // =========================================================================
  // Metodi delegati getter/setter che leggono/scrivono direttamente nelle mappe
  // =========================================================================

  public String getAppName() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("appName")
        && this.brandingSettings.get("appName") != null) {
      return this.brandingSettings.get("appName").toString();
    }
    return "Access Sphere";
  }

  public void setAppName(String appName) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("appName", appName);
  }

  public String getAppTagline() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("appTagline")
        && this.brandingSettings.get("appTagline") != null) {
      return this.brandingSettings.get("appTagline").toString();
    }
    return "Gestione Identità & Accessi";
  }

  public void setAppTagline(String appTagline) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("appTagline", appTagline);
  }

  public String getLogoUrl() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("logoUrl")
        && this.brandingSettings.get("logoUrl") != null) {
      return this.brandingSettings.get("logoUrl").toString();
    }
    return "/img/logo-minimal.svg";
  }

  public void setLogoUrl(String logoUrl) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("logoUrl", logoUrl);
  }

  public String getFaviconUrl() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("faviconUrl")
        && this.brandingSettings.get("faviconUrl") != null) {
      return this.brandingSettings.get("faviconUrl").toString();
    }
    return "/img/logo-minimal.svg";
  }

  public void setFaviconUrl(String faviconUrl) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("faviconUrl", faviconUrl);
  }

  public String getFooterCopyright() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("footerCopyright")
        && this.brandingSettings.get("footerCopyright") != null) {
      return this.brandingSettings.get("footerCopyright").toString();
    }
    return "© 2026 Access Sphere";
  }

  public void setFooterCopyright(String footerCopyright) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("footerCopyright", footerCopyright);
  }

  public String getSupportEmail() {
    if (this.brandingSettings != null
        && this.brandingSettings.containsKey("supportEmail")
        && this.brandingSettings.get("supportEmail") != null) {
      return this.brandingSettings.get("supportEmail").toString();
    }
    return "support@accesssphere.io";
  }

  public void setSupportEmail(String supportEmail) {
    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    }
    this.brandingSettings.put("supportEmail", supportEmail);
  }

  public String getLoginMode() {
    if (this.loginSettings != null
        && this.loginSettings.containsKey("loginMode")
        && this.loginSettings.get("loginMode") != null) {
      return this.loginSettings.get("loginMode").toString();
    }
    return "SHOWCASE";
  }

  public void setLoginMode(String loginMode) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("loginMode", loginMode);
  }

  public String getLoginBgUrl() {
    if (this.loginSettings != null
        && this.loginSettings.containsKey("loginBgUrl")
        && this.loginSettings.get("loginBgUrl") != null) {
      return this.loginSettings.get("loginBgUrl").toString();
    }
    return null;
  }

  public void setLoginBgUrl(String loginBgUrl) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("loginBgUrl", loginBgUrl);
  }

  public Integer getLoginBgOpacity() {
    if (this.loginSettings != null && this.loginSettings.containsKey("loginBgOpacity")) {
      Object val = this.loginSettings.get("loginBgOpacity");
      if (val instanceof Number num) return num.intValue();
      if (val != null) {
        try {
          return Integer.parseInt(val.toString());
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return 50;
  }

  public void setLoginBgOpacity(Integer loginBgOpacity) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("loginBgOpacity", loginBgOpacity != null ? loginBgOpacity : 50);
  }

  public String getLoginHeroTitle() {
    if (this.loginSettings != null
        && this.loginSettings.containsKey("loginHeroTitle")
        && this.loginSettings.get("loginHeroTitle") != null) {
      return this.loginSettings.get("loginHeroTitle").toString();
    }
    return "Autenticazione Sicura & Modulare";
  }

  public void setLoginHeroTitle(String loginHeroTitle) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("loginHeroTitle", loginHeroTitle);
  }

  public String getLoginHeroSubtitle() {
    if (this.loginSettings != null
        && this.loginSettings.containsKey("loginHeroSubtitle")
        && this.loginSettings.get("loginHeroSubtitle") != null) {
      return this.loginSettings.get("loginHeroSubtitle").toString();
    }
    return null;
  }

  public void setLoginHeroSubtitle(String loginHeroSubtitle) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("loginHeroSubtitle", loginHeroSubtitle);
  }

  public Boolean getHideHomeButton() {
    if (this.loginSettings != null && this.loginSettings.containsKey("hideHomeButton")) {
      Object val = this.loginSettings.get("hideHomeButton");
      if (val instanceof Boolean b) return b;
      if (val != null) return Boolean.parseBoolean(val.toString());
    }
    return false;
  }

  public void setHideHomeButton(Boolean hideHomeButton) {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    }
    this.loginSettings.put("hideHomeButton", hideHomeButton != null ? hideHomeButton : false);
  }

  public String getPrivacyPolicyUrl() {
    if (this.policySettings != null
        && this.policySettings.containsKey("privacyPolicyUrl")
        && this.policySettings.get("privacyPolicyUrl") != null) {
      return this.policySettings.get("privacyPolicyUrl").toString();
    }
    return "/privacy-policy";
  }

  public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
    if (this.policySettings == null) {
      this.policySettings = defaultPolicySettings();
    }
    this.policySettings.put("privacyPolicyUrl", privacyPolicyUrl);
  }

  public String getCookiePolicyUrl() {
    if (this.policySettings != null
        && this.policySettings.containsKey("cookiePolicyUrl")
        && this.policySettings.get("cookiePolicyUrl") != null) {
      return this.policySettings.get("cookiePolicyUrl").toString();
    }
    return "/cookie-policy";
  }

  public void setCookiePolicyUrl(String cookiePolicyUrl) {
    if (this.policySettings == null) {
      this.policySettings = defaultPolicySettings();
    }
    this.policySettings.put("cookiePolicyUrl", cookiePolicyUrl);
  }

  // =========================================================================
  // Metodi di sincronizzazione ciclo di vita JPA (@PrePersist, @PreUpdate, @PostLoad)
  // =========================================================================

  @PrePersist
  @PreUpdate
  public void syncCategoryMapsBeforeSave() {
    if (this.loginSettings == null) {
      this.loginSettings = defaultLoginSettings();
    } else {
      this.loginSettings = new LinkedHashMap<>(this.loginSettings);
      defaultLoginSettings().forEach(this.loginSettings::putIfAbsent);
    }

    if (this.brandingSettings == null) {
      this.brandingSettings = defaultBrandingSettings();
    } else {
      this.brandingSettings = new LinkedHashMap<>(this.brandingSettings);
      defaultBrandingSettings().forEach(this.brandingSettings::putIfAbsent);
    }

    if (this.policySettings == null) {
      this.policySettings = defaultPolicySettings();
    } else {
      this.policySettings = new LinkedHashMap<>(this.policySettings);
      defaultPolicySettings().forEach(this.policySettings::putIfAbsent);
    }
  }

  @PostLoad
  public void syncCategoryMapsAfterLoad() {
    syncCategoryMapsBeforeSave();
  }

  /** Ritorna la mappa deserializzata della categoria Login. */
  public Map<String, Object> getLoginSettingsMap() {
    return getLoginSettings();
  }

  /** Ritorna la mappa deserializzata della categoria Branding. */
  public Map<String, Object> getBrandingSettingsMap() {
    return getBrandingSettings();
  }

  /** Ritorna la mappa deserializzata della categoria Policy. */
  public Map<String, Object> getPolicySettingsMap() {
    return getPolicySettings();
  }
}
