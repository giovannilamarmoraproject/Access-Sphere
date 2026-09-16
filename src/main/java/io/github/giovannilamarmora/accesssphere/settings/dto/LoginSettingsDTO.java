package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tipizzato per la categoria di impostazioni 'Personalizzazione Login'.
 * Raggruppa modalità della schermata di login, sfondi custom, opacità e testi hero.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginSettingsDTO {
  private String loginMode;
  private String loginBgUrl;
  private Integer loginBgOpacity;
  private String loginHeroTitle;
  private String loginHeroSubtitle;
  private Boolean hideHomeButton;
}
