package io.github.giovannilamarmora.accesssphere.settings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio per la categoria 'Conformità & Politiche'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicySettings {
  private String privacyPolicyUrl;
  private String cookiePolicyUrl;
}
