package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PublicAppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
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
          .loginHeroSubtitle("Piattaforma moderna per la gestione delle identità (IAM), federazione OAuth 2.0 / OpenID Connect e protezione avanzata con crittografia JWT e JWE.")
          .footerCopyright("© 2026 Access Sphere")
          .privacyPolicyUrl("/privacy-policy")
          .cookiePolicyUrl("/cookie-policy")
          .loginBgOpacity(50)
          .build();
    }
    PublicAppSettingsDTO dto = new PublicAppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
    return dto;
  }

  public static AppSettingsDTO toDTO(AppSettingsEntity entity) {
    if (entity == null) return new AppSettingsDTO();
    AppSettingsDTO dto = new AppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
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
    if (source.getLogoUrl() != null) target.setLogoUrl(source.getLogoUrl());
    if (source.getFaviconUrl() != null) target.setFaviconUrl(source.getFaviconUrl());
    if (source.getActiveTheme() != null) target.setActiveTheme(source.getActiveTheme());
    if (source.getDefaultLanguage() != null) target.setDefaultLanguage(source.getDefaultLanguage());
    if (source.getLoginMode() != null) target.setLoginMode(source.getLoginMode());
    if (source.getLoginBgUrl() != null) target.setLoginBgUrl(source.getLoginBgUrl());
    if (source.getLoginBgOpacity() != null) target.setLoginBgOpacity(source.getLoginBgOpacity());
    if (source.getLoginHeroTitle() != null) target.setLoginHeroTitle(source.getLoginHeroTitle());
    if (source.getLoginHeroSubtitle() != null) target.setLoginHeroSubtitle(source.getLoginHeroSubtitle());
    if (source.getFooterCopyright() != null) target.setFooterCopyright(source.getFooterCopyright());
    if (source.getPrivacyPolicyUrl() != null) target.setPrivacyPolicyUrl(source.getPrivacyPolicyUrl());
    if (source.getCookiePolicyUrl() != null) target.setCookiePolicyUrl(source.getCookiePolicyUrl());
    if (source.getSupportEmail() != null) target.setSupportEmail(source.getSupportEmail());
  }
}
