package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDAOAdapter implements UserDataService {

  @Autowired private IUserDAO userDAO;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserEntity> findUserEntityByUsernameOrEmail(String username, String email) {
    return Mono.just(userDAO.findUserEntityByUsernameOrEmail(username, email));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserEntity> findUserEntityByEmail(String email) {
    return Mono.just(userDAO.findUserEntityByEmail(email));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserEntity> save(UserEntity user) {
    return Mono.just(userDAO.save(user));
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<UserEntity> findUserEntityByTokenReset(String token) {
    return Mono.just(userDAO.findUserEntityByTokenReset(token));
  }
}
