package io.github.giovannilamarmora.accesssphere.data.user.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la rappresentazione di un singolo Utente IAM.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} con l'oggetto di
 * dominio {@link User}. Questo consente al motore di documentazione Swagger/OpenAPI di generare
 * un contratto JSON completo di schema e valori di esempio concreti per l'utenza IAM.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper containing full details of an IAM user")
public class UserResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "User details")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(description = "Details of the IAM user configured in the system")
  private User data;
}
