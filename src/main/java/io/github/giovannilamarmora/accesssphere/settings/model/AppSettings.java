package io.github.giovannilamarmora.accesssphere.settings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio primario per le impostazioni applicative (AppSettings).
 * Disaccoppiato dalle entità JPA di persistenza e dai contratti REST DTO di trasporto.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppSettings extends GenericDTO {
  private Long id;
  private String activeTheme;
  private String defaultLanguage;
  private String navigationLayout;

  private BrandingSettings brandingSettings;
  private LoginSettings loginSettings;
  private PolicySettings policySettings;
}
