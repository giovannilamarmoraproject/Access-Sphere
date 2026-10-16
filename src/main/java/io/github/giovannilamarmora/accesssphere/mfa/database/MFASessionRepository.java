package io.github.giovannilamarmora.accesssphere.mfa.database;

import io.github.giovannilamarmora.accesssphere.mfa.entity.MFASessionEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository di persistenza per l'aggregato delle sessioni e dei token temporanei di autenticazione MFA.
 *
 * <p>Incapsula l'accesso dati verso {@link IMFASessionDAO}, aggiungendo tracciamento strutturato del ciclo di vita.
 */
@Repository
public class MFASessionRepository {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private IMFASessionDAO mfaSessionDAO;

  /**
   * Salva o aggiorna un record di sessione MFA su database.
   *
   * @param entity Entità sessione da salvare.
   * @return Entità salvata con identificatore generato.
   */
  public MFASessionEntity save(MFASessionEntity entity) {
    LOG.debug("🔐 Persisting MFA session entity for user [{}]", entity.getIdentifier());
    MFASessionEntity saved = mfaSessionDAO.save(entity);
    LOG.debug("⚖️ Successfully persisted MFA session ID [{}]", saved.getId());
    return saved;
  }

  /**
   * Ricerca i record di sessione associati a un token temporaneo o token dispositivo fidato.
   *
   * @param token Stringa del token.
   * @return Lista delle entità corrispondenti.
   */
  public List<MFASessionEntity> findByTempTokenOrDeviceToken(String token) {
    LOG.debug("🔐 Querying MFA session by token");
    List<MFASessionEntity> list = mfaSessionDAO.findByTempTokenOrDeviceToken(token);
    if (ObjectToolkit.isNullOrEmpty(list)) {
      LOG.debug("⚖️ No MFA session found for token");
    } else {
      LOG.debug("⚖️ Found {} matching MFA session(s)", list.size());
    }
    return list;
  }

  /**
   * Revoca tutti i token temporanei attivi per l'utente eccetto quello attualmente in corso di validazione.
   *
   * @param status Stato revocato da impostare.
   * @param tempToken Token corrente da preservare.
   * @param identifier Identificativo dell'utente.
   * @param currentTime Timestamp epoch corrente.
   */
  public void revokeTokensExcept(
      TokenStatus status, String tempToken, String identifier, long currentTime) {
    LOG.debug("🔐 Revoking other MFA tokens for user [{}]", identifier);
    mfaSessionDAO.revokeTokensExcept(status, tempToken, identifier, currentTime);
  }

  /**
   * Rimuove fisicamente dal database le sessioni MFA scadute o revocate.
   */
  public void deleteExpiredToken() {
    LOG.info("🧹 Purging expired and revoked MFA sessions from database");
    mfaSessionDAO.deleteExpiredToken();
  }
}
