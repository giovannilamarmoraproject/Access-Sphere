package io.github.giovannilamarmora.accesssphere.settings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio per la categoria 'Personalizzazione Login'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginSettings {
  private String loginMode;
  private String loginBgUrl;
  private Integer loginBgOpacity;
  private String loginHeroTitle;
  private String loginHeroSubtitle;
  private Boolean hideHomeButton;
}
