package io.github.giovannilamarmora.accesssphere.scheduler;

import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
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
import reactor.core.publisher.Mono;

@Logged
@Service
public class UsersSyncScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(UsersSyncScheduler.class);

  @Autowired private DataService dataService;

  // @Scheduled(initialDelay = 1000)
  @Scheduled(cron = "0 0 0 * * *")
  @LogInterceptor(type = LogTimeTracker.ActionType.SCHEDULER)
  public void syncUsers() {
    if (dataService.getIsStrapiEnabled()) {
      LOG.info("\uD83D\uDE80 Starting Scheduler users sync with Strapi");
      Mono<List<User>> strapiUsers = dataService.getStrapiUsers();
      Mono<List<User>> dbClientsMono =
          dataService
              .getUsers()
              .onErrorResume(
                  throwable -> {
                    if (throwable instanceof OAuthException) {
                      return Mono.just(List.of());
                    }
                    return Mono.error(throwable);
                  });

      strapiUsers
          .zipWith(dbClientsMono)
          .subscribe(
              result -> {
                List<User> strapiUserList = result.getT1();
                List<User> dbUsers = result.getT2();
                syncClients(strapiUserList, dbUsers);
                LOG.info("\uD83D\uDE80 Scheduler finished successfully!");
              },
              error -> LOG.error("Error occurred during userss sync", error));
    } else {
      LOG.info("Strapi is not enabled, skipping users sync");
    }
  }

  private void syncClients(List<User> strapiUsers, List<User> dbUsers) {
    // Convert the list of dbClients to a map for quick lookup
    Map<String, User> dbUserMap =
        dbUsers.stream().collect(Collectors.toMap(User::getIdentifier, Function.identity()));

    // Iterate over strapiClients to add or update clients in the database
    for (User userStrapi : strapiUsers) {
      User dbUser = dbUserMap.get(userStrapi.getIdentifier());

      if (dbUser == null) {
        // Add new client to the database
        dataService.saveUserIntoDatabase(userStrapi);
      } else {
        // Update existing client in the database if there are changes
        if (!isUserEqual(userStrapi, dbUser)) {
          userStrapi.setId(dbUser.getId());
          dataService.updateUserIntoDatabase(userStrapi);
        } else LOG.info("Data already updated for user={}", dbUser.getIdentifier());
      }
    }

    // Optionally handle deletion of clients that are no longer in Strapi
    Set<String> strapiUsersId =
        strapiUsers.stream().map(User::getIdentifier).collect(Collectors.toSet());

    for (User dbUser : dbUsers) {
      if (!strapiUsersId.contains(dbUser.getIdentifier())) {
        dataService.deleteClientFromDatabase(dbUser);
      }
    }
  }

  private boolean isUserEqual(User strapiUser, User dbUser) {
    return Objects.equals(strapiUser.getIdentifier(), dbUser.getIdentifier())
        && Objects.equals(strapiUser.getName(), dbUser.getName())
        && Objects.equals(strapiUser.getSurname(), dbUser.getSurname())
        && Objects.equals(strapiUser.getEmail(), dbUser.getEmail())
        && Objects.equals(strapiUser.getUsername(), dbUser.getUsername())
        && Objects.equals(strapiUser.getRoles(), dbUser.getRoles())
        && Objects.equals(strapiUser.getProfilePhoto(), dbUser.getProfilePhoto())
        && Objects.equals(strapiUser.getAddresses(), dbUser.getAddresses())
        && Objects.equals(strapiUser.getPhoneNumber(), dbUser.getPhoneNumber())
        && Objects.equals(strapiUser.getBirthDate(), dbUser.getBirthDate())
        && Objects.equals(strapiUser.getGender(), dbUser.getGender())
        && Objects.equals(strapiUser.getOccupation(), dbUser.getOccupation())
        && Objects.equals(strapiUser.getEducation(), dbUser.getEducation())
        && Objects.equals(strapiUser.getNationality(), dbUser.getNationality())
        && Objects.equals(strapiUser.getSsn(), dbUser.getSsn())
        && Objects.equals(strapiUser.getTokenReset(), dbUser.getTokenReset())
        && Objects.equals(strapiUser.getAttributes(), dbUser.getAttributes());
  }
}
