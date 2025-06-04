package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDAOAdapter implements UserDataService {

  @Autowired private IUserDAO userDAO;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity findUserEntityByUsernameOrEmail(String username, String email) {
    return userDAO.findUserEntityByUsernameOrEmail(username, email);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity findUserEntityByEmail(String email) {
    return userDAO.findUserEntityByEmail(email);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity saveAndFlush(UserEntity user) {
    return userDAO.saveAndFlush(user);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity findUserEntityByTokenReset(String token) {
    return userDAO.findUserEntityByTokenReset(token);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserEntity findUserEntityByIdentifier(String identifier) {
    return userDAO.findUserEntityByIdentifier(identifier);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserEntity> findAll() {
    return userDAO.findAll();
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void delete(UserEntity userEntity) {
    userDAO.delete(userEntity);
  }

  @Override
  public void deleteByIdentifier(String identifier) {
    userDAO.deleteUserEntityByIdentifier(identifier);
  }
}
