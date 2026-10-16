package io.github.giovannilamarmora.accesssphere.mfa.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Involucro di risposta standard OpenAPI per l'inizializzazione del setup MFA.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} con {@link MFASetupDataResponse}.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper containing MFA setup details")
public class MFASetupResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "MFA secret generated successfully")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(description = "Details generated during MFA initialization")
  private MFASetupDataResponse data;
}
