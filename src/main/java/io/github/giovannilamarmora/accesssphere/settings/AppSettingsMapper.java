package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BrandingSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.LoginSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PolicySettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import io.github.giovannilamarmora.accesssphere.settings.model.AppSettings;
import io.github.giovannilamarmora.accesssphere.settings.model.BrandingSettings;
import io.github.giovannilamarmora.accesssphere.settings.model.LoginSettings;
import io.github.giovannilamarmora.accesssphere.settings.model.PolicySettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mapper completo tra Entity relazionale (JPA), Model di dominio e DTO/Response di trasporto.
 */
@Component
public class AppSettingsMapper {

  // =========================================================================
  // Entity <-> Model (Utilizzati dal Repository)
  // =========================================================================

  public static List<AppSettings> toModelList(List<AppSettingsEntity> entities) {
    if (entities == null) return List.of();
    return entities.stream().map(AppSettingsMapper::toModel).toList();
  }

  public static AppSettings toModel(AppSettingsEntity entity) {
    if (entity == null) {
      entity = new AppSettingsEntity();
    }
    entity.syncCategoryMapsBeforeSave();

    AppSettings model = new AppSettings();
    model.setId(entity.getId());
    model.setActiveTheme(entity.getActiveTheme());
    model.setDefaultLanguage(entity.getDefaultLanguage());
    model.setNavigationLayout(
        entity.getNavigationLayout() != null ? entity.getNavigationLayout() : "HEADER");
    model.setBrandingSettings(toBrandingModel(entity.getBrandingSettings()));
    model.setLoginSettings(toLoginModel(entity.getLoginSettings()));
    model.setPolicySettings(toPolicyModel(entity.getPolicySettings()));
    return model;
  }

  public static AppSettingsEntity toEntity(AppSettings model) {
    if (model == null) {
      return new AppSettingsEntity();
    }
    AppSettingsEntity entity = new AppSettingsEntity();
    updateEntity(entity, model);
    return entity;
  }

  public static void updateEntity(AppSettingsEntity target, AppSettings source) {
    if (source == null || target == null) return;

    if (source.getId() != null) {
      target.setId(source.getId());
    }
    if (source.getActiveTheme() != null) {
      target.setActiveTheme(source.getActiveTheme());
    }
    if (source.getDefaultLanguage() != null) {
      target.setDefaultLanguage(source.getDefaultLanguage());
    }
    if (source.getNavigationLayout() != null) {
      target.setNavigationLayout(source.getNavigationLayout());
    }

    if (source.getBrandingSettings() != null) {
      BrandingSettings bModel = source.getBrandingSettings();
      Map<String, Object> brandingMap =
          target.getBrandingSettings() != null
              ? new LinkedHashMap<>(target.getBrandingSettings())
              : AppSettingsEntity.defaultBrandingSettings();

      if (bModel.getAppName() != null) brandingMap.put("appName", bModel.getAppName());
      if (bModel.getAppTagline() != null) brandingMap.put("appTagline", bModel.getAppTagline());
      if (bModel.getLogoUrl() != null) {
        brandingMap.put(
            "logoUrl",
            bModel.getLogoUrl().isBlank() ? "/img/logo-minimal.svg" : bModel.getLogoUrl());
      }
      if (bModel.getFaviconUrl() != null) {
        brandingMap.put(
            "faviconUrl",
            bModel.getFaviconUrl().isBlank() ? "/img/logo-minimal.svg" : bModel.getFaviconUrl());
      }
      if (bModel.getFooterCopyright() != null) {
        brandingMap.put("footerCopyright", bModel.getFooterCopyright());
      }
      if (bModel.getSupportEmail() != null) {
        brandingMap.put("supportEmail", bModel.getSupportEmail());
      }
      target.setBrandingSettings(brandingMap);
    }

    if (source.getLoginSettings() != null) {
      LoginSettings lModel = source.getLoginSettings();
      Map<String, Object> loginMap =
          target.getLoginSettings() != null
              ? new LinkedHashMap<>(target.getLoginSettings())
              : AppSettingsEntity.defaultLoginSettings();

      if (lModel.getLoginMode() != null) loginMap.put("loginMode", lModel.getLoginMode());
      if (lModel.getLoginBgUrl() != null) loginMap.put("loginBgUrl", lModel.getLoginBgUrl());
      if (lModel.getLoginBgOpacity() != null) loginMap.put("loginBgOpacity", lModel.getLoginBgOpacity());
      if (lModel.getLoginHeroTitle() != null) loginMap.put("loginHeroTitle", lModel.getLoginHeroTitle());
      if (lModel.getLoginHeroSubtitle() != null) loginMap.put("loginHeroSubtitle", lModel.getLoginHeroSubtitle());
      if (lModel.getHideHomeButton() != null) loginMap.put("hideHomeButton", lModel.getHideHomeButton());
      target.setLoginSettings(loginMap);
    }

    if (source.getPolicySettings() != null) {
      PolicySettings pModel = source.getPolicySettings();
      Map<String, Object> policyMap =
          target.getPolicySettings() != null
              ? new LinkedHashMap<>(target.getPolicySettings())
              : AppSettingsEntity.defaultPolicySettings();

      if (pModel.getPrivacyPolicyUrl() != null) policyMap.put("privacyPolicyUrl", pModel.getPrivacyPolicyUrl());
      if (pModel.getCookiePolicyUrl() != null) policyMap.put("cookiePolicyUrl", pModel.getCookiePolicyUrl());
      target.setPolicySettings(policyMap);
    }

    target.syncCategoryMapsBeforeSave();
  }

  // =========================================================================
  // Model <-> DTO (Utilizzati dal Service e Controller)
  // =========================================================================

  public static AppSettingsDTO toDTO(AppSettings model) {
    if (model == null) return new AppSettingsDTO();
    AppSettingsDTO dto = new AppSettingsDTO();
    dto.setId(model.getId());
    dto.setActiveTheme(model.getActiveTheme());
    dto.setDefaultLanguage(model.getDefaultLanguage());
    dto.setNavigationLayout(model.getNavigationLayout());
    if (model.getBrandingSettings() != null) {
      dto.setBrandingSettings(toBrandingDTO(model.getBrandingSettings()));
    }
    if (model.getLoginSettings() != null) {
      dto.setLoginSettings(toLoginDTO(model.getLoginSettings()));
    }
    if (model.getPolicySettings() != null) {
      dto.setPolicySettings(toPolicyDTO(model.getPolicySettings()));
    }
    return dto;
  }

  public static AppSettings toModel(AppSettingsDTO dto) {
    if (dto == null) return new AppSettings();
    AppSettings model = new AppSettings();
    model.setId(dto.getId());
    model.setActiveTheme(dto.getActiveTheme());
    model.setDefaultLanguage(dto.getDefaultLanguage());
    model.setNavigationLayout(dto.getNavigationLayout());
    if (dto.getBrandingSettings() != null) {
      model.setBrandingSettings(toBrandingModel(dto.getBrandingSettings()));
    }
    if (dto.getLoginSettings() != null) {
      model.setLoginSettings(toLoginModel(dto.getLoginSettings()));
    }
    if (dto.getPolicySettings() != null) {
      model.setPolicySettings(toPolicyModel(dto.getPolicySettings()));
    }
    return model;
  }

  public static AppSettingsDTO toDTO(AppSettingsEntity entity) {
    return toDTO(toModel(entity));
  }

  public static AppSettingsEntity toEntity(AppSettingsDTO dto) {
    return toEntity(toModel(dto));
  }

  public static void updateEntity(AppSettingsEntity target, AppSettingsDTO source) {
    updateEntity(target, toModel(source));
  }

  // =========================================================================
  // Helper di Categoria Model / DTO / Map
  // =========================================================================

  public static BrandingSettings toBrandingModel(Map<String, Object> map) {
    if (map == null) return new BrandingSettings();
    BrandingSettings model = new BrandingSettings();
    if (map.containsKey("appName") && map.get("appName") != null) model.setAppName(map.get("appName").toString());
    if (map.containsKey("appTagline") && map.get("appTagline") != null) model.setAppTagline(map.get("appTagline").toString());
    if (map.containsKey("logoUrl") && map.get("logoUrl") != null) model.setLogoUrl(map.get("logoUrl").toString());
    if (map.containsKey("faviconUrl") && map.get("faviconUrl") != null) model.setFaviconUrl(map.get("faviconUrl").toString());
    if (map.containsKey("footerCopyright") && map.get("footerCopyright") != null) model.setFooterCopyright(map.get("footerCopyright").toString());
    if (map.containsKey("supportEmail") && map.get("supportEmail") != null) model.setSupportEmail(map.get("supportEmail").toString());
    return model;
  }

  public static LoginSettings toLoginModel(Map<String, Object> map) {
    if (map == null) return new LoginSettings();
    LoginSettings model = new LoginSettings();
    if (map.containsKey("loginMode") && map.get("loginMode") != null) model.setLoginMode(map.get("loginMode").toString());
    if (map.containsKey("loginBgUrl")) {
      Object val = map.get("loginBgUrl");
      model.setLoginBgUrl(val != null ? val.toString() : null);
    }
    if (map.containsKey("loginBgOpacity")) {
      Object val = map.get("loginBgOpacity");
      if (val instanceof Number num) {
        model.setLoginBgOpacity(num.intValue());
      } else if (val != null) {
        try {
          model.setLoginBgOpacity(Integer.parseInt(val.toString()));
        } catch (NumberFormatException ignored) {}
      }
    }
    if (map.containsKey("loginHeroTitle") && map.get("loginHeroTitle") != null) model.setLoginHeroTitle(map.get("loginHeroTitle").toString());
    if (map.containsKey("loginHeroSubtitle") && map.get("loginHeroSubtitle") != null) model.setLoginHeroSubtitle(map.get("loginHeroSubtitle").toString());
    if (map.containsKey("hideHomeButton")) {
      Object val = map.get("hideHomeButton");
      if (val instanceof Boolean b) {
        model.setHideHomeButton(b);
      } else if (val != null) {
        model.setHideHomeButton(Boolean.parseBoolean(val.toString()));
      }
    }
    return model;
  }

  public static PolicySettings toPolicyModel(Map<String, Object> map) {
    if (map == null) return new PolicySettings();
    PolicySettings model = new PolicySettings();
    if (map.containsKey("privacyPolicyUrl") && map.get("privacyPolicyUrl") != null) model.setPrivacyPolicyUrl(map.get("privacyPolicyUrl").toString());
    if (map.containsKey("cookiePolicyUrl") && map.get("cookiePolicyUrl") != null) model.setCookiePolicyUrl(map.get("cookiePolicyUrl").toString());
    return model;
  }

  public static BrandingSettingsDTO toBrandingDTO(BrandingSettings model) {
    if (model == null) return new BrandingSettingsDTO();
    return BrandingSettingsDTO.builder()
        .appName(model.getAppName())
        .appTagline(model.getAppTagline())
        .logoUrl(model.getLogoUrl())
        .faviconUrl(model.getFaviconUrl())
        .footerCopyright(model.getFooterCopyright())
        .supportEmail(model.getSupportEmail())
        .build();
  }

  public static BrandingSettings toBrandingModel(BrandingSettingsDTO dto) {
    if (dto == null) return new BrandingSettings();
    return BrandingSettings.builder()
        .appName(dto.getAppName())
        .appTagline(dto.getAppTagline())
        .logoUrl(dto.getLogoUrl())
        .faviconUrl(dto.getFaviconUrl())
        .footerCopyright(dto.getFooterCopyright())
        .supportEmail(dto.getSupportEmail())
        .build();
  }

  public static LoginSettingsDTO toLoginDTO(LoginSettings model) {
    if (model == null) return new LoginSettingsDTO();
    return LoginSettingsDTO.builder()
        .loginMode(model.getLoginMode())
        .loginBgUrl(model.getLoginBgUrl())
        .loginBgOpacity(model.getLoginBgOpacity())
        .loginHeroTitle(model.getLoginHeroTitle())
        .loginHeroSubtitle(model.getLoginHeroSubtitle())
        .hideHomeButton(model.getHideHomeButton())
        .build();
  }

  public static LoginSettings toLoginModel(LoginSettingsDTO dto) {
    if (dto == null) return new LoginSettings();
    return LoginSettings.builder()
        .loginMode(dto.getLoginMode())
        .loginBgUrl(dto.getLoginBgUrl())
        .loginBgOpacity(dto.getLoginBgOpacity())
        .loginHeroTitle(dto.getLoginHeroTitle())
        .loginHeroSubtitle(dto.getLoginHeroSubtitle())
        .hideHomeButton(dto.getHideHomeButton())
        .build();
  }

  public static PolicySettingsDTO toPolicyDTO(PolicySettings model) {
    if (model == null) return new PolicySettingsDTO();
    return PolicySettingsDTO.builder()
        .privacyPolicyUrl(model.getPrivacyPolicyUrl())
        .cookiePolicyUrl(model.getCookiePolicyUrl())
        .build();
  }

  public static PolicySettings toPolicyModel(PolicySettingsDTO dto) {
    if (dto == null) return new PolicySettings();
    return PolicySettings.builder()
        .privacyPolicyUrl(dto.getPrivacyPolicyUrl())
        .cookiePolicyUrl(dto.getCookiePolicyUrl())
        .build();
  }

  public static BrandingSettingsDTO toBrandingDTO(Map<String, Object> map) {
    return toBrandingDTO(toBrandingModel(map));
  }

  public static LoginSettingsDTO toLoginDTO(Map<String, Object> map) {
    return toLoginDTO(toLoginModel(map));
  }

  public static PolicySettingsDTO toPolicyDTO(Map<String, Object> map) {
    return toPolicyDTO(toPolicyModel(map));
  }
}
