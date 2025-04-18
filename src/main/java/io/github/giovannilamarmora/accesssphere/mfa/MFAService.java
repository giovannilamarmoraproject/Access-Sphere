package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.data.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.UserDataValidator;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.auth.MFAAuthenticationService;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategy;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.mfa.MFATokenDataService;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFATokenData;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@Logged
public class MFAService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private UserDataService dataService;
  @Autowired private MFAStrategyFactory strategyFactory;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private MFATokenDataService mfaTokenDataService;
  @Autowired private MFAAuthenticationService mfaAuthenticationService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> generateSecretForUser(MFASetupRequest setupRequest) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Setup MFA for user: {} process started.", setupRequest.userID());
    UserDataValidator.validateIdentifier(setupRequest.userID(), accessTokenData.getIdentifier());
    Mono<User> userMono = dataService.getUserByIdentifier(setupRequest.userID(), true);
    return userMono
        .flatMap(
            user -> {
              MFAStrategy strategy = strategyFactory.getStrategy(setupRequest.mfaMethod());
              return strategy.generateSecret(user, setupRequest);
            })
        .doOnSuccess(
            response -> LOG.info("✅ Setup MFA for user: {} process ended.", setupRequest.userID()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> confirmMFA(MFAConfirmationRequest confirmationRequest) {
    LOG.info("🔑 MFA OTP Confirmation for user: {} process started.", confirmationRequest.userID());

    UserDataValidator.validateIdentifier(
        confirmationRequest.userID(), accessTokenData.getIdentifier());
    Mono<User> userMono = dataService.getUserByIdentifier(confirmationRequest.userID(), true);
    return userMono
        .flatMap(
            user -> {
              MFAMethod mfaMethod =
                  MFAMapper.getMFAMethod(user.getMfaSettings(), confirmationRequest);

              MFAStrategy strategy = strategyFactory.getStrategy(mfaMethod.getType());

              strategy.verifyCode(
                  List.of(mfaMethod), confirmationRequest.otp(), user.getIdentifier());

              user.setMfaSettings(MFAMapper.generateFinalMFA(user, mfaMethod));

              return dataService
                  .updateUser(false, user)
                  .map(
                      user1 -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "MFA Confirmed",
                                TraceUtils.getSpanID(),
                                null);
                        return ResponseEntity.ok(response);
                      });
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "✅ MFA OTP Confirmation for user: {} process ended.",
                    confirmationRequest.userID()));
  }

  public Mono<ResponseEntity<Response>> verifyMfa(
      MfaVerificationRequest mfaRequest, String bearer, ServerWebExchange exchange) {
    LOG.info("🔑 MFA OTP Verification for method: {} process started.", mfaRequest.mfaMethod());

    MFATokenData mfaTokenData = mfaTokenDataService.getByTempToken(bearer);

    MFAValidator.validateMFAMethods(mfaRequest.mfaMethod(), mfaTokenData.getMfaMethods());

    Mono<User> userMono = dataService.getUserByIdentifier(mfaTokenData.getIdentifier(), true);

    MFAStrategy strategy = strategyFactory.getStrategy(mfaRequest.mfaMethod());

    return userMono
        .flatMap(
            user -> {
              strategy.verifyCode(
                  user.getMfaSettings().getMfaMethods(), mfaRequest.otp(), user.getIdentifier());

              return mfaAuthenticationService.verifyMfaAndGenerateToken(
                  mfaRequest, mfaTokenData, user, exchange, dataService);
            })
        .doOnSuccess(
            _ ->
                LOG.info(
                    "✅ MFA OTP Verification for method: {} process ended.",
                    mfaRequest.mfaMethod()));
  }
}
