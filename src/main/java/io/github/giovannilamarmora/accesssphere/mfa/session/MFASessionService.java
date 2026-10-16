package io.github.giovannilamarmora.accesssphere.mfa.session;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.session.dto.LoginStatus;
import io.github.giovannilamarmora.accesssphere.mfa.session.dto.MFASession;
import io.github.giovannilamarmora.accesssphere.mfa.session.entity.MFASessionEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servizio applicativo per la gestione delle sessioni MFA, emissione dei temp_token e convalida dei device_token.
 */
@Service
@Logged
public class MFASessionService {

  private static final Logger LOG = LoggerFilter.getLogger(MFASessionService.class);
  private static final long TEMP_TOKEN_EXPIRY_MILLIS = 10 * 60 * 1000L; // 10 minuti
  private static final long DEVICE_TOKEN_EXPIRY_MILLIS = 30L * 24 * 60 * 60 * 1000L; // 30 giorni

  @Autowired private MFASessionRepository mfaSessionRepository;

  /**
   * Crea e persiste una nuova sessione MFA in stato PENDING per un utente che sta effettuando il login.
   *
   * @param user Utente in fase di login.
   * @param mfaMethods Metodi MFA confermati disponibili per l'utente.
   * @param clientId Identificativo del client OAuth2 chiamante.
   * @param sessionId Identificativo della sessione HTTP.
   * @param payload Payload o claim addizionali.
   * @return {@link MFASession} creata con il nuovo tempToken.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession save(
      User user, List<String> mfaMethods, String clientId, String sessionId, Object payload) {
    LOG.info("🔐 Creating pending MFA session for user [{}] and client [{}]", user.getIdentifier(), clientId);
    Date now = new Date();
    Date exp = new Date(now.getTime() + TEMP_TOKEN_EXPIRY_MILLIS);
    String tempToken = UUID.randomUUID().toString();

    MFASessionEntity mfaTokenToSave =
        new MFASessionEntity(
            null,
            tempToken,
            null,
            LoginStatus.PENDING,
            String.join(" ", mfaMethods.stream().distinct().toList()),
            user.getUsername(),
            clientId,
            sessionId,
            user.getIdentifier(),
            now.getTime(),
            exp.getTime(),
            Mapper.writeObjectToString(payload),
            TokenStatus.ISSUED);

    MFASessionEntity saved = mfaSessionRepository.save(mfaTokenToSave);
    mfaSessionRepository.revokeTokensExcept(
        TokenStatus.REVOKED, tempToken, user.getIdentifier(), System.currentTimeMillis());

    LOG.info("✅ Pending MFA session [{}] created for user [{}]", saved.getId(), user.getIdentifier());
    return MFASessionMapper.toDto(saved);
  }

  /**
   * Finalizza la sessione MFA in stato COMPLETED al termine della verifica OTP.
   *
   * @param mfaSession Dati della sessione temporanea originaria.
   * @param deviceToken Token dispositivo persistito se l'utente ha richiesto 'remember device'.
   * @param mfaMethod Metodo MFA effettivamente utilizzato.
   * @return {@link MFASession} completata.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession completeLogin(MFASession mfaSession, String deviceToken, String mfaMethod) {
    LOG.info("🔐 Completing MFA session [{}] for user [{}]", mfaSession.getId(), mfaSession.getIdentifier());
    Date now = new Date();
    Date exp = new Date(now.getTime() + DEVICE_TOKEN_EXPIRY_MILLIS);

    MFASessionEntity mfaTokenToSave =
        new MFASessionEntity(
            mfaSession.getId(),
            null,
            deviceToken,
            LoginStatus.COMPLETED,
            mfaMethod,
            mfaSession.getSubject(),
            mfaSession.getClientId(),
            mfaSession.getSessionId(),
            mfaSession.getIdentifier(),
            now.getTime(),
            exp.getTime(),
            Mapper.writeObjectToString(mfaSession.getPayload()),
            TokenStatus.ISSUED);

    MFASessionEntity saved = mfaSessionRepository.save(mfaTokenToSave);
    LOG.info("✅ MFA session [{}] completed successfully", saved.getId());
    return MFASessionMapper.toDto(saved);
  }

  /**
   * Recupera e convalida la sessione MFA corrispondente a un token temporaneo fornito nell'header Authorization.
   *
   * @param token Token Bearer o stringa pura del tempToken.
   * @return {@link MFASession} valida.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession getByTempToken(String token) {
    if (ObjectToolkit.isNullOrEmpty(token)) {
      LOG.error("❌ Token not found, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    String extractedToken =
        token.startsWith("Bearer ") ? token.replaceFirst("Bearer ", "").trim() : token.trim();

    if (extractedToken.isEmpty()) {
      LOG.error("❌ The current Bearer is empty, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    List<MFASessionEntity> mfaTokenEntities =
        mfaSessionRepository.findByTempTokenOrDeviceToken(extractedToken);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("❌ MFA session data not found on Database for provided temp token");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    MFASessionEntity mfaToken = mfaTokenEntities.getFirst();
    Date now = new Date();

    if (now.after(new Date(mfaToken.getExpireDate()))
        || !LoginStatus.PENDING.equals(mfaToken.getLoginStatus())
        || TokenStatus.EXPIRED.equals(mfaToken.getStatus())
        || TokenStatus.REVOKED.equals(mfaToken.getStatus())) {
      LOG.error(
          "❌ The current MFA token is expired on {} with status {} and loginStatus {}",
          new Date(mfaToken.getExpireDate()),
          mfaToken.getStatus(),
          mfaToken.getLoginStatus());
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return MFASessionMapper.toDto(mfaToken);
  }

  /**
   * Ricerca una sessione MFA valida per un token dispositivo fidato.
   *
   * @param token Stringa del device token.
   * @return {@link MFASession} trovata o null se assente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession getByDeviceToken(String token) {
    if (ObjectToolkit.isNullOrEmpty(token)) {
      LOG.error("❌ Device Token not found, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    List<MFASessionEntity> mfaTokenEntities =
        mfaSessionRepository.findByTempTokenOrDeviceToken(token.trim());

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.warn("❌ MFA device token not found in database");
      return null;
    }

    return MFASessionMapper.toDto(mfaTokenEntities.getFirst());
  }

  /**
   * Elimina fisicamente i record di sessione MFA scaduti o revocati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteMFATokenExpired() {
    mfaSessionRepository.deleteExpiredToken();
  }
}
