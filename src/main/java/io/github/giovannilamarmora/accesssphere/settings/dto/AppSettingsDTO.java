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

}
