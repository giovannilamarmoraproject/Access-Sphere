package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class MFAValidator {

  private static final Logger LOG = LoggerFilter.getLogger(OAuthValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateMFAMethods(MFAType actual, List<MFAType> expected) {
    if (ObjectToolkit.isNullOrEmpty(actual)) {
      LOG.error("The Client must have a valid MFA method to proceed");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_MFA_METHOD,
          "Invalid or missing MFA Method!");
    }

    if (!expected.contains(actual)) {
      LOG.error("MFA method {} is not valid. Expected methods: {}", actual, expected);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_MFA_METHOD,
          "Invalid MFA Method! Expected one of: " + expected);
    }
  }
}
