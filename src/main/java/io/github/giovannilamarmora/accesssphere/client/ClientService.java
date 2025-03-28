package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiException;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Logged
@Service
public class ClientService {
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String STRAPI_STATUS_LOG =
      "\uD83C\uDF10 Strapi status is Active, performing call with strapi client";

  @Getter
  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  @Autowired private AccessTokenData accessTokenData;
  @Autowired private StrapiClient strapiClient;
  @Autowired private IClientDAO iClientDAO;
  @Autowired private TechUserService techUserService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ClientCredential> getClientCredentialByClientID(String clientID) {
    if (isStrapiEnabled) {
      LOG.info(STRAPI_STATUS_LOG);
      Mono<ResponseEntity<StrapiResponse>> strapiData = strapiClient.getClientByClientID(clientID);
      return strapiData
          .map(
              responseEntity -> {
                if (ObjectUtils.isEmpty(responseEntity.getBody())
                    || ObjectUtils.isEmpty(responseEntity.getBody().getData())) {
                  LOG.error("Strapi returned an empty object");
                  throw new StrapiException(
                      ExceptionMap.ERR_OAUTH_400,
                      ExceptionType.INVALID_CLIENT_ID,
                      "Invalid client_id provided!");
                }
                return StrapiMapper.mapFromStrapiResponseToClientCredential(
                    responseEntity.getBody());
              })
          .onErrorResume(
              throwable -> {
                if (!throwable.getMessage().contains("Invalid client_id provided!")) {
                  LOG.info(
                      "Error on strapi, getting data from database, message is {}",
                      throwable.getMessage());
                  return getClientFromDatabaseByClientID(clientID);
                }
                return Mono.error(throwable);
              });
    }
    return getClientFromDatabaseByClientID(clientID);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> getClients() {
    Response response =
        new Response(HttpStatus.OK.value(), "Client credential list", TraceUtils.getSpanID(), null);

    return getClientCredentials()
        .map(
            client -> {
              response.setData(client);
              return ResponseEntity.ok(response);
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<ClientCredential>> getClientCredentials() {
    if (isStrapiEnabled) {
      LOG.info(STRAPI_STATUS_LOG);
      Mono<List<ClientCredential>> clientCredentials = getStrapiClientCredentials();
      return clientCredentials
          .map(
              client -> {
                TechUserValidator.validateTechClient(
                    client, accessTokenData, techUserService.getTech_client_id());
                return client;
              })
          .onErrorResume(
              throwable -> {
                if (!throwable.getMessage().contains("Invalid client_id provided!")
                    && !throwable
                        .getMessage()
                        .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_403.getMessage())) {
                  LOG.info(
                      "Error on strapi, getting data from database, message is {}",
                      throwable.getMessage());
                  return getClientsFromDatabase()
                      .map(
                          client -> {
                            TechUserValidator.validateTechClient(
                                client, accessTokenData, techUserService.getTech_client_id());
                            return client;
                          });
                }
                return Mono.error(throwable);
              });
    }
    return getClientsFromDatabase()
        .map(
            client -> {
              TechUserValidator.validateTechClient(
                  client, accessTokenData, techUserService.getTech_client_id());
              return client;
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  // @Cacheable(value = CLIENT_CREDENTIAL_CACHE, key = "#clientID", condition = "#clientID!=null")
  public Mono<List<ClientCredential>> getStrapiClientCredentials() {
    Mono<ResponseEntity<StrapiResponse>> strapiData = strapiClient.getClients();
    return strapiData.map(
        responseEntity -> {
          if (ObjectUtils.isEmpty(responseEntity.getBody())
              || ObjectUtils.isEmpty(responseEntity.getBody().getData())) {
            LOG.error("Strapi returned an empty object");
            throw new StrapiException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
          }
          return StrapiMapper.mapFromStrapiResponseToClientCredentials(responseEntity.getBody());
        });
  }

  private Mono<ClientCredential> getClientFromDatabaseByClientID(String clientID) {
    ClientCredentialEntity clientCredentialEntity = iClientDAO.findByClientId(clientID);
    if (ObjectUtils.isEmpty(clientCredentialEntity)) {
      if (ObjectUtils.isEmpty(clientCredentialEntity)) {
        LOG.error("Client credential not found on Database");
        throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
      }
    }
    return Mono.just(clientCredentialEntity)
        .map(ClientMapper::fromClientCredentialEntityToClientCredential);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<ClientCredential>> getClientsFromDatabase() {
    List<ClientCredentialEntity> clientCredentialEntity = iClientDAO.findAll();
    if (ObjectUtils.isEmpty(clientCredentialEntity)) {
      LOG.error("Client credential not found on Database");
      return Mono.error(new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client!"));
    }
    return Mono.just(clientCredentialEntity)
        .map(ClientMapper::fromClientCredentialEntitiesToClientCredential);
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void addClientToDatabase(ClientCredential clientCredential) {
    // Convertire ClientCredential in un'entità del database, ad esempio ClientEntity
    ClientCredentialEntity clientEntity =
        ClientMapper.fromClientCredentialToClientCredentialEntity(clientCredential);
    iClientDAO.save(clientEntity);
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void updateClientInDatabase(ClientCredential clientCredential) {
    // Trovare l'entità esistente nel database
    ClientCredentialEntity existingClientEntity =
        iClientDAO.findByClientId(clientCredential.getClientId());
    if (!ObjectUtils.isEmpty(existingClientEntity)) {
      // Aggiornare i campi dell'entità esistente con i valori del ClientCredential
      ClientMapper.updateEntityFields(existingClientEntity, clientCredential);
      iClientDAO.save(existingClientEntity);
    } else {
      // Se il client non esiste, possiamo decidere di aggiungerlo o lanciare un'eccezione
      throw new IllegalArgumentException("Client not found: " + clientCredential.getClientId());
    }
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteClientFromDatabase(ClientCredential clientCredential) {
    // Trovare l'entità esistente nel database
    ClientCredentialEntity existingClientOptional =
        iClientDAO.findByClientId(clientCredential.getClientId());
    if (!ObjectUtils.isEmpty(existingClientOptional)) {
      iClientDAO.delete(existingClientOptional);
    }
  }
}
