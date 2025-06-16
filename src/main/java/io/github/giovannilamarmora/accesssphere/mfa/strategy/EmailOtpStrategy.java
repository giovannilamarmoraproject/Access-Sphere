package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.TemplateParam;
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
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Logged
public class EmailOtpStrategy implements MFAStrategy {

  @Value(value = "${rest.client.email-sender.mfaTemplateID}")
  private String otpTemplate;

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private UserDataService dataService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private StrapiService strapiService;

  /** Attivazione (invio del primo codice) */
  public Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest req) {
    LOG.info("🔑 Generate EMAIL-OTP for {}", req.identifier());

    String otp = OneTimeCodeUtils.generateNumericCode(6);
    String hashedOtp = OneTimeCodeUtils.hash(otp);
    long ttlSeconds = 900; // 5 min
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
            finalLocale -> strapiService.getTemplateById(otpTemplate, finalLocale));

    return dataService
        .updateUser(false, user)
        .zipWith(strapiEmailTemplateMono)
        .flatMap(
            objects -> {
              User user1 = objects.getT1();
              StrapiEmailTemplate emailTemplate = objects.getT2();

              Map<String, String> emailParams =
                  TemplateParam.getOTPTemplateParam(objects.getT1(), otp);
              if (!ObjectToolkit.isNullOrEmpty(otp)) emailParams.put("OTP_CODE", otp);

              EmailContent emailContent =
                  EmailContent.builder()
                      .subject(emailTemplate.getSubject())
                      .to(user1.getEmail())
                      .sentDate(new Date())
                      .build();
              return emailSenderService
                  .sendEmail(objects.getT2(), emailParams, emailContent)
                  .flatMap(
                      emailResponse -> {
                        // objects1.getT1().setToken(objects1.getT2().getTokenReset());
                        String message = "OTP Sent! Check your email address!";

                        Response response =
                            new Response(
                                HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);

                        return Mono.just(ResponseEntity.ok(response));
                      });
            })
        .doOnSuccess(r -> LOG.info("📧  OTP e-mail inviato a {}", user.getEmail()));
  }

  /** Verifica */
  @Override
  public void verifyCode(List<MFAMethod> methods, String otp, String identifier) {

    boolean ok =
        methods.stream()
            .filter(m -> m.getType() == MFAType.EMAIL)
            .anyMatch(
                m -> {
                  boolean notExpired =
                      m.getExpiry() != null && m.getExpiry() > System.currentTimeMillis();
                  return notExpired && OneTimeCodeUtils.matches(otp, m.getHashedOTP());
                });

    if (!ok) {
      LOG.error("❌ OTP e-mail non valida per {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, ExceptionType.INVALID_OTP_CODE, "Invalid OTP code");
    }
    LOG.info("✅ OTP e-mail verificato per {}", identifier);
  }

  /**
   * @param user
   * @param setupRequest
   * @return
   */
  @Override
  public Mono<ResponseEntity<Response>> mfaChallenge(User user, MFASetupRequest setupRequest) {
    LOG.info(
        "🔑 MFA Challenge Process started for type: {} and identifier for {}",
        setupRequest.type(),
        setupRequest.identifier());
    return generateSecret(user, setupRequest);
  }
}
