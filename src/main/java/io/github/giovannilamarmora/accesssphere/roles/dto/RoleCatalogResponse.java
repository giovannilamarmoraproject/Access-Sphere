package io.github.giovannilamarmora.accesssphere.roles.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wrapper standard di risposta OpenAPI per la rappresentazione di una singola definizione di ruolo
 * a catalogo.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard response wrapper containing a single role definition")
public class RoleCatalogResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "Role definition details")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @Schema(description = "Details of the application role definition")
  private RoleDefinitionResponse data;
}
