package io.github.giovannilamarmora.accesssphere.user.dto;

import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la lista degli Utenti IAM.
 *
 * <p>Estende {@link Response} tipizzando esplicitamente il campo {@code data} come collezione
 * tipizzata {@link List} di {@link User}. Questo consente a Swagger UI di documentare accuratamente
 * la struttura dell'elenco utenti.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper containing the list of registered IAM users")
public class UserListResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "Users list")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @ArraySchema(schema = @Schema(description = "IAM User item", implementation = User.class))
  @Schema(description = "Complete list of registered IAM users in the catalog")
  private List<UserDataResponse> data;
}
