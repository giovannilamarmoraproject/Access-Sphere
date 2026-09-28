package io.github.giovannilamarmora.accesssphere.roles.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entità relazionale che mappa l'associazione esplicita e contestualizzata tra un Utente IAM, uno
 * specifico Client OAuth2 e un Ruolo applicativo assegnato. Rappresenta la nuova architettura RBAC
 * multi-tenant di Access-Sphere.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(
    name = "USER_CLIENT_ROLE",
    indexes = {
      @Index(name = "IDX_UCR_USER", columnList = "USER_IDENTIFIER"),
      @Index(name = "IDX_UCR_CLIENT", columnList = "CLIENT_ID"),
      @Index(name = "IDX_UCR_USER_CLIENT", columnList = "USER_IDENTIFIER, CLIENT_ID")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_USER_CLIENT_ROLE",
          columnNames = {"USER_IDENTIFIER", "CLIENT_ID", "ROLE"})
    })
public class UserClientRoleEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "USER_IDENTIFIER", nullable = false)
  private String userIdentifier;

  @UpperCase
  @Column(name = "CLIENT_ID", nullable = false)
  private String clientId;

  @UpperCase
  @Column(name = "ROLE", nullable = false)
  private String role;

  public UserClientRoleEntity(String userIdentifier, String clientId, String role) {
    this.userIdentifier = userIdentifier;
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
    this.role = role != null ? role.trim().toUpperCase() : null;
  }
}
