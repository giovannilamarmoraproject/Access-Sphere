package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO che modella un ruolo applicativo (AppRole) associato a un client OAuth2. Configurato con
 * {@link AppRoleDeserializer} per supportare in ingresso sia oggetti strutturati che stringhe
 * semplici (es. "ROLE_ADMIN"), con fallback automatico al tipo standard DEFAULT.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = AppRoleDeserializer.class)
public class AppRole extends GenericDTO {

  private Long id;

  @JsonAlias({"name", "roleName", "value"})
  private String role;

  private String type = AppRoleType.DEFAULT.getValue();

  private String description;

  /**
   * Costruttore di convenienza a partire dal solo nome del ruolo. Assegna automaticamente il tipo
   * canonico {@link AppRoleType#DEFAULT}.
   *
   * @param role Nome identificativo del ruolo (es. "ROLE_ADMIN").
   */
  public AppRole(String role) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type = AppRoleType.DEFAULT.getValue();
  }

  /**
   * Costruttore per specificare nome ruolo e tipologia applicativa.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo (es. DEFAULT, TECHNICAL, ADMIN).
   */
  public AppRole(String role, String type) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? type.trim().toUpperCase()
            : AppRoleType.DEFAULT.getValue();
  }

  /**
   * Costruttore completo per nome, tipologia e descrizione.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione dettagliata dello scope del ruolo.
   */
  public AppRole(String role, String type, String description) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? type.trim().toUpperCase()
            : AppRoleType.DEFAULT.getValue();
    this.description = description;
  }

  /**
   * Verifica se il ruolo corrente e' marcato con la tipologia fondamentale DEFAULT.
   *
   * @return true se il tipo corrisponde a "DEFAULT" (case-insensitive), false altrimenti.
   */
  public boolean isDefault() {
    return AppRoleType.DEFAULT.getValue().equalsIgnoreCase(this.type);
  }

  /**
   * Verifica se il nome del ruolo corrente corrisponde a quello specificato (case-insensitive).
   *
   * @param targetRole Nome del ruolo da confrontare.
   * @return true se i ruoli coincidono, false altrimenti.
   */
  public boolean matches(String targetRole) {
    return this.role != null && this.role.equalsIgnoreCase(targetRole);
  }

  /**
   * Factory method statico per istanziare rapidamente un AppRole dal solo nome.
   *
   * @param role Nome del ruolo.
   * @return Nuova istanza di {@link AppRole}.
   */
  public static AppRole of(String role) {
    return new AppRole(role);
  }

  /**
   * Factory method statico completo.
   *
   * @param role Nome del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione del ruolo.
   * @return Nuova istanza di {@link AppRole}.
   */
  public static AppRole of(String role, String type, String description) {
    return new AppRole(role, type, description);
  }
}
