package io.github.giovannilamarmora.accesssphere.scheduler;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.MDCUtils;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Logged
@Service
public class ClientSyncScheduler {

  @Value(value = "${env:Default}")
  private String env;

  public static final String TRACE_ID_KEY = "traceId";
  public static final String SPAN_ID_KEY = "spanId";
  public static final String PARENT_ID_KEY = "parentId";
  public static final String ENV_KEY = "env";

  private static final Logger LOG = LoggerFactory.getLogger(ClientSyncScheduler.class);

  @Autowired private ClientService clientService;

  // @Scheduled(initialDelay = 1000)
  @Scheduled(cron = "0 0 0 * * *")
  @LogInterceptor(type = LogTimeTracker.ActionType.SCHEDULER)
  public void syncClients() {
    MDCUtils.registerDefaultMDC(env).subscribe();
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    if (clientService.getIsStrapiEnabled()) {
      LOG.info("\uD83D\uDE80 Starting Scheduler client sync with Strapi");
      Mono<List<ClientCredential>> strapiClientsMono = clientService.getStrapiClientCredentials();
      Mono<List<ClientCredential>> dbClientsMono =
          clientService
              .getClientsFromDatabase()
              .onErrorResume(
                  throwable -> {
                    if (throwable instanceof OAuthException) {
                      return Mono.just(List.of());
                    }
                    return Mono.error(throwable);
                  });

      strapiClientsMono
          .zipWith(dbClientsMono)
          .contextWrite(MDCUtils.contextViewMDC(env))
          .doOnEach(signal -> MDCUtils.setContextMap(contextMap))
          .subscribe(
              result -> {
                // MDC.setContextMap(contextMap);
                List<ClientCredential> strapiClients = result.getT1();
                List<ClientCredential> dbClients = result.getT2();
                syncClients(strapiClients, dbClients);
                LOG.info("\uD83D\uDE80 Scheduler finished successfully!");
              },
              error -> LOG.error("Error occurred during client sync", error));
    } else {
      LOG.info("Strapi is not enabled, skipping client sync");
    }
  }

  private void syncClients(List<ClientCredential> strapiClients, List<ClientCredential> dbClients) {
    // Convert the list of dbClients to a map for quick lookup
    Map<String, ClientCredential> dbClientMap =
        dbClients.stream()
            .collect(Collectors.toMap(ClientCredential::getClientId, Function.identity()));

    // Iterate over strapiClients to add or update clients in the database
    for (ClientCredential strapiClient : strapiClients) {
      ClientCredential dbClient = dbClientMap.get(strapiClient.getClientId());

      if (dbClient == null) {
        // Add new client to the database
        clientService.addClientToDatabase(strapiClient);
      } else {
        // Update existing client in the database if there are changes
        if (!isClientEqual(strapiClient, dbClient)) {
          strapiClient.setId(dbClient.getId());
          clientService.updateClientInDatabase(strapiClient);
        } else LOG.info("Data already updated for client={}", dbClient.getClientId());
      }
    }

    // Optionally handle deletion of clients that are no longer in Strapi
    Set<String> strapiClientIds =
        strapiClients.stream().map(ClientCredential::getClientId).collect(Collectors.toSet());

    for (ClientCredential dbClient : dbClients) {
      if (!strapiClientIds.contains(dbClient.getClientId())) {
        clientService.deleteClientFromDatabase(dbClient);
      }
    }
  }

  private boolean isClientEqual(ClientCredential strapiClient, ClientCredential dbClient) {
    // Compare all relevant fields to determine if they are equal
    return Objects.equals(strapiClient.getExternalClientId(), dbClient.getExternalClientId())
        && Objects.equals(strapiClient.getClientSecret(), dbClient.getClientSecret())
        && Objects.equals(strapiClient.getScopes(), dbClient.getScopes())
        && Objects.equals(strapiClient.getRedirect_uri(), dbClient.getRedirect_uri())
        && Objects.equals(strapiClient.getAccessType(), dbClient.getAccessType())
        && Objects.equals(strapiClient.getAuthType(), dbClient.getAuthType())
        && Objects.equals(strapiClient.getTokenType(), dbClient.getTokenType())
        && Objects.equals(strapiClient.getJwtSecret(), dbClient.getJwtSecret())
        && Objects.equals(strapiClient.getJwtExpiration(), dbClient.getJwtExpiration())
        && Objects.equals(strapiClient.getJweSecret(), dbClient.getJweSecret())
        && Objects.equals(strapiClient.getJweExpiration(), dbClient.getJweExpiration())
        && Objects.equals(strapiClient.getRegistrationToken(), dbClient.getRegistrationToken())
        && Objects.equals(strapiClient.getDefaultRole(), dbClient.getDefaultRole())
        && Objects.equals(strapiClient.getAppRoles(), dbClient.getAppRoles())
        && Objects.equals(strapiClient.getIdToken(), dbClient.getIdToken())
        && Objects.equals(strapiClient.getAccessToken(), dbClient.getAccessToken())
        && Objects.equals(strapiClient.getStrapiToken(), dbClient.getStrapiToken());
  }
}
