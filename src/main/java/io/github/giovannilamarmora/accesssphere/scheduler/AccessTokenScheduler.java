package io.github.giovannilamarmora.accesssphere.scheduler;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Logged
@Service
public class AccessTokenScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTokenScheduler.class);

  @Autowired private AccessTokenService accessTokenService;

  // @Scheduled(initialDelay = 1000)
  @Scheduled(cron = "0 0 0 * * SUN")
  @LogInterceptor(type = LogTimeTracker.ActionType.SCHEDULER)
  public void deleteAccessTokenExpired() {
    LOG.info("\uD83D\uDE80 Starting Scheduler to clean the Database AccessToken");
    accessTokenService.deleteAccessTokenExpired();
    LOG.info("\uD83D\uDE80 Finished Scheduler to clean the Database AccessToken");
  }
}
