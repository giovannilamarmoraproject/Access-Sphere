package io.github.giovannilamarmora.accesssphere.client.database;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientMapper;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class OAuth2ClientRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IOAuth2ClientDAO oAuth2ClientDAO;

  public Optional<List<OAuth2Client>> findAll() {
    LOG.debug("🔐 Getting all OAuth2 Clients");
    List<OAuth2ClientEntity> getEntityList = oAuth2ClientDAO.findAll();

    if (ObjectToolkit.isNullOrEmpty(getEntityList)) {
      LOG.warn("❌ OAuth2 Clients not found in Database");
      return Optional.empty();
    }

    LOG.debug("⚖️ Found {} OAuth2 Clients in Database", getEntityList.size());
    return Optional.of(OAuth2ClientMapper.toModelList(getEntityList));
  }

  public Optional<OAuth2Client> findByClientId(String clientId) {
    LOG.debug("🔐 Getting OAuth2 Client for client ID: {}", clientId);
    OAuth2ClientEntity getEntity = oAuth2ClientDAO.findByClientId(clientId);

    if (ObjectToolkit.isNullOrEmpty(getEntity)) {
      LOG.warn("❌ OAuth2 Client: {} not found in Database", clientId);
      return Optional.empty();
    }

    LOG.debug("⚖️ Found OAuth2 Client {} in Database", clientId);
    return Optional.of(OAuth2ClientMapper.toModel(getEntity));
  }

  public Boolean existsByClientId(String clientId) {
    LOG.debug("🔐 Checking if OAuth2 Client {} exist", clientId);
    return oAuth2ClientDAO.existsByClientId(clientId);
  }

  public OAuth2Client save(OAuth2Client oAuth2Client) {
    LOG.debug("🔐 Saving OAuth2 Client for client ID: {}", oAuth2Client.getClientId());
    OAuth2ClientEntity mapEntity = OAuth2ClientMapper.toEntity(oAuth2Client);

    OAuth2ClientEntity savedEntity = oAuth2ClientDAO.save(mapEntity);

    LOG.debug("⚖️ Saved OAuth2 Client {} in Database", oAuth2Client.getClientId());
    return OAuth2ClientMapper.toModel(savedEntity);
  }

  public void deleteByClientId(String clientId) {
    LOG.debug("🔐 Deleting OAuth2 Client with clientId: {}", clientId);
    oAuth2ClientDAO.deleteByClientId(clientId);
    LOG.debug("⚖️ Deleted OAuth2 Client with clientId: {} in Database", clientId);
  }
}
