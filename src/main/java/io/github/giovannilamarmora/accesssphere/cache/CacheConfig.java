package io.github.giovannilamarmora.accesssphere.cache;

import io.github.giovannilamarmora.accesssphere.data.user.database.UserDAOAdapter;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Objects;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;

@Configuration
public class CacheConfig {

  @Autowired private CacheManager cacheManager;
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Bean(name = "userDataService")
  @ConditionalOnProperty(prefix = "app.cache", name = "enable", havingValue = "true")
  UserDataService getCachedUserData() {
    LOG.info("[CACHE] Cache Status Enabled");
    return new UserCacheService();
  }

  @Bean(name = "userDataService")
  @ConditionalOnProperty(
      prefix = "app.cache",
      name = "enable",
      havingValue = "false",
      matchIfMissing = true)
  UserDataService getDatabaseUserData() {
    LOG.info("[CACHE] Cache Status Disabled");
    return new UserDAOAdapter();
  }

  @Scheduled(cron = "${app.cache.cron}")
  public void evictAllCaches() {
    if (!ObjectUtils.isEmpty(cacheManager) && !cacheManager.getCacheNames().isEmpty()) {
      LOG.info("Deleting cache for {}", cacheManager.getCacheNames());
      cacheManager
          .getCacheNames()
          .forEach(
              cacheName -> {
                if (!ObjectUtils.isEmpty(cacheName)
                    && !ObjectUtils.isEmpty(cacheManager.getCache(cacheName))) {
                  Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                }
              });
    }
  }
}
