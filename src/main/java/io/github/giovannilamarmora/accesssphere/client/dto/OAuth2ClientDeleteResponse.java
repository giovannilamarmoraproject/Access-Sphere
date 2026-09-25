package io.github.giovannilamarmora.accesssphere.client.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per l'operazione di eliminazione di un Client OAuth2.
 *
 * <p>Estende {@link Response} rappresentando lo stato 200 OK con payload {@code data} vuoto o nullo
 * a seguito della de-registrazione del client dal catalogo.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper for the deletion outcome of an OAuth 2.0 client")
public class OAuth2ClientDeleteResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Descriptive message confirming successful deletion",
      example = "Client deleted")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(
      description = "Payload data (null following successful deletion)",
      nullable = true,
      example = "null")
  private Object data;
}
