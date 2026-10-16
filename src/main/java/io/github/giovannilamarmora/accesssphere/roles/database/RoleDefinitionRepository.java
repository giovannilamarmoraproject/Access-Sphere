package io.github.giovannilamarmora.accesssphere.roles.database;

import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.mapper.RoleDefinitionMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RoleDefinitionRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;

  public Optional<List<RoleDefinition>> findAll() {
    LOG.debug("🔐 Getting all Role Definition");
    List<RoleDefinitionEntity> getEntityList = roleDefinitionDAO.findAll();

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("⚖️ Role Definitions not found");
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} RoleDefinition", getEntityList.size());
    return Optional.of(RoleDefinitionMapper.entitiesToModelList(getEntityList));
  }

  public Optional<List<RoleDefinition>> findByClientId(String clientId) {
    LOG.debug("🔐 Getting all Role Definition with client ID {}", clientId);
    List<RoleDefinitionEntity> getEntityList = roleDefinitionDAO.findByClientId(clientId);

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("⚖️ Role Definitions for client {} not found", clientId);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} RoleDefinition for client {}", getEntityList.size(), clientId);
    return Optional.of(RoleDefinitionMapper.entitiesToModelList(getEntityList));
  }

  public Optional<RoleDefinition> findByClientIdAndRole(String clientId, String role) {
    LOG.debug("🔐 Getting Role Definition for client ID {} and role {}", clientId, role);
    Optional<RoleDefinitionEntity> getEntity =
        roleDefinitionDAO.findByClientIdAndRole(clientId, role);

    if (getEntity.isEmpty()) {
      LOG.warn("⚖️ Role Definition for client {} and role {} not found", clientId, role);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found Role Definition for client {} and role {}", clientId, role);
    return Optional.of(RoleDefinitionMapper.toModel(getEntity.get()));
  }

  public Boolean existsByClientIdAndRole(String clientId, String role) {
    LOG.debug("🔐 Checking if role {} exist with client ID {}", role, clientId);
    return roleDefinitionDAO.existsByClientIdAndRole(clientId, role);
  }

  public RoleDefinition save(RoleDefinition roleDefinition) {
    LOG.debug("🔐 Saving Role Definition with name: {}", roleDefinition.getRole());
    RoleDefinitionEntity toEntity = RoleDefinitionMapper.toEntity(roleDefinition);

    RoleDefinitionEntity savedEntity = roleDefinitionDAO.save(toEntity);

    LOG.debug("⚖️ Saved Role Definition with name: {} in Database", roleDefinition.getRole());
    return RoleDefinitionMapper.toModel(savedEntity);
  }

  public void deleteByClientId(String clientId) {
    LOG.debug("🔐 Deleting Role Definition with clientId: {}", clientId);
    roleDefinitionDAO.deleteByClientId(clientId);
    LOG.debug("⚖️ Deleted Role Definition with clientId: {} in Database", clientId);
  }

  public void deleteByClientIdAndRole(String clientId, String role) {
    LOG.debug("🔐 Deleting Role Definition with clientId: {} and role: {}", clientId, role);
    roleDefinitionDAO.deleteByClientIdAndRole(clientId, role);
    LOG.debug(
        "⚖️ Deleted Role Definition with clientId: {} and role: {} in Database", clientId, role);
  }
}
