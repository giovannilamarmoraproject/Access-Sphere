package io.github.giovannilamarmora.accesssphere.roles.database;

import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserClientRoleRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IUserClientRoleDAO userClientRoleDAO;

  public Optional<List<UserClientRole>> findByUserIdentifier(String identifier) {
    LOG.debug("🔐 Getting all UserClient Role for identifier{} ", identifier);
    List<UserClientRoleEntity> getEntityList = userClientRoleDAO.findByUserIdentifier(identifier);

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("⚖️ UserClient Role for identifier: {} not found", identifier);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} UserClient Role for identifier{} ", getEntityList.size(), identifier);
    return Optional.of(UserClientRoleMapper.toModelList(getEntityList));
  }
}
