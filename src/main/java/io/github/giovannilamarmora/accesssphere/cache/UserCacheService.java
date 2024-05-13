package io.github.giovannilamarmora.accesssphere.cache;

import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserCacheService implements UserDataService {

  private static final String USER_CACHE = "Users_Cache";

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private CacheManager cacheManager;
  @Autowired private IUserDAO userDAO;

  @Override
  @Cacheable(value = USER_CACHE, key = "#email", condition = "#email!=null")
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  //public Mono<UserEntity> findUserEntityByEmail(String email) {
    public Mono<UserEntity> findUserEntityByEmail(String email) {
    LOG.info("[Caching] Finding User into Database by email {}", email);
    return Mono.just(userDAO.findUserEntityByEmail(email));
  }

  @Override
  @Caching(
      cacheable = {
        @Cacheable(value = USER_CACHE, key = "#username", condition = "#username!=null"),
        @Cacheable(value = USER_CACHE, key = "#email", condition = "#email!=null")
      })
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public Mono<UserEntity> findUserEntityByUsernameOrEmail(String username, String email) {
    LOG.info("[Caching] Finding User into Database by username {} and email {}", username, email);
    return Mono.just(userDAO.findUserEntityByUsernameOrEmail(username, email));
  }

  @Caching(evict = @CacheEvict(value = USER_CACHE))
  @LogInterceptor(type = LogTimeTracker.ActionType.CACHE)
  public void deleteUserCache() {
    LOG.info("[Caching] Deleting cache for {}", USER_CACHE);
    Objects.requireNonNull(cacheManager.getCache(USER_CACHE)).clear();
  }

  @Override
  public Mono<UserEntity> save(UserEntity user) {
    deleteUserCache();
    return Mono.just(userDAO.save(user));
  }

  @Override
  public Mono<UserEntity> findUserEntityByTokenReset(String token) {
    return Mono.just(userDAO.findUserEntityByTokenReset(token));
  }
}
