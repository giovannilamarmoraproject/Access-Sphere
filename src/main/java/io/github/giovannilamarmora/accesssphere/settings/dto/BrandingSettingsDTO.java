package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tipizzato per la categoria di impostazioni 'Branding & Identità'.
 * Raggruppa nome dell'applicazione, tagline, loghi, copyright e contatti di supporto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandingSettingsDTO {
  private String appName;
  private String appTagline;
  private String logoUrl;
  private String faviconUrl;
  private String footerCopyright;
  private String supportEmail;
}
