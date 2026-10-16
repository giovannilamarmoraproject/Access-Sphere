package io.github.giovannilamarmora.accesssphere.user.dto;

import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la cancellazione di un Utente IAM.
 *
 * <p>Estende {@link Response} documentando l'esito della cancellazione permanente dell'account.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper confirming the deletion of an IAM user")
public class UserDeleteResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "User successfully deleted")
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
