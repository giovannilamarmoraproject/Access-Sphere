package io.github.giovannilamarmora.accesssphere.cache;

import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
public class UserCacheService implements UserDataService {

  private static final String USER_CACHE = "Users_Cache";

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private CacheManager cacheManager;
  @Autowired private IUserDAO userDAO;

  @Override
  @Cacheable(value = USER_CACHE, key = "#email", condition = "#email!=null")
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  // public Mono<UserEntity> findUserEntityByEmail(String email) {
  public UserEntity findUserEntityByEmail(String email) {
    LOG.info("[Caching] Finding User into Database by email {}", email);
    return userDAO.findUserEntityByEmail(email);
  }

  @Override
  @Caching(
      cacheable = {
        @Cacheable(value = USER_CACHE, key = "#username", condition = "#username!=null"),
        @Cacheable(value = USER_CACHE, key = "#email", condition = "#email!=null")
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public UserEntity findUserEntityByUsernameOrEmail(String username, String email) {
    LOG.info("[Caching] Finding User into Database by username {} and email {}", username, email);
    return userDAO.findUserEntityByUsernameOrEmail(username, email);
  }

  @Caching(evict = @CacheEvict(value = USER_CACHE))
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public void deleteUserCache() {
    LOG.info("[Caching] Deleting cache for {}", USER_CACHE);
    Objects.requireNonNull(cacheManager.getCache(USER_CACHE)).clear();
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public UserEntity saveAndFlush(UserEntity user) {
    deleteUserCache();
    return userDAO.saveAndFlush(user);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public UserEntity findUserEntityByTokenReset(String token) {
    return userDAO.findUserEntityByTokenReset(token);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public UserEntity findUserEntityByIdentifier(String identifier) {
    return userDAO.findUserEntityByIdentifier(identifier);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public List<UserEntity> findAll() {
    return userDAO.findAll();
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public void delete(UserEntity userEntity) {
    deleteUserCache();
    userDAO.delete(userEntity);
  }
}
