package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO sintetico che rappresenta le informazioni essenziali di un utente a cui e assegnato un ruolo.
 * Utilizzato per il conteggio e l'anteprima degli utenti impattati in fase di consultazione o
 * eliminazione ruoli.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary information of an IAM user assigned to an application role")
public class UserSummaryDTO extends GenericDTO {

  @Schema(
      description = "Unique UUID identifier of the user",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String identifier;

  @Schema(description = "Unique username of the user", example = "johndoe")
  private String username;

  @Schema(description = "First name of the user", example = "John")
  private String name;

  @Schema(description = "Last name of the user", example = "Doe")
  private String surname;

  @Schema(description = "Primary email address of the user", example = "john.doe@example.com")
  private String email;
}
