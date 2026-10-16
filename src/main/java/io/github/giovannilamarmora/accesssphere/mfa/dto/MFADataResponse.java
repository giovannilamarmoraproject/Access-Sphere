package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload di risposta puro per le operazioni sul ciclo di vita dei metodi MFA (conferma, abilitazione, rimozione, invio OTP).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of an MFA operation outcome and active configuration")
public class MFADataResponse {

  @Schema(description = "Target user identifier (UUID)", example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String identifier;

  @Schema(description = "Overall MFA enabled status", example = "true")
  private Boolean mfaEnabled;

  @Schema(description = "Status or message describing the operation outcome", example = "CONFIRMED")
  private String status;

  @Schema(description = "List of confirmed and active MFA methods for the user")
  private List<MFAMethod> activeMethods;
}
