package io.github.giovannilamarmora.accesssphere.client.dto;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la rappresentazione di un singolo Client OAuth2.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} con l'oggetto di
 * dominio {@link OAuth2Client}. Questo consente al motore di documentazione Swagger/OpenAPI di
 * generare un contratto JSON completo di schema e valori di esempio concreti, superando il limite
 * della definizione generica con Object vuoto.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Involucro di risposta standard contenente i dettagli completi di un Client OAuth 2.0")
public class OAuth2ClientResponse extends Response {

  @Schema(
      description = "Codice di stato HTTP dell'operazione",
      example = "200")
  private Integer status;

  @Schema(
      description = "Messaggio descrittivo sintetico dell'esito dell'operazione",
      example = "Client details")
  private String message;

  @Schema(
      description = "Identificativo di tracciamento univoco della richiesta distribuita (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(description = "Dati del Client OAuth 2.0 configurato nel sistema")
  private OAuth2Client data;
}
