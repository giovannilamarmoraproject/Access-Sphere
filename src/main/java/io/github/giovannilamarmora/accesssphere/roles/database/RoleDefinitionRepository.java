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
    return Optional.of(RoleDefinitionMapper.toModelList(getEntityList));
  }

  public Optional<List<RoleDefinition>> findByClientId(String clientId) {
    LOG.debug("🔐 Getting all Role Definition with client ID {}", clientId);
    List<RoleDefinitionEntity> getEntityList = roleDefinitionDAO.findByClientId(clientId);

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("⚖️ Role Definitions for client {} not found", clientId);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} RoleDefinition for client {}", getEntityList.size(), clientId);
    return Optional.of(RoleDefinitionMapper.toModelList(getEntityList));
  }
}
