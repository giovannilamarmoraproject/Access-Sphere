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
 * semplici (es. "ROLE_USER"), con fallback automatico al tipo standard USER.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = AppRoleDeserializer.class)
public class AppRole extends GenericDTO {

  @io.swagger.v3.oas.annotations.media.Schema(description = "Identificativo numerico del ruolo a database", example = "1")
  private Long id;

  @JsonAlias({"name", "roleName", "value"})
  @io.swagger.v3.oas.annotations.media.Schema(description = "Nome identificativo univoco del ruolo per il client", example = "ROLE_USER")
  private String role;

  @io.swagger.v3.oas.annotations.media.Schema(description = "Tipologia di ruolo (USER, ADMIN, TECHNICAL, MANAGER)", example = "USER")
  private String type = AppRoleType.USER.getValue();

  @io.swagger.v3.oas.annotations.media.Schema(description = "Descrizione testuale delle responsabilita del ruolo", example = "Ruolo standard utente applicativo")
  private String description;

  @io.swagger.v3.oas.annotations.media.Schema(
      description = "Lista dei permessi granulari associati al ruolo (es. [\"user:read\", \"user:write\"])",
      example = "[\"user:read\", \"user:write\"]")
  private java.util.List<String> permissions;

  /**
   * Costruttore di convenienza a partire dal solo nome del ruolo. Assegna automaticamente il tipo
   * canonico {@link AppRoleType#USER}.
   *
   * @param role Nome identificativo del ruolo (es. "ROLE_USER").
   */
  public AppRole(String role) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type = AppRoleType.USER.getValue();
  }

  /**
   * Costruttore per specificare nome ruolo e tipologia applicativa.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo (es. USER, TECHNICAL, ADMIN, MANAGER).
   */
  public AppRole(String role, String type) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? ("DEFAULT".equalsIgnoreCase(type.trim()) ? AppRoleType.USER.getValue() : type.trim().toUpperCase())
            : AppRoleType.USER.getValue();
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
            ? ("DEFAULT".equalsIgnoreCase(type.trim()) ? AppRoleType.USER.getValue() : type.trim().toUpperCase())
            : AppRoleType.USER.getValue();
    this.description = description;
  }

  /**
   * Costruttore completo comprensivo di permessi granulari.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione del ruolo.
   * @param permissions Lista di permessi associati (es. "users:read").
   */
  public AppRole(String role, String type, String description, java.util.List<String> permissions) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? ("DEFAULT".equalsIgnoreCase(type.trim()) ? AppRoleType.USER.getValue() : type.trim().toUpperCase())
            : AppRoleType.USER.getValue();
    this.description = description;
    this.permissions = permissions;
  }

  public AppRole(Long id, String role, String type, String description) {
    this.id = id;
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? ("DEFAULT".equalsIgnoreCase(type.trim()) ? AppRoleType.USER.getValue() : type.trim().toUpperCase())
            : AppRoleType.USER.getValue();
    this.description = description;
  }

  /**
   * Verifica se il ruolo corrente e' marcato con la tipologia standard USER.
   *
   * @return true se il tipo corrisponde a "USER" (case-insensitive), false altrimenti.
   */
  public boolean isUser() {
    return AppRoleType.USER.getValue().equalsIgnoreCase(this.type);
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
