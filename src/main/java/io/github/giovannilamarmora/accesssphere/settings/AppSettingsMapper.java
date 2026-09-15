package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PublicAppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class AppSettingsMapper {

  public static PublicAppSettingsDTO toPublicDTO(AppSettingsEntity entity) {
    if (entity == null) {
      return PublicAppSettingsDTO.builder()
          .appName("Access Sphere")
          .appTagline("Gestione Identità & Accessi")
          .logoUrl("/img/logo-minimal.svg")
          .faviconUrl("/img/logo-minimal.svg")
          .activeTheme("cosmic-purple")
          .defaultLanguage("auto")
          .loginMode("SHOWCASE")
          .loginHeroTitle("Autenticazione Sicura & Modulare")
          .loginHeroSubtitle(
              "Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID Connect e protezione avanzata con crittografia JWT e JWE.")
          .footerCopyright("© 2026 Access Sphere")
          .privacyPolicyUrl("/privacy-policy")
          .cookiePolicyUrl("/cookie-policy")
          .loginBgOpacity(50)
          .hideHomeButton(false)
          .navigationLayout("HEADER")
          .build();
    }
    PublicAppSettingsDTO dto = new PublicAppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
    dto.setHideHomeButton(entity.getHideHomeButton() != null && entity.getHideHomeButton());
    dto.setNavigationLayout(
        entity.getNavigationLayout() != null ? entity.getNavigationLayout() : "HEADER");
    return dto;
  }

  public static AppSettingsDTO toDTO(AppSettingsEntity entity) {
    if (entity == null) return new AppSettingsDTO();
    AppSettingsDTO dto = new AppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
    dto.setLoginSettings(entity.getLoginSettingsMap());
    dto.setBrandingSettings(entity.getBrandingSettingsMap());
    dto.setPolicySettings(entity.getPolicySettingsMap());
    return dto;
  }

  public static AppSettingsEntity toEntity(AppSettingsDTO dto) {
    if (dto == null) return new AppSettingsEntity();
    AppSettingsEntity entity = new AppSettingsEntity();
    BeanUtils.copyProperties(dto, entity);
    return entity;
  }

  public static void updateEntity(AppSettingsEntity target, AppSettingsDTO source) {
    if (source == null || target == null) return;
    if (source.getAppName() != null) target.setAppName(source.getAppName());
    if (source.getAppTagline() != null) target.setAppTagline(source.getAppTagline());
    if (source.getLogoUrl() != null) {
      target.setLogoUrl(
          source.getLogoUrl().isBlank() ? "/img/logo-minimal.svg" : source.getLogoUrl());
    }
    if (source.getFaviconUrl() != null) {
      target.setFaviconUrl(
          source.getFaviconUrl().isBlank() ? "/img/logo-minimal.svg" : source.getFaviconUrl());
    }
    if (source.getActiveTheme() != null) target.setActiveTheme(source.getActiveTheme());
    if (source.getDefaultLanguage() != null) target.setDefaultLanguage(source.getDefaultLanguage());
    if (source.getLoginMode() != null) target.setLoginMode(source.getLoginMode());
    if (source.getLoginBgUrl() != null) target.setLoginBgUrl(source.getLoginBgUrl());
    if (source.getLoginBgOpacity() != null) target.setLoginBgOpacity(source.getLoginBgOpacity());
    if (source.getLoginHeroTitle() != null) target.setLoginHeroTitle(source.getLoginHeroTitle());
    if (source.getLoginHeroSubtitle() != null)
      target.setLoginHeroSubtitle(source.getLoginHeroSubtitle());
    if (source.getFooterCopyright() != null) target.setFooterCopyright(source.getFooterCopyright());
    if (source.getPrivacyPolicyUrl() != null)
      target.setPrivacyPolicyUrl(source.getPrivacyPolicyUrl());
    if (source.getCookiePolicyUrl() != null) target.setCookiePolicyUrl(source.getCookiePolicyUrl());
    if (source.getSupportEmail() != null) target.setSupportEmail(source.getSupportEmail());
    if (source.getHideHomeButton() != null) target.setHideHomeButton(source.getHideHomeButton());
    if (source.getNavigationLayout() != null)
      target.setNavigationLayout(source.getNavigationLayout());

    // Supporto per aggiornamento tramite mappa raggruppata "loginSettings"
    if (source.getLoginSettings() != null) {
      Map<String, Object> map = source.getLoginSettings();
      if (map.containsKey("loginMode")
          && source.getLoginMode() == null
          && map.get("loginMode") != null) target.setLoginMode(map.get("loginMode").toString());
      if (map.containsKey("loginBgUrl")
          && source.getLoginBgUrl() == null
          && map.get("loginBgUrl") != null) target.setLoginBgUrl(map.get("loginBgUrl").toString());
      if (map.containsKey("loginBgOpacity")
          && source.getLoginBgOpacity() == null
          && map.get("loginBgOpacity") != null) {
        if (map.get("loginBgOpacity") instanceof Number num)
          target.setLoginBgOpacity(num.intValue());
      }
      if (map.containsKey("loginHeroTitle")
          && source.getLoginHeroTitle() == null
          && map.get("loginHeroTitle") != null)
        target.setLoginHeroTitle(map.get("loginHeroTitle").toString());
      if (map.containsKey("loginHeroSubtitle")
          && source.getLoginHeroSubtitle() == null
          && map.get("loginHeroSubtitle") != null)
        target.setLoginHeroSubtitle(map.get("loginHeroSubtitle").toString());
      if (map.containsKey("hideHomeButton")
          && source.getHideHomeButton() == null
          && map.get("hideHomeButton") != null) {
        if (map.get("hideHomeButton") instanceof Boolean b) target.setHideHomeButton(b);
      }
    }

    // Supporto per aggiornamento tramite mappa raggruppata "brandingSettings"
    if (source.getBrandingSettings() != null) {
      Map<String, Object> map = source.getBrandingSettings();
      if (map.containsKey("appName") && source.getAppName() == null && map.get("appName") != null)
        target.setAppName(map.get("appName").toString());
      if (map.containsKey("appTagline")
          && source.getAppTagline() == null
          && map.get("appTagline") != null) target.setAppTagline(map.get("appTagline").toString());
      if (map.containsKey("logoUrl") && source.getLogoUrl() == null && map.get("logoUrl") != null)
        target.setLogoUrl(map.get("logoUrl").toString());
      if (map.containsKey("faviconUrl")
          && source.getFaviconUrl() == null
          && map.get("faviconUrl") != null) target.setFaviconUrl(map.get("faviconUrl").toString());
      if (map.containsKey("footerCopyright")
          && source.getFooterCopyright() == null
          && map.get("footerCopyright") != null)
        target.setFooterCopyright(map.get("footerCopyright").toString());
      if (map.containsKey("supportEmail")
          && source.getSupportEmail() == null
          && map.get("supportEmail") != null)
        target.setSupportEmail(map.get("supportEmail").toString());
    }

    // Supporto per aggiornamento tramite mappa raggruppata "policySettings"
    if (source.getPolicySettings() != null) {
      Map<String, Object> map = source.getPolicySettings();
      if (map.containsKey("privacyPolicyUrl")
          && source.getPrivacyPolicyUrl() == null
          && map.get("privacyPolicyUrl") != null)
        target.setPrivacyPolicyUrl(map.get("privacyPolicyUrl").toString());
      if (map.containsKey("cookiePolicyUrl")
          && source.getCookiePolicyUrl() == null
          && map.get("cookiePolicyUrl") != null)
        target.setCookiePolicyUrl(map.get("cookiePolicyUrl").toString());
    }
  }
}
