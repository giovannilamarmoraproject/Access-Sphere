package io.github.giovannilamarmora.accesssphere.roles.database;

import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA per la gestione e persistenza delle definizioni dei ruoli applicativi
 * e relativi permessi nella tabella {@code ROLE_DEFINITIONS}.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Consumato da {@code RolesService} per la consultazione e il ciclo di vita CRUD dei ruoli.</li>
 *   <li>Consumato da {@code UserService} per la risoluzione dinamica delle autorizzazioni dell'utente.</li>
 *   <li>Consumato da {@code ProjectInitializerService} per il seeding e la migrazione automatica da JSON legacy.</li>
 * </ul>
 */
@Repository
public interface IRoleDefinitionDAO extends JpaRepository<RoleDefinitionEntity, Long> {

  List<RoleDefinitionEntity> findByClientId(String clientId);

  Optional<RoleDefinitionEntity> findByClientIdAndRole(String clientId, String role);

  boolean existsByClientIdAndRole(String clientId, String role);

  @Transactional
  void deleteByClientIdAndRole(String clientId, String role);

  @Transactional
  void deleteByClientId(String clientId);
}
