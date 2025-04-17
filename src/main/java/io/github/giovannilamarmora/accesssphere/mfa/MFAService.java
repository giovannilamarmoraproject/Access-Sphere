package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.data.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategy;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Logged
public class MFAService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private UserDataService dataService;
  @Autowired private MFAStrategyFactory strategyFactory;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> generateSecretForUser(MFASetupRequest setupRequest) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Setup MFA for user: {} process started.", setupRequest.userID());
    Mono<User> userMono = dataService.getUserByIdentifier(setupRequest.userID(), true);
    return userMono
        .flatMap(
            user -> {
              MFAStrategy strategy = strategyFactory.getStrategy(setupRequest.type());
              return strategy.generateSecret(user, setupRequest);
            })
        .doOnSuccess(
            response -> LOG.info("✅ Setup MFA for user: {} process ended.", setupRequest.userID()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> confirmMFA(MFAConfirmationRequest confirmationRequest) {
    LOG.info("🔑 MFA OTP Confirmation for user: {} process started.", confirmationRequest.userID());

    Mono<User> userMono = dataService.getUserByIdentifier(confirmationRequest.userID(), true);
    return userMono
        .flatMap(
            user -> {
              MFAMethod mfaMethod =
                  MFAMapper.getMFAMethod(user.getMfaSettings(), confirmationRequest);

              MFAStrategy strategy = strategyFactory.getStrategy(mfaMethod.getType());

              return strategy.verifyCode(user, mfaMethod, confirmationRequest);
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "✅ MFA OTP Confirmation for user: {} process ended.",
                    confirmationRequest.userID()));
  }
}
