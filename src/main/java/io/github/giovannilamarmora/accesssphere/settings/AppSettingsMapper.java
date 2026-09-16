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

    // Branding: check flat fields first, fallback to brandingSettings map
    if (source.getAppName() != null) {
      target.setAppName(source.getAppName());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("appName") != null) {
      target.setAppName(source.getBrandingSettings().get("appName").toString());
    }

    if (source.getAppTagline() != null) {
      target.setAppTagline(source.getAppTagline());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("appTagline") != null) {
      target.setAppTagline(source.getBrandingSettings().get("appTagline").toString());
    }

    if (source.getLogoUrl() != null) {
      target.setLogoUrl(
          source.getLogoUrl().isBlank() ? "/img/logo-minimal.svg" : source.getLogoUrl());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("logoUrl") != null) {
      target.setLogoUrl(source.getBrandingSettings().get("logoUrl").toString());
    }

    if (source.getFaviconUrl() != null) {
      target.setFaviconUrl(
          source.getFaviconUrl().isBlank() ? "/img/logo-minimal.svg" : source.getFaviconUrl());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("faviconUrl") != null) {
      target.setFaviconUrl(source.getBrandingSettings().get("faviconUrl").toString());
    }

    if (source.getFooterCopyright() != null) {
      target.setFooterCopyright(source.getFooterCopyright());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("footerCopyright") != null) {
      target.setFooterCopyright(source.getBrandingSettings().get("footerCopyright").toString());
    }

    if (source.getSupportEmail() != null) {
      target.setSupportEmail(source.getSupportEmail());
    } else if (source.getBrandingSettings() != null
        && source.getBrandingSettings().get("supportEmail") != null) {
      target.setSupportEmail(source.getBrandingSettings().get("supportEmail").toString());
    }

    // Login Settings: check flat fields first, fallback to loginSettings map
    if (source.getLoginMode() != null) {
      target.setLoginMode(source.getLoginMode());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("loginMode") != null) {
      target.setLoginMode(source.getLoginSettings().get("loginMode").toString());
    }

    if (source.getLoginBgUrl() != null) {
      target.setLoginBgUrl(source.getLoginBgUrl());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("loginBgUrl") != null) {
      target.setLoginBgUrl(source.getLoginSettings().get("loginBgUrl").toString());
    }

    if (source.getLoginBgOpacity() != null) {
      target.setLoginBgOpacity(source.getLoginBgOpacity());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("loginBgOpacity") != null) {
      Object val = source.getLoginSettings().get("loginBgOpacity");
      if (val instanceof Number num) target.setLoginBgOpacity(num.intValue());
    }

    if (source.getLoginHeroTitle() != null) {
      target.setLoginHeroTitle(source.getLoginHeroTitle());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("loginHeroTitle") != null) {
      target.setLoginHeroTitle(source.getLoginSettings().get("loginHeroTitle").toString());
    }

    if (source.getLoginHeroSubtitle() != null) {
      target.setLoginHeroSubtitle(source.getLoginHeroSubtitle());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("loginHeroSubtitle") != null) {
      target.setLoginHeroSubtitle(source.getLoginSettings().get("loginHeroSubtitle").toString());
    }

    if (source.getHideHomeButton() != null) {
      target.setHideHomeButton(source.getHideHomeButton());
    } else if (source.getLoginSettings() != null
        && source.getLoginSettings().get("hideHomeButton") != null) {
      Object val = source.getLoginSettings().get("hideHomeButton");
      if (val instanceof Boolean b) target.setHideHomeButton(b);
    }

    // Policy Settings: check flat fields first, fallback to policySettings map
    if (source.getPrivacyPolicyUrl() != null) {
      target.setPrivacyPolicyUrl(source.getPrivacyPolicyUrl());
    } else if (source.getPolicySettings() != null
        && source.getPolicySettings().get("privacyPolicyUrl") != null) {
      target.setPrivacyPolicyUrl(source.getPolicySettings().get("privacyPolicyUrl").toString());
    }

    if (source.getCookiePolicyUrl() != null) {
      target.setCookiePolicyUrl(source.getCookiePolicyUrl());
    } else if (source.getPolicySettings() != null
        && source.getPolicySettings().get("cookiePolicyUrl") != null) {
      target.setCookiePolicyUrl(source.getPolicySettings().get("cookiePolicyUrl").toString());
    }

    // Explicitly synchronize category maps before persisting so Hibernate marks the entity as dirty
    target.syncCategoryMapsBeforeSave();
  }
}
