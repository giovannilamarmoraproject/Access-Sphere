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
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "APP_SETTINGS")
public class AppSettingsEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "APP_NAME", nullable = false)
  private String appName = "Access Sphere";

  @Column(name = "APP_TAGLINE")
  private String appTagline = "Gestione Identità & Accessi";

  @Lob
  @Column(name = "LOGO_URL", columnDefinition = "LONGTEXT")
  private String logoUrl = "/img/logo-minimal.svg";

  @Lob
  @Column(name = "FAVICON_URL", columnDefinition = "LONGTEXT")
  private String faviconUrl = "/img/logo-minimal.svg";

  @Column(name = "ACTIVE_THEME", nullable = false)
  private String activeTheme = "cosmic-purple";

  @Column(name = "DEFAULT_LANGUAGE", nullable = false)
  private String defaultLanguage = "auto";

  @Column(name = "LOGIN_MODE", nullable = false)
  private String loginMode = "SHOWCASE"; // "SHOWCASE" or "CUSTOM_BACKGROUND"

  @Lob
  @Column(name = "LOGIN_BG_URL", columnDefinition = "LONGTEXT")
  private String loginBgUrl;

  @Column(name = "LOGIN_BG_OPACITY")
  private Integer loginBgOpacity = 50; // Dimming overlay percentage (0-100)

  @Column(name = "LOGIN_HERO_TITLE")
  private String loginHeroTitle = "Autenticazione Sicura & Modulare";

  @Lob
  @Column(name = "LOGIN_HERO_SUBTITLE", columnDefinition = "TEXT")
  private String loginHeroSubtitle =
      "Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID Connect e protezione avanzata con crittografia JWT e JWE.";

  @Column(name = "FOOTER_COPYRIGHT")
  private String footerCopyright = "© 2026 Access Sphere";

  @Column(name = "PRIVACY_POLICY_URL")
  private String privacyPolicyUrl = "/privacy-policy";

  @Column(name = "COOKIE_POLICY_URL")
  private String cookiePolicyUrl = "/cookie-policy";

  @Column(name = "SUPPORT_EMAIL")
  private String supportEmail = "support@accesssphere.io";

  @Column(name = "HIDE_HOME_BUTTON")
  private Boolean hideHomeButton = false;

  /**
   * Modalità di layout della navigazione per le pagine interne autenticate: "HEADER" (Navigazione
   * orizzontale superiore predefinita) oppure "SIDEBAR" (Barra di navigazione laterale
   * espandibile/collassabile M3).
   */
  @Column(name = "NAVIGATION_LAYOUT")
  private String navigationLayout = "HEADER";

  /**
   * Mappa serializzata JSON persistita sul DB per raggruppare le impostazioni della categoria
   * "Personalizzazione Login": Include: loginMode, loginBgUrl, loginBgOpacity, loginHeroTitle,
   * loginHeroSubtitle, hideHomeButton.
   */
  @Lob
  @Column(name = "LOGIN_SETTINGS", columnDefinition = "LONGTEXT")
  private String loginSettings;

  /**
   * Mappa serializzata JSON persistita sul DB per raggruppare le impostazioni della categoria
   * "Branding & Identità": Include: appName, appTagline, logoUrl, faviconUrl, footerCopyright,
   * supportEmail.
   */
  @Lob
  @Column(name = "BRANDING_SETTINGS", columnDefinition = "LONGTEXT")
  private String brandingSettings;

  /**
   * Mappa serializzata JSON persistita sul DB per raggruppare le impostazioni della categoria
   * "Conformità & Politiche": Include: privacyPolicyUrl, cookiePolicyUrl.
   */
  @Lob
  @Column(name = "POLICY_SETTINGS", columnDefinition = "TEXT")
  private String policySettings;

  /**
   * Sincronizza le mappe di categoria prima di ogni operazione di INSERT o UPDATE sul database.
   * Assicura che i dati delle singole proprietà siano serializzati in formato JSON nelle rispettive
   * colonne raggruppate.
   */
  @PrePersist
  @PreUpdate
  public void syncCategoryMapsBeforeSave() {
    try {
      // 1. Raggruppamento Impostazioni Login
      Map<String, Object> loginMap = new LinkedHashMap<>();
      loginMap.put("loginMode", this.loginMode);
      loginMap.put("loginBgUrl", this.loginBgUrl);
      loginMap.put("loginBgOpacity", this.loginBgOpacity);
      loginMap.put("loginHeroTitle", this.loginHeroTitle);
      loginMap.put("loginHeroSubtitle", this.loginHeroSubtitle);
      loginMap.put("hideHomeButton", this.hideHomeButton);
      this.loginSettings = Mapper.writeObjectToString(loginMap);

      // 2. Raggruppamento Impostazioni Branding
      Map<String, Object> brandingMap = new LinkedHashMap<>();
      brandingMap.put("appName", this.appName);
      brandingMap.put("appTagline", this.appTagline);
      brandingMap.put("logoUrl", this.logoUrl);
      brandingMap.put("faviconUrl", this.faviconUrl);
      brandingMap.put("footerCopyright", this.footerCopyright);
      brandingMap.put("supportEmail", this.supportEmail);
      this.brandingSettings = Mapper.writeObjectToString(brandingMap);

      // 3. Raggruppamento Impostazioni Politiche & Conformità
      Map<String, Object> policyMap = new LinkedHashMap<>();
      policyMap.put("privacyPolicyUrl", this.privacyPolicyUrl);
      policyMap.put("cookiePolicyUrl", this.cookiePolicyUrl);
      this.policySettings = Mapper.writeObjectToString(policyMap);
    } catch (Exception e) {
      // Gestione di fallback silenziosa per non bloccare la persistenza standard
    }
  }

  /**
   * Sincronizza i valori delle proprietà dopo il caricamento dal database (@PostLoad). Se i campi
   * specifici sono nulli o non inizializzati, recupera i valori dalle mappe serializzate JSON.
   */
  @PostLoad
  public void syncCategoryMapsAfterLoad() {
    try {
      if (this.loginSettings != null && !this.loginSettings.isBlank()) {
        Map<String, Object> loginMap =
            Mapper.readObject(this.loginSettings, new TypeReference<>() {});
        if (loginMap != null) {
          if (this.loginMode == null && loginMap.containsKey("loginMode"))
            this.loginMode = (String) loginMap.get("loginMode");
          if (this.loginBgUrl == null && loginMap.containsKey("loginBgUrl"))
            this.loginBgUrl = (String) loginMap.get("loginBgUrl");
          if (this.loginBgOpacity == null && loginMap.containsKey("loginBgOpacity"))
            this.loginBgOpacity = (Integer) loginMap.get("loginBgOpacity");
          if (this.loginHeroTitle == null && loginMap.containsKey("loginHeroTitle"))
            this.loginHeroTitle = (String) loginMap.get("loginHeroTitle");
          if (this.loginHeroSubtitle == null && loginMap.containsKey("loginHeroSubtitle"))
            this.loginHeroSubtitle = (String) loginMap.get("loginHeroSubtitle");
          if (this.hideHomeButton == null && loginMap.containsKey("hideHomeButton"))
            this.hideHomeButton = (Boolean) loginMap.get("hideHomeButton");
        }
      }
      if (this.brandingSettings != null && !this.brandingSettings.isBlank()) {
        Map<String, Object> brandingMap =
            Mapper.readObject(this.brandingSettings, new TypeReference<>() {});
        if (brandingMap != null) {
          if (this.appName == null && brandingMap.containsKey("appName"))
            this.appName = (String) brandingMap.get("appName");
          if (this.appTagline == null && brandingMap.containsKey("appTagline"))
            this.appTagline = (String) brandingMap.get("appTagline");
          if (this.logoUrl == null && brandingMap.containsKey("logoUrl"))
            this.logoUrl = (String) brandingMap.get("logoUrl");
          if (this.faviconUrl == null && brandingMap.containsKey("faviconUrl"))
            this.faviconUrl = (String) brandingMap.get("faviconUrl");
          if (this.footerCopyright == null && brandingMap.containsKey("footerCopyright"))
            this.footerCopyright = (String) brandingMap.get("footerCopyright");
          if (this.supportEmail == null && brandingMap.containsKey("supportEmail"))
            this.supportEmail = (String) brandingMap.get("supportEmail");
        }
      }
      if (this.policySettings != null && !this.policySettings.isBlank()) {
        Map<String, Object> policyMap =
            Mapper.readObject(this.policySettings, new TypeReference<>() {});
        if (policyMap != null) {
          if (this.privacyPolicyUrl == null && policyMap.containsKey("privacyPolicyUrl"))
            this.privacyPolicyUrl = (String) policyMap.get("privacyPolicyUrl");
          if (this.cookiePolicyUrl == null && policyMap.containsKey("cookiePolicyUrl"))
            this.cookiePolicyUrl = (String) policyMap.get("cookiePolicyUrl");
        }
      }
    } catch (Exception e) {
      // Fallback silenzioso
    }
  }

  /** Ritorna la mappa dinamica deserializzata della categoria Login. */
  public Map<String, Object> getLoginSettingsMap() {
    if (this.loginSettings != null && !this.loginSettings.isBlank()) {
      try {
        return Mapper.readObject(this.loginSettings, new TypeReference<>() {});
      } catch (Exception ignored) {
      }
    }
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("loginMode", this.loginMode);
    map.put("loginBgUrl", this.loginBgUrl);
    map.put("loginBgOpacity", this.loginBgOpacity);
    map.put("loginHeroTitle", this.loginHeroTitle);
    map.put("loginHeroSubtitle", this.loginHeroSubtitle);
    map.put("hideHomeButton", this.hideHomeButton);
    return map;
  }

  /** Ritorna la mappa dinamica deserializzata della categoria Branding. */
  public Map<String, Object> getBrandingSettingsMap() {
    if (this.brandingSettings != null && !this.brandingSettings.isBlank()) {
      try {
        return Mapper.readObject(this.brandingSettings, new TypeReference<>() {});
      } catch (Exception ignored) {
      }
    }
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("appName", this.appName);
    map.put("appTagline", this.appTagline);
    map.put("logoUrl", this.logoUrl);
    map.put("faviconUrl", this.faviconUrl);
    map.put("footerCopyright", this.footerCopyright);
    map.put("supportEmail", this.supportEmail);
    return map;
  }

  /** Ritorna la mappa dinamica deserializzata della categoria Policy. */
  public Map<String, Object> getPolicySettingsMap() {
    if (this.policySettings != null && !this.policySettings.isBlank()) {
      try {
        return Mapper.readObject(this.policySettings, new TypeReference<>() {});
      } catch (Exception ignored) {
      }
    }
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("privacyPolicyUrl", this.privacyPolicyUrl);
    map.put("cookiePolicyUrl", this.cookiePolicyUrl);
    return map;
  }
}
