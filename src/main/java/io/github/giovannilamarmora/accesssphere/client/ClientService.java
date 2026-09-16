package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserValidator;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Service per la gestione dei Client OAuth2. Fornisce funzionalita di base (lettura, inserimento,
 * aggiornamento, eliminazione) basate esclusivamente sul database locale.
 */
@Logged
@Service
public class ClientService {
  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private AccessTokenData accessTokenData;
  @Autowired private IClientDAO iClientDAO;
  @Autowired private TechUserService techUserService;

  /**
   * Recupera le credenziali di un Client dato il suo clientID.
   *
   * @param clientID Identificativo univoco del client.
   * @return {@link Mono} contenente il {@link ClientCredential}.
   * @throws OAuthException se il client non viene trovato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ClientCredential> getClientCredentialByClientID(String clientID) {
    LOG.info("Searching for credentials for client ID: {}", clientID);
    ClientCredentialEntity clientCredentialEntity = iClientDAO.findByClientId(clientID);
    if (ObjectUtils.isEmpty(clientCredentialEntity)) {
      LOG.error("Client with ID: {} not found in Database", clientID);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }
    return Mono.just(clientCredentialEntity).map(ClientMapper::toDTO);
  }

  /** Recupera la lista di tutti i Client restituiti come oggetto Response reattivo. */
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

  /**
   * Recupera la lista di tutti i ClientCredential dal database. Applica validazione sui permessi
   * del tech client.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<ClientCredential>> getClientCredentials() {
    LOG.info("Retrieving all registered clients from Database");
    List<ClientCredentialEntity> clientCredentialEntity = iClientDAO.findAll();
    if (ObjectUtils.isEmpty(clientCredentialEntity)) {
      LOG.error("No clients found in Database");
      return Mono.error(new OAuthException(ExceptionMap.ERR_OAUTH_404, "Clients not found!"));
    }
    return Mono.just(clientCredentialEntity)
        .map(ClientMapper::toDTOList)
        .map(
            client -> {
              TechUserValidator.validateTechClient(
                  client, accessTokenData, techUserService.getTech_client_id());
              return client;
            });
  }

  /** Inserisce un nuovo ClientCredential a database. */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void addClientToDatabase(ClientCredential clientCredential) {
    LOG.info("Inserting new client into Database: {}", clientCredential.getClientId());
    ClientCredentialEntity clientEntity = ClientMapper.toEntity(clientCredential);
    iClientDAO.save(clientEntity);
    LOG.debug("Client {} successfully inserted", clientCredential.getClientId());
  }

  /**
   * Aggiorna un ClientCredential esistente a database.
   *
   * @throws IllegalArgumentException se il client non esiste.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void updateClientInDatabase(ClientCredential clientCredential) {
    LOG.info("Updating client in Database: {}", clientCredential.getClientId());
    ClientCredentialEntity existingClientEntity =
        iClientDAO.findByClientId(clientCredential.getClientId());
    if (!ObjectUtils.isEmpty(existingClientEntity)) {
      ClientMapper.updateEntity(existingClientEntity, clientCredential);
      iClientDAO.save(existingClientEntity);
      LOG.debug("Client {} successfully updated", clientCredential.getClientId());
    } else {
      LOG.error("Cannot update: Client {} not found", clientCredential.getClientId());
      throw new IllegalArgumentException("Client not found: " + clientCredential.getClientId());
    }
  }

  /** Rimuove un ClientCredential dal database. */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteClientFromDatabase(ClientCredential clientCredential) {
    LOG.info("Removing client from Database: {}", clientCredential.getClientId());
    ClientCredentialEntity existingClientOptional =
        iClientDAO.findByClientId(clientCredential.getClientId());
    if (!ObjectUtils.isEmpty(existingClientOptional)) {
      iClientDAO.delete(existingClientOptional);
      LOG.debug("Client {} successfully removed", clientCredential.getClientId());
    } else {
      LOG.warn("No action taken: Client {} not found for removal", clientCredential.getClientId());
    }
  }
}
