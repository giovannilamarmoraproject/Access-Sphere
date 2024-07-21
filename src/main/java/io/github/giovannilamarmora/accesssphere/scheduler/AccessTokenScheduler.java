package io.github.giovannilamarmora.accesssphere.scheduler;

import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.MDCUtils;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Logged
@Service
public class AccessTokenScheduler {

  @Value(value = "${env:Default}")
  private String env;

  private static final Logger LOG = LoggerFactory.getLogger(AccessTokenScheduler.class);

  @Autowired private AccessTokenService accessTokenService;

  // @Scheduled(initialDelay = 1000)
  @Scheduled(cron = "0 0 0 * * SUN")
  @LogInterceptor(type = LogTimeTracker.ActionType.SCHEDULER)
  public void deleteAccessTokenExpired() {
    MDCUtils.registerDefaultMDC(env);
    LOG.info("\uD83D\uDE80 Starting Scheduler to clean the Database AccessToken");
    accessTokenService.deleteAccessTokenExpired();
    LOG.info("\uD83D\uDE80 Finished Scheduler to clean the Database AccessToken");
  }
}
