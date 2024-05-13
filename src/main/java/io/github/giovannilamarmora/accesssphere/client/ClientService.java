package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiException;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class ClientService {
  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  @Autowired private StrapiClient strapiClient;
  // TODO: [CACHE] Valuta se inserire la cache
  @Autowired private IClientDAO iClientDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ClientCredential> getClientCredentialByClientID(String clientID) {
    if (isStrapiEnabled) {
      LOG.info("Strapi is active");
      Mono<ResponseEntity<StrapiResponse>> strapiData = strapiClient.getClientByClientID(clientID);
      return strapiData
          .map(
              responseEntity -> {
                if (ObjectUtils.isEmpty(responseEntity.getBody())) {
                  LOG.error("Strapi returned an empty object");
                  throw new StrapiException(ExceptionMap.ERR_STRAPI_404.getMessage());
                }
                return StrapiMapper.mapFromStrapiResponseToClientCredential(
                    responseEntity.getBody());
              })
          .onErrorResume(
              throwable -> {
                LOG.info(
                    "Error on strapi, getting data from database, message is {}",
                    throwable.getMessage());
                return getClientFromDatabaseByClientID(clientID);
              });
    }
    return getClientFromDatabaseByClientID(clientID);
  }

  private Mono<ClientCredential> getClientFromDatabaseByClientID(String clientID) {
    return Mono.just(iClientDAO
        .findByClientId(clientID))
        .map(ClientMapper::fromClientCredentialEntityToClientCredential);
  }
}
