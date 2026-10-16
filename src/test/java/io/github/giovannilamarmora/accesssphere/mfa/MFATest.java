package io.github.giovannilamarmora.accesssphere.mfa;

import static org.junit.jupiter.api.Assertions.*;

import io.github.giovannilamarmora.accesssphere.mfa.dto.*;
import io.github.giovannilamarmora.accesssphere.mfa.model.*;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.EmailStrategy;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.TotpStrategy;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MFATest {

  @Test
  @DisplayName("TOTPLabel should parse correctly and support EMAIL")
  void testTotpLabelParsing() {
    assertEquals(TOTPLabel.GOOGLE_AUTHENTICATOR, TOTPLabel.fromString("google-authenticator"));
    assertEquals(TOTPLabel.MICROSOFT_AUTHENTICATOR, TOTPLabel.fromString("MICROSOFT_AUTHENTICATOR"));
    assertEquals(TOTPLabel.EMAIL, TOTPLabel.fromString("email"));
    assertEquals(TOTPLabel.EMAIL, TOTPLabel.fromString("EMAIL"));
    assertNull(TOTPLabel.fromString(null));
    assertNull(TOTPLabel.fromString("   "));
    assertNull(TOTPLabel.fromString("unknown-label-xyz"));
  }

  @Test
  @DisplayName("MFAUtils should generate 6-digit email OTP and format/parse secrets")
  void testMFAUtils() {
    String otp = MFAUtils.generateEmailOtpCode();
    assertNotNull(otp);
    assertEquals(6, otp.length());
    assertTrue(otp.matches("^\\d{6}$"));

    long expiry = System.currentTimeMillis() + 60000;
    String formattedSecret = MFAUtils.formatEmailOtpSecret(otp, expiry);
    assertTrue(formattedSecret.startsWith(otp + ":"));

    String secret = MFAUtils.generateSecret();
    assertNotNull(secret);

    String otpAuthUrl = MFAUtils.getOtpAuthUrl("user@test.com", secret, "Access-Sphere");
    assertTrue(otpAuthUrl.startsWith("otpauth://totp/"));
    assertTrue(otpAuthUrl.contains("user@test.com"));
  }

  @Test
  @DisplayName("MFAValidator should validate OTP formats and methods correctly")
  void testMFAValidator() {
    assertDoesNotThrow(() -> MFAValidator.validateOtpFormat("123456"));
    assertThrows(MFAException.class, () -> MFAValidator.validateOtpFormat("12345"));
    assertThrows(MFAException.class, () -> MFAValidator.validateOtpFormat("1234567"));
    assertThrows(MFAException.class, () -> MFAValidator.validateOtpFormat("abcdef"));
    assertThrows(MFAException.class, () -> MFAValidator.validateOtpFormat(null));

    assertDoesNotThrow(() -> MFAValidator.validateMFAMethods(MFAType.TOTP, List.of(MFAType.TOTP, MFAType.EMAIL)));
    assertDoesNotThrow(() -> MFAValidator.validateMFAMethods(MFAType.EMAIL, List.of(MFAType.TOTP, MFAType.EMAIL)));
    assertThrows(MFAException.class, () -> MFAValidator.validateMFAMethods(MFAType.EMAIL, List.of(MFAType.TOTP)));
  }

  @Test
  @DisplayName("MFAValidator should validate setup and confirmation requests")
  void testMFAValidatorRequests() {
    MFASetupRequest validTotp = new MFASetupRequest("usr-1", TOTPLabel.GOOGLE_AUTHENTICATOR, MFAType.TOTP, false);
    assertDoesNotThrow(() -> MFAValidator.validateSetupRequest(validTotp));

    MFASetupRequest validEmail = new MFASetupRequest("usr-1", null, MFAType.EMAIL, false);
    assertDoesNotThrow(() -> MFAValidator.validateSetupRequest(validEmail));

    MFASetupRequest invalidTotpNoLabel = new MFASetupRequest("usr-1", null, MFAType.TOTP, false);
    assertThrows(MFAException.class, () -> MFAValidator.validateSetupRequest(invalidTotpNoLabel));

    MFAConfirmationRequest validConfirm = new MFAConfirmationRequest("usr-1", null, MFAType.EMAIL, "123456");
    assertDoesNotThrow(() -> MFAValidator.validateConfirmationRequest(validConfirm));

    MFAConfirmationRequest invalidConfirmOtp = new MFAConfirmationRequest("usr-1", null, MFAType.EMAIL, "123");
    assertThrows(MFAException.class, () -> MFAValidator.validateConfirmationRequest(invalidConfirmOtp));
  }

  @Test
  @DisplayName("MFAMapper should create temporary and final MFA settings for Email and TOTP")
  void testMFAMapper() {
    User user = new User();
    user.setIdentifier("usr-100");

    MFASetupRequest setupRequest = new MFASetupRequest("usr-100", null, MFAType.EMAIL, false);
    String secret = MFAUtils.formatEmailOtpSecret("654321", System.currentTimeMillis() + 600000);

    MFASetting tempSetting = MFAMapper.generateTempMFA(user, setupRequest, secret);
    assertNotNull(tempSetting);
    assertFalse(tempSetting.getEnabled());
    assertEquals(1, tempSetting.getMfaMethods().size());

    MFAMethod tempMethod = tempSetting.getMfaMethods().getFirst();
    assertEquals(MFAType.EMAIL, tempMethod.getType());
    assertEquals(TOTPLabel.EMAIL, tempMethod.getLabel());
    assertFalse(tempMethod.isConfirmed());

    user.setMfaSettings(tempSetting);
    MFAConfirmationRequest confirmRequest = new MFAConfirmationRequest("usr-100", null, MFAType.EMAIL, "654321");
    MFAMethod foundMethod = MFAMapper.getMFAMethod(user.getMfaSettings(), confirmRequest);
    assertNotNull(foundMethod);

    MFASetting finalSetting = MFAMapper.generateFinalMFA(user, foundMethod);
    assertTrue(finalSetting.getEnabled());
    assertTrue(finalSetting.getMfaMethods().getFirst().isConfirmed());
  }

  @Test
  @DisplayName("MFAStrategyFactory should resolve both TOTP and EMAIL strategies")
  void testStrategyFactory() {
    TotpStrategy totp = new TotpStrategy();
    EmailStrategy email = new EmailStrategy();
    MFAStrategyFactory factory = new MFAStrategyFactory(totp, email);

    assertSame(totp, factory.getStrategy(MFAType.TOTP));
    assertSame(email, factory.getStrategy(MFAType.EMAIL));
    assertThrows(MFAException.class, () -> factory.getStrategy(null));
  }

  @Test
  @DisplayName("EmailStrategy should verify valid OTP and reject invalid or expired OTP")
  void testEmailStrategyVerification() {
    EmailStrategy emailStrategy = new EmailStrategy();
    long future = System.currentTimeMillis() + 600000;
    long past = System.currentTimeMillis() - 10000;

    MFAMethod validMethod = new MFAMethod(MFAType.EMAIL, TOTPLabel.EMAIL, "999888:" + future, true, LocalDateTime.now(), LocalDateTime.now());
    MFAMethod expiredMethod = new MFAMethod(MFAType.EMAIL, TOTPLabel.EMAIL, "111222:" + past, true, LocalDateTime.now(), LocalDateTime.now());

    // Valid OTP
    assertDoesNotThrow(() -> emailStrategy.verifyCode(List.of(validMethod), "999888", "usr-1"));

    // Incorrect code
    assertThrows(MFAException.class, () -> emailStrategy.verifyCode(List.of(validMethod), "000000", "usr-1"));

    // Expired code
    assertThrows(MFAException.class, () -> emailStrategy.verifyCode(List.of(expiredMethod), "111222", "usr-1"));
  }
}
