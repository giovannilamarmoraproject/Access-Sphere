package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Factory per la risoluzione della strategia di autenticazione a due fattori corrispondente al {@link MFAType}.
 */
@Service
@RequiredArgsConstructor
public class MFAStrategyFactory {
  private static final Logger LOG = LoggerFilter.getLogger(MFAStrategyFactory.class);

  private final TotpStrategy totpStrategy;
  private final EmailStrategy emailStrategy;

  /**
   * Restituisce l'implementazione concreta di {@link MFAStrategy} idonea a gestire il tipo MFA specificato.
   *
   * @param mfaType Tipologia di MFA (TOTP o EMAIL).
   * @return Strategia concreta per la gestione del flusso.
   */
  public MFAStrategy getStrategy(MFAType mfaType) {
    if (mfaType == null) {
      LOG.error("❌ MFA type cannot be null in factory resolution");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA type is required");
    }
    switch (mfaType) {
      case TOTP -> {
        return totpStrategy;
      }
      case EMAIL -> {
        return emailStrategy;
      }
      default -> {
        LOG.error("❌ Unsupported MFA type: {}", mfaType);
        throw new MFAException(ExceptionMap.ERR_MFA_400, "Unsupported MFA type: " + mfaType);
      }
    }
  }
}
