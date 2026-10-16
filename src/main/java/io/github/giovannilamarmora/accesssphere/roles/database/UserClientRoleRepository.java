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

  public Optional<List<UserClientRole>> findAll() {
    LOG.debug("🔐 Getting all UserClient Role");
    List<UserClientRoleEntity> getEntityList = userClientRoleDAO.findAll();

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("⚖️ UserClient Role not found");
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} UserClient Role", getEntityList.size());
    return Optional.of(UserClientRoleMapper.toModelList(getEntityList));
  }

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

  public Optional<List<UserClientRole>> findByUserIdentifierAndClientId(
      String identifier, String clientId) {
    LOG.debug(
        "🔐 Getting all UserClient Role for identifier: {} and clientId: {}",
        identifier,
        clientId);
    List<UserClientRoleEntity> getEntityList =
        userClientRoleDAO.findByUserIdentifierAndClientId(identifier, clientId);

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn(
          "⚖️ UserClient Role for identifier: {} and clientId: {} not found",
          identifier,
          clientId);
      return Optional.empty();
    }

    LOG.debug(
        "⚖️ Found {} UserClient Role for identifier: {} and clientId: {}",
        getEntityList.size(),
        identifier,
        clientId);
    return Optional.of(UserClientRoleMapper.toModelList(getEntityList));
  }

  public Optional<List<UserClientRole>> findByClientIdAndRole(String clientId, String role) {
    LOG.debug("🔐 Getting all UserClient Role for clientId: {} and role: {}", clientId, role);
    List<UserClientRoleEntity> getEntityList =
        userClientRoleDAO.findByClientIdAndRole(clientId, role);

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.debug("⚖️ UserClient Role for clientId: {} and role: {} not found", clientId, role);
      return Optional.empty();
    }

    LOG.debug(
        "⚖️ Found {} UserClient Role for clientId: {} and role: {}",
        getEntityList.size(),
        clientId,
        role);
    return Optional.of(UserClientRoleMapper.toModelList(getEntityList));
  }

  public boolean existsByUserIdentifierAndClientIdAndRole(
      String identifier, String clientId, String role) {
    LOG.debug(
        "🔐 Checking if UserClient Role exists for identifier: {}, clientId: {}, role: {}",
        identifier,
        clientId,
        role);
    return userClientRoleDAO.existsByUserIdentifierAndClientIdAndRole(identifier, clientId, role);
  }

  public UserClientRole save(UserClientRole userClientRole, String identifier) {
    LOG.debug("🔐 Saving UserClient Role with name: {}", userClientRole.getRole());
    UserClientRoleEntity toEntity = UserClientRoleMapper.toEntity(userClientRole, identifier);

    UserClientRoleEntity savedEntity = userClientRoleDAO.save(toEntity);

    LOG.debug("⚖️ Saved UserClient Role with name: {} in Database", userClientRole.getRole());
    return UserClientRoleMapper.toModel(savedEntity);
  }

  public List<UserClientRole> saveAll(List<UserClientRole> userClientRoles, String identifier) {
    LOG.debug("🔐 Saving UserClient Role for identifier: {}", identifier);
    List<UserClientRoleEntity> toEntities =
        UserClientRoleMapper.toEntityList(userClientRoles, identifier);

    List<UserClientRoleEntity> savedEntity = userClientRoleDAO.saveAllAndFlush(toEntities);

    LOG.debug("⚖️ Saved UserClient Role for identifier: {} in Database", identifier);
    return UserClientRoleMapper.toModelList(savedEntity);
  }

  public void deleteByClientId(String clientId) {
    LOG.debug("🔐 Deleting UserClient Role with clientId: {}", clientId);
    userClientRoleDAO.deleteByClientId(clientId);
    LOG.debug("⚖️ Deleted UserClient Role with clientId: {} in Database", clientId);
  }

  public void deleteByClientIdAndRole(String clientId, String role) {
    LOG.debug("🔐 Deleting UserClient Role with clientId: {} and role: {}", clientId, role);
    userClientRoleDAO.deleteByClientIdAndRole(clientId, role);
    LOG.debug(
        "⚖️ Deleted UserClient Role with clientId: {} and role: {} in Database", clientId, role);
  }

  public void deleteByIdentifier(String identifier) {
    LOG.debug("🔐 Deleting UserClient Role with identifier: {}", identifier);
    userClientRoleDAO.deleteByUserIdentifier(identifier);
    LOG.debug("⚖️ Deleted UserClient Role with identifier: {} in Database", identifier);
  }

  public void deleteByUserIdentifierAndClientId(String identifier, String clientId) {
    LOG.debug(
        "🔐 Deleting UserClient Role with identifier: {} and clientId: {}", identifier, clientId);
    userClientRoleDAO.deleteByUserIdentifierAndClientId(identifier, clientId);
    LOG.debug(
        "⚖️ Deleted UserClient Role with identifier: {} and clientId: {} in Database",
        identifier,
        clientId);
  }

  public void deleteByUserIdentifierAndClientIdAndRole(
      String identifier, String clientId, String role) {
    LOG.debug(
        "🔐 Deleting UserClient Role with identifier: {}, clientId: {} and role: {}",
        identifier,
        clientId,
        role);
    userClientRoleDAO.deleteByUserIdentifierAndClientIdAndRole(identifier, clientId, role);
    LOG.debug(
        "⚖️ Deleted UserClient Role with identifier: {}, clientId: {} and role: {} in Database",
        identifier,
        clientId,
        role);
  }
}
