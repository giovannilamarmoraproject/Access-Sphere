package io.github.giovannilamarmora.accesssphere.roles.database;

import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA per l'accesso e la manipolazione delle associazioni
 * relazionali tra utenti, client OAuth2 e ruoli nella tabella USER_CLIENT_ROLE.
 */
@Repository
public interface IUserClientRoleDAO extends JpaRepository<UserClientRoleEntity, Long> {

  List<UserClientRoleEntity> findByUserIdentifier(String userIdentifier);

  List<UserClientRoleEntity> findByUserIdentifierAndClientId(String userIdentifier, String clientId);

  List<UserClientRoleEntity> findByClientId(String clientId);

  List<UserClientRoleEntity> findByClientIdAndRole(String clientId, String role);

  boolean existsByUserIdentifierAndClientIdAndRole(
      String userIdentifier, String clientId, String role);

  @Transactional
  void deleteByUserIdentifier(String userIdentifier);

  @Transactional
  void deleteByUserIdentifierAndClientIdAndRole(
      String userIdentifier, String clientId, String role);

  @Transactional
  void deleteByUserIdentifierAndClientId(String userIdentifier, String clientId);

  @Transactional
  void deleteByClientIdAndRole(String clientId, String role);

  @Transactional
  void deleteByClientId(String clientId);
}
