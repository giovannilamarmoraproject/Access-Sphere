package io.github.giovannilamarmora.accesssphere.user.database;

import io.github.giovannilamarmora.accesssphere.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private IUserDAO userDAO;

  /**
   * Recupera un utente dal database locale a partire dal suo indirizzo email. Effettua la
   * validazione di esistenza e la conversione in DTO {@link User}.
   *
   * @param email Indirizzo email dell'utente.
   * @return Mono contenente il DTO {@link User}.
   */
  public Optional<User> getUserByEmail(String email) {
    LOG.debug("👤 Getting user by email: {}", email);
    UserEntity getEntity = userDAO.findUserEntityByEmail(email);

    if (ObjectToolkit.isNullOrEmpty(getEntity)) {
      LOG.warn("❌ User with email: {} not found in Database", email);
      return Optional.empty();
    }

    LOG.debug("👤 Found User {} in Database", email);
    return Optional.of(UserMapper.toModel(getEntity));
  }

  public Optional<List<User>> findAll() {
    LOG.debug("👤 Getting all user into the database");
    List<UserEntity> getEntities = userDAO.findAll();

    if (ObjectToolkit.isNullOrEmpty(getEntities)) {
      LOG.warn("❌ Users not found in Database");
      return Optional.empty();
    }

    LOG.debug("👤 Found {} User in Database", getEntities.size());
    return Optional.of(UserMapper.toModelList(getEntities));
  }
}
