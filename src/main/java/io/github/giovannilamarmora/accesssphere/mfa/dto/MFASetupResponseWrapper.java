package io.github.giovannilamarmora.accesssphere.mfa.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per l'inizializzazione del setup MFA.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} con l'oggetto
 * {@link MFASetupResponse} per generare uno schema Swagger accurato e completo di esempi.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper containing MFA setup details")
public class MFASetupResponseWrapper extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(description = "Descriptive message of the operation outcome", example = "MFA Generated")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(description = "MFA setup payload with secret or QR code")
  private MFASetupResponse data;
}
