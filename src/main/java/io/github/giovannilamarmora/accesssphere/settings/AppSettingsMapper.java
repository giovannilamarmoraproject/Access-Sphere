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
      entity = new AppSettingsEntity();
    }
    entity.syncCategoryMapsBeforeSave();

    PublicAppSettingsDTO dto = new PublicAppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
    dto.setHideHomeButton(entity.getHideHomeButton() != null && entity.getHideHomeButton());
    dto.setNavigationLayout(
        entity.getNavigationLayout() != null ? entity.getNavigationLayout() : "HEADER");
    dto.setLoginSettings(entity.getLoginSettingsMap());
    dto.setBrandingSettings(entity.getBrandingSettingsMap());
    dto.setPolicySettings(entity.getPolicySettingsMap());
    return dto;
  }

  public static AppSettingsDTO toDTO(AppSettingsEntity entity) {
    if (entity == null) return new AppSettingsDTO();
    entity.syncCategoryMapsBeforeSave();

    AppSettingsDTO dto = new AppSettingsDTO();
    BeanUtils.copyProperties(entity, dto);
    dto.setHideHomeButton(entity.getHideHomeButton() != null && entity.getHideHomeButton());
    dto.setNavigationLayout(
        entity.getNavigationLayout() != null ? entity.getNavigationLayout() : "HEADER");
    dto.setLoginSettings(entity.getLoginSettingsMap());
    dto.setBrandingSettings(entity.getBrandingSettingsMap());
    dto.setPolicySettings(entity.getPolicySettingsMap());
    return dto;
  }

  public static AppSettingsEntity toEntity(AppSettingsDTO dto) {
    if (dto == null) return new AppSettingsEntity();
    AppSettingsEntity entity = new AppSettingsEntity();
    updateEntity(entity, dto);
    return entity;
  }

  public static void updateEntity(AppSettingsEntity target, AppSettingsDTO source) {
    if (source == null || target == null) return;

    if (source.getActiveTheme() != null) target.setActiveTheme(source.getActiveTheme());
    if (source.getDefaultLanguage() != null) target.setDefaultLanguage(source.getDefaultLanguage());
    if (source.getNavigationLayout() != null)
      target.setNavigationLayout(source.getNavigationLayout());

    // 1. Branding Settings: priorità alla mappa categorizzata brandingSettings
    if (source.getBrandingSettings() != null) {
      Map<String, Object> branding = source.getBrandingSettings();
      if (branding.containsKey("appName") && branding.get("appName") != null) {
        target.setAppName(branding.get("appName").toString());
      }
      if (branding.containsKey("appTagline")) {
        Object val = branding.get("appTagline");
        target.setAppTagline(val != null ? val.toString() : null);
      }
      if (branding.containsKey("logoUrl")) {
        Object val = branding.get("logoUrl");
        target.setLogoUrl(
            val != null && !val.toString().isBlank() ? val.toString() : "/img/logo-minimal.svg");
      }
      if (branding.containsKey("faviconUrl")) {
        Object val = branding.get("faviconUrl");
        target.setFaviconUrl(
            val != null && !val.toString().isBlank() ? val.toString() : "/img/logo-minimal.svg");
      }
      if (branding.containsKey("footerCopyright")) {
        Object val = branding.get("footerCopyright");
        target.setFooterCopyright(val != null ? val.toString() : null);
      }
      if (branding.containsKey("supportEmail")) {
        Object val = branding.get("supportEmail");
        target.setSupportEmail(val != null ? val.toString() : null);
      }
      target.getBrandingSettings().putAll(branding);
    }
    // Fallback retrocompatibilità per campi flat di branding
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
    if (source.getFooterCopyright() != null) target.setFooterCopyright(source.getFooterCopyright());
    if (source.getSupportEmail() != null) target.setSupportEmail(source.getSupportEmail());

    // 2. Login Settings: priorità alla mappa categorizzata loginSettings
    if (source.getLoginSettings() != null) {
      Map<String, Object> login = source.getLoginSettings();
      if (login.containsKey("loginMode") && login.get("loginMode") != null) {
        target.setLoginMode(login.get("loginMode").toString());
      }
      if (login.containsKey("loginBgUrl")) {
        Object val = login.get("loginBgUrl");
        target.setLoginBgUrl(val != null ? val.toString() : null);
      }
      if (login.containsKey("loginBgOpacity")) {
        Object val = login.get("loginBgOpacity");
        if (val instanceof Number num) {
          target.setLoginBgOpacity(num.intValue());
        } else if (val != null) {
          try {
            target.setLoginBgOpacity(Integer.parseInt(val.toString()));
          } catch (NumberFormatException ignored) {
          }
        }
      }
      if (login.containsKey("loginHeroTitle")) {
        Object val = login.get("loginHeroTitle");
        target.setLoginHeroTitle(val != null ? val.toString() : null);
      }
      if (login.containsKey("loginHeroSubtitle")) {
        Object val = login.get("loginHeroSubtitle");
        target.setLoginHeroSubtitle(val != null ? val.toString() : null);
      }
      if (login.containsKey("hideHomeButton")) {
        Object val = login.get("hideHomeButton");
        if (val instanceof Boolean b) {
          target.setHideHomeButton(b);
        } else if (val != null) {
          target.setHideHomeButton(Boolean.parseBoolean(val.toString()));
        }
      }
      target.getLoginSettings().putAll(login);
    }
    // Fallback retrocompatibilità per campi flat di login
    if (source.getLoginMode() != null) target.setLoginMode(source.getLoginMode());
    if (source.getLoginBgUrl() != null) target.setLoginBgUrl(source.getLoginBgUrl());
    if (source.getLoginBgOpacity() != null) target.setLoginBgOpacity(source.getLoginBgOpacity());
    if (source.getLoginHeroTitle() != null) target.setLoginHeroTitle(source.getLoginHeroTitle());
    if (source.getLoginHeroSubtitle() != null)
      target.setLoginHeroSubtitle(source.getLoginHeroSubtitle());
    if (source.getHideHomeButton() != null) target.setHideHomeButton(source.getHideHomeButton());

    // 3. Policy Settings: priorità alla mappa categorizzata policySettings
    if (source.getPolicySettings() != null) {
      Map<String, Object> policy = source.getPolicySettings();
      if (policy.containsKey("privacyPolicyUrl")) {
        Object val = policy.get("privacyPolicyUrl");
        target.setPrivacyPolicyUrl(val != null ? val.toString() : null);
      }
      if (policy.containsKey("cookiePolicyUrl")) {
        Object val = policy.get("cookiePolicyUrl");
        target.setCookiePolicyUrl(val != null ? val.toString() : null);
      }
      target.getPolicySettings().putAll(policy);
    }
    // Fallback retrocompatibilità per campi flat di policy
    if (source.getPrivacyPolicyUrl() != null)
      target.setPrivacyPolicyUrl(source.getPrivacyPolicyUrl());
    if (source.getCookiePolicyUrl() != null) target.setCookiePolicyUrl(source.getCookiePolicyUrl());

    // Sincronizza le mappe di categoria per il salvataggio su DB
    target.syncCategoryMapsBeforeSave();
  }
}
