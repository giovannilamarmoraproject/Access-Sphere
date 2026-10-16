package io.github.giovannilamarmora.accesssphere.mfa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio per le impostazioni globali e i metodi MFA registrati per un utente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MFASetting {
  private Boolean enabled;
  private List<MFAMethod> mfaMethods;
}
