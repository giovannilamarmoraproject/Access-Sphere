package io.github.giovannilamarmora.accesssphere.settings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio per la categoria 'Branding & Identità'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandingSettings {
  private String appName;
  private String appTagline;
  private String logoUrl;
  private String faviconUrl;
  private String footerCopyright;
  private String supportEmail;
}
