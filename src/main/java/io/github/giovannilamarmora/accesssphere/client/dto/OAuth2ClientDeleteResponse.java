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
@Schema(description = "Involucro di risposta standard per l'esito di cancellazione di un Client OAuth 2.0")
public class OAuth2ClientDeleteResponse extends Response {

  @Schema(
      description = "Codice di stato HTTP dell'operazione",
      example = "200")
  private Integer status;

  @Schema(
      description = "Messaggio descrittivo dell'avvenuta eliminazione",
      example = "Client deleted")
  private String message;

  @Schema(
      description = "Identificativo di tracciamento univoco della richiesta distribuita (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(
      description = "Dati del payload (nullo a seguito di eliminazione avvenuta con successo)",
      nullable = true,
      example = "null")
  private Object data;
}
