package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MFAStrategyFactory {
  private static final Logger LOG = LoggerFilter.getLogger(MFAStrategyFactory.class);

  private final TotpStrategy totpStrategy;
  private final EmailOtpStrategy emailOtpStrategy;

  public MFAStrategy getStrategy(MFAType mfaType) {
    switch (mfaType) {
      case TOTP:
        return totpStrategy;
      case EMAIL:
        return emailOtpStrategy;
      default:
        throw new MFAException(ExceptionMap.ERR_MFA_400, "Unsupported MFA type: " + mfaType);
    }
  }
}
