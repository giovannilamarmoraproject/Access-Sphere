package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiEmailTemplate;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiLocale;
import io.github.giovannilamarmora.accesssphere.data.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.MFAMapper;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.utilities.OneTimeCodeUtils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@Logged
public class EmailOtpStrategy implements MFAStrategy {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private UserDataService dataService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private StrapiService strapiService;

  /** Attivazione (invio del primo codice) */
  public Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest req) {
    LOG.info("🔑  Generazione EMAIL-OTP per {}", req.identifier());

    String otp = OneTimeCodeUtils.generateNumericCode(6);
    String hashedOtp = OneTimeCodeUtils.hash(otp);
    long ttlSeconds = 300; // 5 min
    long expiry = Instant.now().plusSeconds(ttlSeconds).toEpochMilli();

    MFASetting setting = MFAMapper.generateEmailMFA(user, req, hashedOtp, expiry);

    user.setMfaSettings(setting);

    Mono<List<StrapiLocale>> strapiLocaleMono = strapiService.locales();
String locale = ObjectToolkit.getOrDefault(req.locale(), "en-GB");
    Mono<String> finalLocaleMono =
            strapiLocaleMono.map(
                    strapiLocales ->
                            strapiLocales.stream()
                                    .filter(loc -> loc.getCode().equals(locale))
                                    .findFirst()
                                    .map(
                                            foundLocale -> {
                                              LOG.info("Locale found: {}", foundLocale.getCode());
                                              return foundLocale.getCode();
                                            })
                                    .orElseGet(
                                            () ->
                                                    strapiLocales.stream()
                                                            .filter(loc -> Boolean.TRUE.equals(loc.getIsDefault()))
                                                            .findFirst()
                                                            .map(
                                                                    defaultLocale -> {
                                                                      LOG.warn(
                                                                              "Locale '{}' not found. Using default locale: {}",
                                                                              locale,
                                                                              defaultLocale.getCode());
                                                                      return defaultLocale.getCode();
                                                                    })
                                                            .orElseGet(
                                                                    () -> {
                                                                      LOG.error(
                                                                              "Locale '{}' not found and no default locale available. Using fallback: en-GB",
                                                                              locale);
                                                                      return "en-GB";
                                                                    })));

    Mono<StrapiEmailTemplate> strapiEmailTemplateMono =
            finalLocaleMono.flatMap(
                    finalLocale ->
                            strapiService.getTemplateById(changePassword.getTemplateId(), finalLocale));

    return dataService
        .updateUser(false, user)
        .map(
            user1 -> {
              EmailContent emailContent =
                  EmailContent.builder()
                      .subject(user1..getSubject())
                      .to(changePassword.getEmail())
                      .sentDate(new Date())
                      .build();
              emailSenderService.sendEmail(
                  user.getEmail(), "Il tuo codice di verifica", buildMailBody(otp, ttlSeconds));
            })
        .thenReturn(
            ResponseEntity.ok(
                new Response(HttpStatus.OK.value(), "OTP inviato", TraceUtils.getSpanID(), null)))
        .doOnSuccess(r -> LOG.info("📧  OTP e-mail inviato a {}", user.getEmail()));
  }

  /** Verifica */
  @Override
  public void verifyCode(List<MFAMethod> methods, String otp, String identifier) {

    boolean ok =
        methods.stream()
            .filter(m -> m.getType() == MFAType.EMAIL_OTP)
            .anyMatch(
                m -> {
                  boolean notExpired =
                      m.getExpiresAt() != null && m.getExpiresAt() > System.currentTimeMillis();
                  return notExpired && OneTimeCodeUtils.matches(otp, m.getHashedCode());
                });

    if (!ok) {
      LOG.error("❌ OTP e-mail non valida per {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, ExceptionType.INVALID_OTP_CODE, "Invalid OTP code");
    }
    LOG.info("✅ OTP e-mail verificato per {}", identifier);
  }

  /** Corpo HTML minimal dell’e-mail */
  private String buildMailBody(String code, long ttlSeconds) {
    return """
          <p>Ciao,</p>
          <p>ecco il tuo codice di verifica:</p>
          <h2 style="letter-spacing:3px;">%s</h2>
          <p>Scade tra %d minuti.</p>
          <p>Se non hai richiesto questo codice ignora la mail.</p>
          """
        .formatted(code, ttlSeconds / 60);
  }
}
