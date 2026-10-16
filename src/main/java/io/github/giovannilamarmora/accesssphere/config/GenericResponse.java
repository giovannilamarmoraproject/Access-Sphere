package io.github.giovannilamarmora.accesssphere.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello DTO base per metadati di audit temporali (creazione, aggiornamento, eliminazione logica).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Generic audit metadata response containing resource timestamps")
public class GenericResponse {

  @Schema(
      description = "Creation timestamp of the resource (ISO-8601)",
      example = "2026-10-16T10:15:30")
  private LocalDateTime creationDate;

  @Schema(
      description = "Last update timestamp of the resource (ISO-8601)",
      example = "2026-10-16T12:30:45")
  private LocalDateTime updateDate;

  @Schema(
      description = "Soft deletion timestamp of the resource, if marked as deleted (ISO-8601)",
      example = "2026-10-16T14:00:00")
  private LocalDateTime deletedDate;
}
