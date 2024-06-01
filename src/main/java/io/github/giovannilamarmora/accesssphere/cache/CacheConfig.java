package io.github.giovannilamarmora.accesssphere.cache;

import io.github.giovannilamarmora.accesssphere.data.user.database.UserDAOAdapter;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

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
}
