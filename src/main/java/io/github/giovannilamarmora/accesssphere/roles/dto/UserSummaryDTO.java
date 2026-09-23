package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO sintetico che rappresenta le informazioni essenziali di un utente a cui è assegnato un ruolo.
 * Utilizzato per il conteggio e l'anteprima degli utenti impattati in fase di consultazione o eliminazione ruoli.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDTO extends GenericDTO {

  private String identifier;
  private String username;
  private String name;
  private String surname;
  private String email;
}
