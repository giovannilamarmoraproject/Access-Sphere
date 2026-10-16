package io.github.giovannilamarmora.accesssphere.roles.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la lista sintetizzata degli utenti assegnati a uno
 * specifico ruolo applicativo.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description =
        "Standard response wrapper containing the list of users assigned to a specific role")
public class RoleUsersListResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "Assigned users successfully retrieved")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @ArraySchema(
      schema = @Schema(description = "Assigned user summary", implementation = UserSummaryDTO.class))
  @Schema(description = "List of users currently holding the role")
  private List<UserSummaryDTO> data;
}
