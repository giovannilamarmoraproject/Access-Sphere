package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tipizzato per la categoria di impostazioni 'Conformità & Politiche'.
 * Raggruppa gli URI informativi per privacy policy e cookie policy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicySettingsDTO {
  private String privacyPolicyUrl;
  private String cookiePolicyUrl;
}
