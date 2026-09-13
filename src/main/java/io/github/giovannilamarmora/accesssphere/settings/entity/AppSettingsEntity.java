package io.github.giovannilamarmora.accesssphere.settings.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
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
}
