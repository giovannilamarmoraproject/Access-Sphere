package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppSettingsDTO {
  private Long id;
  private String appName;
  private String appTagline;
  private String logoUrl;
  private String faviconUrl;
  private String activeTheme;
  private String defaultLanguage;
  private String loginMode;
  private String loginBgUrl;
  private Integer loginBgOpacity;
  private String loginHeroTitle;
  private String loginHeroSubtitle;
  private String footerCopyright;
  private String privacyPolicyUrl;
  private String cookiePolicyUrl;
  private String supportEmail;
}
