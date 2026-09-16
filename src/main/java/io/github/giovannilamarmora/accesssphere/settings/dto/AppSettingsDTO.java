package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object unificato per la gestione delle impostazioni dell'applicazione.
 * Raggruppa i dati in oggetti tipizzati per categoria (Branding, Login, Policy)
 * eliminando le proprietà piatte duplicate a livello root.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppSettingsDTO {
  private Long id;
  private String activeTheme;
  private String defaultLanguage;
  private String navigationLayout;

  // Categorie di impostazioni tipizzate
  private BrandingSettingsDTO brandingSettings;
  private LoginSettingsDTO loginSettings;
  private PolicySettingsDTO policySettings;

  @JsonIgnore
  public void setBrandingSettings(java.util.Map<String, Object> map) {
    this.brandingSettings = io.github.giovannilamarmora.accesssphere.settings.AppSettingsMapper.toBrandingDTO(map);
  }

  public void setBrandingSettings(BrandingSettingsDTO brandingSettings) {
    this.brandingSettings = brandingSettings;
  }

  @JsonIgnore
  public void setLoginSettings(java.util.Map<String, Object> map) {
    this.loginSettings = io.github.giovannilamarmora.accesssphere.settings.AppSettingsMapper.toLoginDTO(map);
  }

  public void setLoginSettings(LoginSettingsDTO loginSettings) {
    this.loginSettings = loginSettings;
  }

  @JsonIgnore
  public void setPolicySettings(java.util.Map<String, Object> map) {
    this.policySettings = io.github.giovannilamarmora.accesssphere.settings.AppSettingsMapper.toPolicyDTO(map);
  }

  public void setPolicySettings(PolicySettingsDTO policySettings) {
    this.policySettings = policySettings;
  }

  // Helper delegati retrocompatibili annotati con @JsonIgnore per non serializzare campi piatti ridondanti nel JSON

  @JsonIgnore
  public String getAppName() {
    return brandingSettings != null ? brandingSettings.getAppName() : null;
  }

  @JsonIgnore
  public void setAppName(String appName) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setAppName(appName);
  }

  @JsonIgnore
  public String getAppTagline() {
    return brandingSettings != null ? brandingSettings.getAppTagline() : null;
  }

  @JsonIgnore
  public void setAppTagline(String appTagline) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setAppTagline(appTagline);
  }

  @JsonIgnore
  public String getLogoUrl() {
    return brandingSettings != null ? brandingSettings.getLogoUrl() : null;
  }

  @JsonIgnore
  public void setLogoUrl(String logoUrl) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setLogoUrl(logoUrl);
  }

  @JsonIgnore
  public String getFaviconUrl() {
    return brandingSettings != null ? brandingSettings.getFaviconUrl() : null;
  }

  @JsonIgnore
  public void setFaviconUrl(String faviconUrl) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setFaviconUrl(faviconUrl);
  }

  @JsonIgnore
  public String getFooterCopyright() {
    return brandingSettings != null ? brandingSettings.getFooterCopyright() : null;
  }

  @JsonIgnore
  public void setFooterCopyright(String footerCopyright) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setFooterCopyright(footerCopyright);
  }

  @JsonIgnore
  public String getSupportEmail() {
    return brandingSettings != null ? brandingSettings.getSupportEmail() : null;
  }

  @JsonIgnore
  public void setSupportEmail(String supportEmail) {
    if (this.brandingSettings == null) this.brandingSettings = new BrandingSettingsDTO();
    this.brandingSettings.setSupportEmail(supportEmail);
  }

  @JsonIgnore
  public String getLoginMode() {
    return loginSettings != null ? loginSettings.getLoginMode() : null;
  }

  @JsonIgnore
  public void setLoginMode(String loginMode) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setLoginMode(loginMode);
  }

  @JsonIgnore
  public String getLoginBgUrl() {
    return loginSettings != null ? loginSettings.getLoginBgUrl() : null;
  }

  @JsonIgnore
  public void setLoginBgUrl(String loginBgUrl) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setLoginBgUrl(loginBgUrl);
  }

  @JsonIgnore
  public Integer getLoginBgOpacity() {
    return loginSettings != null ? loginSettings.getLoginBgOpacity() : null;
  }

  @JsonIgnore
  public void setLoginBgOpacity(Integer loginBgOpacity) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setLoginBgOpacity(loginBgOpacity);
  }

  @JsonIgnore
  public String getLoginHeroTitle() {
    return loginSettings != null ? loginSettings.getLoginHeroTitle() : null;
  }

  @JsonIgnore
  public void setLoginHeroTitle(String loginHeroTitle) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setLoginHeroTitle(loginHeroTitle);
  }

  @JsonIgnore
  public String getLoginHeroSubtitle() {
    return loginSettings != null ? loginSettings.getLoginHeroSubtitle() : null;
  }

  @JsonIgnore
  public void setLoginHeroSubtitle(String loginHeroSubtitle) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setLoginHeroSubtitle(loginHeroSubtitle);
  }

  @JsonIgnore
  public Boolean getHideHomeButton() {
    return loginSettings != null ? loginSettings.getHideHomeButton() : null;
  }

  @JsonIgnore
  public void setHideHomeButton(Boolean hideHomeButton) {
    if (this.loginSettings == null) this.loginSettings = new LoginSettingsDTO();
    this.loginSettings.setHideHomeButton(hideHomeButton);
  }

  @JsonIgnore
  public String getPrivacyPolicyUrl() {
    return policySettings != null ? policySettings.getPrivacyPolicyUrl() : null;
  }

  @JsonIgnore
  public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
    if (this.policySettings == null) this.policySettings = new PolicySettingsDTO();
    this.policySettings.setPrivacyPolicyUrl(privacyPolicyUrl);
  }

  @JsonIgnore
  public String getCookiePolicyUrl() {
    return policySettings != null ? policySettings.getCookiePolicyUrl() : null;
  }

  @JsonIgnore
  public void setCookiePolicyUrl(String cookiePolicyUrl) {
    if (this.policySettings == null) this.policySettings = new PolicySettingsDTO();
    this.policySettings.setCookiePolicyUrl(cookiePolicyUrl);
  }
}
