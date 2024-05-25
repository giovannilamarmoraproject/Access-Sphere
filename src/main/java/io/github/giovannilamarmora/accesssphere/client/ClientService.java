package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiException;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.LoggerFilter;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class ClientService {
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  @Autowired private StrapiClient strapiClient;
  @Autowired private IClientDAO iClientDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ClientCredential> getClientCredentialByClientID(String clientID) {
    if (isStrapiEnabled) {
      LOG.info("Strapi is active");
      Mono<ResponseEntity<StrapiResponse>> strapiData = strapiClient.getClientByClientID(clientID);
      return strapiData
          .map(
              responseEntity -> {
                if (ObjectUtils.isEmpty(responseEntity.getBody())
                    || ObjectUtils.isEmpty(responseEntity.getBody().getData())) {
                  LOG.error("Strapi returned an empty object");
                  throw new StrapiException(
                      ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
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

  private Mono<ClientCredential> getClientFromDatabaseByClientID(String clientID) {
    ClientCredentialEntity clientCredentialEntity = iClientDAO.findByClientId(clientID);
    if (ObjectUtils.isEmpty(clientCredentialEntity)) {
      if (ObjectUtils.isEmpty(clientCredentialEntity)) {
        LOG.error("Client credential ot found on Database");
        throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
      }
    }
    return Mono.just(clientCredentialEntity)
        .map(ClientMapper::fromClientCredentialEntityToClientCredential);
  }
}
