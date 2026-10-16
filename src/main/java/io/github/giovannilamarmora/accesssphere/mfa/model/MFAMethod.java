package io.github.giovannilamarmora.accesssphere.mfa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio rappresentante un metodo MFA configurato per un utente (TOTP o EMAIL).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MFAMethod extends GenericDTO {
  private MFAType type;
  private TOTPLabel label;

  @JsonIgnore private String secretKey;

  private boolean confirmed;

  public MFAMethod(
      MFAType type,
      TOTPLabel label,
      String secretKey,
      boolean confirmed,
      LocalDateTime creationDate,
      LocalDateTime updateDate) {
    super(null, creationDate, updateDate, null);
    this.type = type;
    this.label = label;
    this.secretKey = secretKey;
    this.confirmed = confirmed;
  }
}
