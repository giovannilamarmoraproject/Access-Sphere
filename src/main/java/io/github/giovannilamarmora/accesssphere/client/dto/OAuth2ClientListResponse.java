package io.github.giovannilamarmora.accesssphere.client.dto;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la lista dei Client OAuth2.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} come collezione
 * tipizzata {@link List} di {@link OAuth2Client}. Questo permette alla documentazione Swagger UI di
 * mostrare un array strutturato di client con attributi e valori di esempio accurati.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Standard response wrapper containing the list of registered OAuth 2.0 clients")
public class OAuth2ClientListResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "Client credential list")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @ArraySchema(
      schema = @Schema(description = "OAuth2 Client item", implementation = OAuth2Client.class))
  @Schema(description = "Complete list of registered OAuth 2.0 clients in the catalog")
  private List<OAuth2ClientDataResponse> data;
}
