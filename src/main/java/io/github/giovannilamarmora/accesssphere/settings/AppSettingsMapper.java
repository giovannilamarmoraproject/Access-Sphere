package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.dto.AppSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.BrandingSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.LoginSettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.dto.PolicySettingsDTO;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mapper bidirezionale tra AppSettingsEntity e AppSettingsDTO.
 * Gestisce la conversione tra le mappe serializzate JSON persistite a DB
 * e gli oggetti DTO tipizzati per categoria (Branding, Login, Policy).
 */
@Component
public class AppSettingsMapper {

  /**
   * Converte un'entità AppSettingsEntity nel DTO tipizzato AppSettingsDTO.
   *
   * @param entity entità da convertire
   * @return AppSettingsDTO popolato con categorie tipizzate
   */
  public static AppSettingsDTO toDTO(AppSettingsEntity entity) {
    if (entity == null) {
      entity = new AppSettingsEntity();
    }
    entity.syncCategoryMapsBeforeSave();

    AppSettingsDTO dto = new AppSettingsDTO();
    dto.setId(entity.getId());
    dto.setActiveTheme(entity.getActiveTheme());
    dto.setDefaultLanguage(entity.getDefaultLanguage());
    dto.setNavigationLayout(
        entity.getNavigationLayout() != null ? entity.getNavigationLayout() : "HEADER");
    dto.setBrandingSettings(toBrandingDTO(entity.getBrandingSettings()));
    dto.setLoginSettings(toLoginDTO(entity.getLoginSettings()));
    dto.setPolicySettings(toPolicyDTO(entity.getPolicySettings()));
    return dto;
  }

  /**
   * Crea una nuova AppSettingsEntity a partire da un AppSettingsDTO.
   *
   * @param dto DTO sorgente
   * @return nuova istanza di AppSettingsEntity
   */
  public static AppSettingsEntity toEntity(AppSettingsDTO dto) {
    if (dto == null) {
      return new AppSettingsEntity();
    }
    AppSettingsEntity entity = new AppSettingsEntity();
    updateEntity(entity, dto);
    return entity;
  }

  /**
   * Aggiorna i campi di un'entità target con i dati provenienti da AppSettingsDTO.
   * Trasforma gli oggetti tipizzati (Branding, Login, Policy) nelle corrispondenti mappe
   * serializzabili sul database.
   *
   * @param target entità da aggiornare
   * @param source DTO con i nuovi valori
   */
  public static void updateEntity(AppSettingsEntity target, AppSettingsDTO source) {
    if (source == null || target == null) return;

    if (source.getActiveTheme() != null) {
      target.setActiveTheme(source.getActiveTheme());
    }
    if (source.getDefaultLanguage() != null) {
      target.setDefaultLanguage(source.getDefaultLanguage());
    }
    if (source.getNavigationLayout() != null) {
      target.setNavigationLayout(source.getNavigationLayout());
    }

    // 1. Aggiornamento Branding Settings
    if (source.getBrandingSettings() != null) {
      BrandingSettingsDTO bDto = source.getBrandingSettings();
      Map<String, Object> brandingMap =
          target.getBrandingSettings() != null
              ? new LinkedHashMap<>(target.getBrandingSettings())
              : AppSettingsEntity.defaultBrandingSettings();

      if (bDto.getAppName() != null) {
        brandingMap.put("appName", bDto.getAppName());
      }
      if (bDto.getAppTagline() != null) {
        brandingMap.put("appTagline", bDto.getAppTagline());
      }
      if (bDto.getLogoUrl() != null) {
        brandingMap.put(
            "logoUrl",
            bDto.getLogoUrl().isBlank() ? "/img/logo-minimal.svg" : bDto.getLogoUrl());
      }
      if (bDto.getFaviconUrl() != null) {
        brandingMap.put(
            "faviconUrl",
            bDto.getFaviconUrl().isBlank() ? "/img/logo-minimal.svg" : bDto.getFaviconUrl());
      }
      if (bDto.getFooterCopyright() != null) {
        brandingMap.put("footerCopyright", bDto.getFooterCopyright());
      }
      if (bDto.getSupportEmail() != null) {
        brandingMap.put("supportEmail", bDto.getSupportEmail());
      }
      target.setBrandingSettings(brandingMap);
    }

    // 2. Aggiornamento Login Settings
    if (source.getLoginSettings() != null) {
      LoginSettingsDTO lDto = source.getLoginSettings();
      Map<String, Object> loginMap =
          target.getLoginSettings() != null
              ? new LinkedHashMap<>(target.getLoginSettings())
              : AppSettingsEntity.defaultLoginSettings();

      if (lDto.getLoginMode() != null) {
        loginMap.put("loginMode", lDto.getLoginMode());
      }
      if (lDto.getLoginBgUrl() != null) {
        loginMap.put("loginBgUrl", lDto.getLoginBgUrl());
      }
      if (lDto.getLoginBgOpacity() != null) {
        loginMap.put("loginBgOpacity", lDto.getLoginBgOpacity());
      }
      if (lDto.getLoginHeroTitle() != null) {
        loginMap.put("loginHeroTitle", lDto.getLoginHeroTitle());
      }
      if (lDto.getLoginHeroSubtitle() != null) {
        loginMap.put("loginHeroSubtitle", lDto.getLoginHeroSubtitle());
      }
      if (lDto.getHideHomeButton() != null) {
        loginMap.put("hideHomeButton", lDto.getHideHomeButton());
      }
      target.setLoginSettings(loginMap);
    }

    // 3. Aggiornamento Policy Settings
    if (source.getPolicySettings() != null) {
      PolicySettingsDTO pDto = source.getPolicySettings();
      Map<String, Object> policyMap =
          target.getPolicySettings() != null
              ? new LinkedHashMap<>(target.getPolicySettings())
              : AppSettingsEntity.defaultPolicySettings();

      if (pDto.getPrivacyPolicyUrl() != null) {
        policyMap.put("privacyPolicyUrl", pDto.getPrivacyPolicyUrl());
      }
      if (pDto.getCookiePolicyUrl() != null) {
        policyMap.put("cookiePolicyUrl", pDto.getCookiePolicyUrl());
      }
      target.setPolicySettings(policyMap);
    }

    target.syncCategoryMapsBeforeSave();
  }

  // =========================================================================
  // Metodi helper per conversione Map <-> DTO
  // =========================================================================

  public static BrandingSettingsDTO toBrandingDTO(Map<String, Object> map) {
    if (map == null) return new BrandingSettingsDTO();
    BrandingSettingsDTO dto = new BrandingSettingsDTO();
    if (map.containsKey("appName") && map.get("appName") != null) {
      dto.setAppName(map.get("appName").toString());
    }
    if (map.containsKey("appTagline") && map.get("appTagline") != null) {
      dto.setAppTagline(map.get("appTagline").toString());
    }
    if (map.containsKey("logoUrl") && map.get("logoUrl") != null) {
      dto.setLogoUrl(map.get("logoUrl").toString());
    }
    if (map.containsKey("faviconUrl") && map.get("faviconUrl") != null) {
      dto.setFaviconUrl(map.get("faviconUrl").toString());
    }
    if (map.containsKey("footerCopyright") && map.get("footerCopyright") != null) {
      dto.setFooterCopyright(map.get("footerCopyright").toString());
    }
    if (map.containsKey("supportEmail") && map.get("supportEmail") != null) {
      dto.setSupportEmail(map.get("supportEmail").toString());
    }
    return dto;
  }

  public static LoginSettingsDTO toLoginDTO(Map<String, Object> map) {
    if (map == null) return new LoginSettingsDTO();
    LoginSettingsDTO dto = new LoginSettingsDTO();
    if (map.containsKey("loginMode") && map.get("loginMode") != null) {
      dto.setLoginMode(map.get("loginMode").toString());
    }
    if (map.containsKey("loginBgUrl")) {
      Object val = map.get("loginBgUrl");
      dto.setLoginBgUrl(val != null ? val.toString() : null);
    }
    if (map.containsKey("loginBgOpacity")) {
      Object val = map.get("loginBgOpacity");
      if (val instanceof Number num) {
        dto.setLoginBgOpacity(num.intValue());
      } else if (val != null) {
        try {
          dto.setLoginBgOpacity(Integer.parseInt(val.toString()));
        } catch (NumberFormatException ignored) {}
      }
    }
    if (map.containsKey("loginHeroTitle") && map.get("loginHeroTitle") != null) {
      dto.setLoginHeroTitle(map.get("loginHeroTitle").toString());
    }
    if (map.containsKey("loginHeroSubtitle") && map.get("loginHeroSubtitle") != null) {
      dto.setLoginHeroSubtitle(map.get("loginHeroSubtitle").toString());
    }
    if (map.containsKey("hideHomeButton")) {
      Object val = map.get("hideHomeButton");
      if (val instanceof Boolean b) {
        dto.setHideHomeButton(b);
      } else if (val != null) {
        dto.setHideHomeButton(Boolean.parseBoolean(val.toString()));
      }
    }
    return dto;
  }

  public static PolicySettingsDTO toPolicyDTO(Map<String, Object> map) {
    if (map == null) return new PolicySettingsDTO();
    PolicySettingsDTO dto = new PolicySettingsDTO();
    if (map.containsKey("privacyPolicyUrl") && map.get("privacyPolicyUrl") != null) {
      dto.setPrivacyPolicyUrl(map.get("privacyPolicyUrl").toString());
    }
    if (map.containsKey("cookiePolicyUrl") && map.get("cookiePolicyUrl") != null) {
      dto.setCookiePolicyUrl(map.get("cookiePolicyUrl").toString());
    }
    return dto;
  }

  public static Map<String, Object> toBrandingMap(BrandingSettingsDTO dto) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (dto == null) return map;
    if (dto.getAppName() != null) map.put("appName", dto.getAppName());
    if (dto.getAppTagline() != null) map.put("appTagline", dto.getAppTagline());
    if (dto.getLogoUrl() != null) map.put("logoUrl", dto.getLogoUrl());
    if (dto.getFaviconUrl() != null) map.put("faviconUrl", dto.getFaviconUrl());
    if (dto.getFooterCopyright() != null) map.put("footerCopyright", dto.getFooterCopyright());
    if (dto.getSupportEmail() != null) map.put("supportEmail", dto.getSupportEmail());
    return map;
  }

  public static Map<String, Object> toLoginMap(LoginSettingsDTO dto) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (dto == null) return map;
    if (dto.getLoginMode() != null) map.put("loginMode", dto.getLoginMode());
    if (dto.getLoginBgUrl() != null) map.put("loginBgUrl", dto.getLoginBgUrl());
    if (dto.getLoginBgOpacity() != null) map.put("loginBgOpacity", dto.getLoginBgOpacity());
    if (dto.getLoginHeroTitle() != null) map.put("loginHeroTitle", dto.getLoginHeroTitle());
    if (dto.getLoginHeroSubtitle() != null) map.put("loginHeroSubtitle", dto.getLoginHeroSubtitle());
    if (dto.getHideHomeButton() != null) map.put("hideHomeButton", dto.getHideHomeButton());
    return map;
  }

  public static Map<String, Object> toPolicyMap(PolicySettingsDTO dto) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (dto == null) return map;
    if (dto.getPrivacyPolicyUrl() != null) map.put("privacyPolicyUrl", dto.getPrivacyPolicyUrl());
    if (dto.getCookiePolicyUrl() != null) map.put("cookiePolicyUrl", dto.getCookiePolicyUrl());
    return map;
  }
}
