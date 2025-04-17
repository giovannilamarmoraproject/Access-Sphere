package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.github.giovannilamarmora.accesssphere.data.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.MFAMapper;
import io.github.giovannilamarmora.accesssphere.mfa.MFAUtils;
import io.github.giovannilamarmora.accesssphere.mfa.dto.*;
import io.github.giovannilamarmora.accesssphere.utilities.QRCodeUtils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Logged
public class TotpStrategy implements MFAStrategy {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private UserDataService dataService;

  public Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest setupRequest) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Generate TOTP MFA for user: {} process started.",
        setupRequest.userID());
    String secret = MFAUtils.generateSecret();
    String otpAuth = MFAUtils.getOtpAuthUrl(user.getEmail(), secret, "Access-Sphere");

    MFASetting mfaSetting = MFAMapper.generateTempMFA(user, setupRequest, secret);

    user.setMfaSettings(mfaSetting);
    return dataService
        .updateUser(false, user)
        .map(
            userUpdated -> {
              MFASetupResponse mfaSetupResponse = new MFASetupResponse(secret, otpAuth, null);
              if (setupRequest.generateImage()) {
                String base64 =
                    "data:image/png;base64,"
                        + Base64.getEncoder()
                            .encodeToString(
                                QRCodeUtils.generateQRCodeWithLogoFromUrl(
                                    otpAuth,
                                    300,
                                    300,
                                    "https://access-sphere.giovannilamarmora.com/img/Access%20Sphere%20Transparent%20512x512.png"));
                mfaSetupResponse = mfaSetupResponse.addQRCode(base64);
              }
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "MFA Generated",
                      TraceUtils.getSpanID(),
                      mfaSetupResponse);
              return ResponseEntity.ok(response);
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "\uD83E\uDD37\u200D♂\uFE0F Generate TOTP MFA for user: {} process ended.",
                    setupRequest.userID()));
  }

  @Override
  public Mono<ResponseEntity<Response>> verifyCode(
      User user, MFAMethod mfaMethod, MFAConfirmationRequest confirmationRequest) {
    GoogleAuthenticator gAuth = new GoogleAuthenticator();
    boolean isValid =
        gAuth.authorize(mfaMethod.getSecretKey(), Integer.parseInt(confirmationRequest.otp()));

    if (!isValid) {
      LOG.error("❌ Invalid OTP code for user: {}", confirmationRequest.userID());
      throw new MFAException(ExceptionMap.ERR_MFA_400, "Invalid OTP code");
    }

    user.setMfaSettings(MFAMapper.generateFinalMFA(user, mfaMethod));

    return dataService
        .updateUser(false, user)
        .map(
            updatedUser -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(), "MFA Confirmed", TraceUtils.getSpanID(), null);
              return ResponseEntity.ok(response);
            });
  }
}
