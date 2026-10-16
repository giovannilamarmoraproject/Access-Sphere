package io.github.giovannilamarmora.accesssphere.email.template.dto;

import io.github.giovannilamarmora.utils.generic.Response;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Standard OpenAPI response wrapper for the list of Email Templates.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Standard response wrapper containing the list of registered email templates")
public class EmailTemplateListResponse extends Response {

  @Schema(description = "HTTP status code of the operation", example = "200")
  private Integer status;

  @Schema(
      description = "Brief descriptive message of the operation outcome",
      example = "Email Templates List")
  private String message;

  @Schema(
      description = "Unique trace identifier for the distributed request (Span ID)",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String spanId;

  @ArraySchema(
      schema =
          @Schema(
              description = "Email template item",
              implementation = EmailTemplateDataResponse.class))
  @Schema(description = "Complete list of registered email templates in the catalog")
  private List<EmailTemplateDataResponse> data;
}
