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
 * tipizzata {@link List} di {@link OAuth2Client}. Questo permette alla documentazione Swagger UI
 * di mostrare un array strutturato di client con attributi e valori di esempio accurati.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Involucro di risposta standard contenente la lista dei Client OAuth 2.0 registrati")
public class OAuth2ClientListResponse extends Response {

  @Schema(
      description = "Codice di stato HTTP dell'operazione",
      example = "200")
  private Integer status;

  @Schema(
      description = "Messaggio descrittivo sintetico dell'esito dell'operazione",
      example = "Client credential list")
  private String message;

  @Schema(
      description = "Identificativo di tracciamento univoco della richiesta distribuita (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @ArraySchema(
      schema = @Schema(description = "Elemento Client OAuth2", implementation = OAuth2Client.class))
  @Schema(description = "Elenco completo dei Client OAuth 2.0 registrati a catalogo")
  private List<OAuth2Client> data;
}
