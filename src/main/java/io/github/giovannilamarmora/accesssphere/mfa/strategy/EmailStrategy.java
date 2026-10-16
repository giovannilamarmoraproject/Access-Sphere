package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.email.sender.EmailParameterResolver;
import io.github.giovannilamarmora.accesssphere.email.sender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.EmailTemplateDomainService;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplateCategory;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.MFAMapper;
import io.github.giovannilamarmora.accesssphere.mfa.MFAUtils;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupDataResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASetting;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Strategia di autenticazione a due fattori basata su codice OTP recapitato via Email.
 *
 * <p>Flusso operativo:
 * <ul>
 *   <li>In fase di setup: genera un codice monouso crittograficamente sicuro a 6 cifre con scadenza a 10 minuti,
 *       lo memorizza nell'anagrafica utente e lo spedisce tramite {@link EmailSenderService} utilizzando il template {@code MFA_OTP}.</li>
 *   <li>In fase di conferma o verifica login: convalida il codice immesso rispetto al segreto memorizzato verificando che non sia scaduto.</li>
 *   <li>Supporta l'invio proattivo e il reinvio on-demand del codice tramite {@link #sendCode(User)}.</li>
 * </ul>
 */
@Service
@Logged
public class EmailStrategy implements MFAStrategy {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String DEFAULT_EXPIRY_MINUTES = "10";
  private static final long EXPIRY_MILLIS = 10 * 60 * 1000L;

  @Autowired private UserService userService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private EmailTemplateDomainService emailTemplateDomainService;

  /**
   * Inizializza il setup MFA per il canale Email, generando il primo codice OTP e inviandolo all'indirizzo dell'utente.
   *
   * @param user Utente per cui configurare l'MFA via email.
   * @param setupRequest Richiesta di setup contenente identificativo e tipo.
   * @return {@link Mono} con {@link ResponseEntity} di avvenuta generazione e invio.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFASetupDataResponse> generateSecret(User user, MFASetupRequest setupRequest) {
    LOG.info("🔑 [EMAIL-MFA] Generate Email MFA for user: {} process started.", setupRequest.identifier());

    if (ObjectToolkit.isNullOrEmpty(user.getEmail())) {
      LOG.error("❌ User {} does not have a configured email address", setupRequest.identifier());
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, "User email address is missing or not configured");
    }

    String otpCode = MFAUtils.generateEmailOtpCode();
    long expiry = System.currentTimeMillis() + EXPIRY_MILLIS;
    String secret = MFAUtils.formatEmailOtpSecret(otpCode, expiry);

    MFASetting mfaSetting = MFAMapper.generateTempMFA(user, setupRequest, secret);
    user.setMfaSettings(mfaSetting);

    return userService
        .updateUser(setupRequest.identifier(), user)
        .flatMap(updatedUser -> sendOtpEmail(updatedUser, otpCode, DEFAULT_EXPIRY_MINUTES))
        .map(emailResponse -> new MFASetupDataResponse(null, null, null))
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [EMAIL-MFA] Generate Email MFA for user: {} process ended.",
                    setupRequest.identifier()));
  }

  /**
   * Convalida il codice OTP a 6 cifre inserito dall'utente confrontandolo con quello inviato per email e verificandone la validità temporale.
   *
   * @param mfaMethods Lista dei metodi configurati per l'utente.
   * @param otp Codice a 6 cifre da verificare.
   * @param identifier Identificativo utente per log ed eccezioni.
   */
  @Override
  public void verifyCode(List<MFAMethod> mfaMethods, String otp, String identifier) {
    if (ObjectToolkit.isNullOrEmpty(mfaMethods)) {
      LOG.error("❌ No MFA methods configured for user: {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, ExceptionType.OTP_NOT_CONFIGURED, "No MFA methods configured");
    }

    List<MFAMethod> emailMethods =
        mfaMethods.stream().filter(m -> m.getType() == MFAType.EMAIL).toList();

    if (emailMethods.isEmpty()) {
      LOG.error("❌ No Email MFA configured for user: {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.OTP_NOT_CONFIGURED,
          "Email MFA is not configured for user");
    }

    long now = System.currentTimeMillis();
    boolean codeMatched = false;
    boolean expired = false;

    for (MFAMethod method : emailMethods) {
      if (method.getSecretKey() != null) {
        String[] parts = method.getSecretKey().split(":");
        if (parts.length >= 2) {
          String storedCode = parts[0];
          try {
            long expiry = Long.parseLong(parts[1]);
            if (storedCode.equals(otp.trim())) {
              if (now <= expiry) {
                codeMatched = true;
                break;
              } else {
                expired = true;
              }
            }
          } catch (NumberFormatException e) {
            LOG.warn("⚠️ Invalid timestamp in email secret key: {}", parts[1]);
          }
        }
      }
    }

    if (expired) {
      LOG.error("❌ Email OTP code has expired for user: {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_OTP_CODE,
          "The OTP code has expired. Please request a new code.");
    }

    if (!codeMatched) {
      LOG.error("❌ Invalid Email OTP code for user: {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, ExceptionType.INVALID_OTP_CODE, "Invalid OTP code");
    }

    LOG.info("✅ Email OTP code verified successfully for user: {}", identifier);
  }

  /**
   * Genera e invia un nuovo codice OTP monouso all'indirizzo email dell'utente aggiornando la chiave temporanea su DB.
   *
   * @param user Utente a cui recapitare l'OTP.
   * @return {@link Mono} con esito booleano dell'invio.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Boolean> sendCode(User user) {
    if (user == null || ObjectToolkit.isNullOrEmpty(user.getEmail())) {
      LOG.error("❌ Cannot send OTP email: user or email is null");
      return Mono.just(false);
    }
    if (user.getMfaSettings() == null
        || ObjectToolkit.isNullOrEmpty(user.getMfaSettings().getMfaMethods())) {
      LOG.error("❌ No MFA methods configured for user: {}", user.getIdentifier());
      return Mono.just(false);
    }

    MFAMethod emailMethod =
        user.getMfaSettings().getMfaMethods().stream()
            .filter(m -> m.getType() == MFAType.EMAIL)
            .findFirst()
            .orElse(null);

    if (emailMethod == null) {
      LOG.error("❌ No Email MFA method found for user: {}", user.getIdentifier());
      return Mono.just(false);
    }

    String otpCode = MFAUtils.generateEmailOtpCode();
    long expiry = System.currentTimeMillis() + EXPIRY_MILLIS;
    emailMethod.setSecretKey(MFAUtils.formatEmailOtpSecret(otpCode, expiry));
    emailMethod.setUpdateDate(LocalDateTime.now());

    return userService
        .updateUser(user.getIdentifier(), user)
        .flatMap(updatedUser -> sendOtpEmail(updatedUser, otpCode, DEFAULT_EXPIRY_MINUTES))
        .map(response -> true)
        .onErrorResume(
            err -> {
              LOG.error(
                  "❌ Failed to send OTP email to user {}: {}",
                  user.getIdentifier(),
                  err.getMessage());
              return Mono.just(false);
            });
  }

  /**
   * Compila il template email dedicato per l'OTP MFA ed esegue il dispatch SMTP tramite {@link EmailSenderService}.
   *
   * @param user Utente destinatario.
   * @param otpCode Codice generato.
   * @param expiryMinutes Minuti di validità del codice.
   * @return {@link Mono} con risposta del dispatcher.
   */
  private Mono<EmailResponse> sendOtpEmail(User user, String otpCode, String expiryMinutes) {
    LOG.info("📧 Preparing to send MFA OTP email to {}", user.getEmail());

    return emailTemplateDomainService
        .getTemplateByCategoryLocaleAndClient(EmailTemplateCategory.MFA_OTP.getCode(), null, null)
        .flatMap(
            template -> {
              Map<String, String> eventParams = new LinkedHashMap<>();
              eventParams.put("AUTH.OTP_CODE", otpCode);
              eventParams.put("AUTH.EXPIRE_MINUTES", expiryMinutes);

              Map<String, String> resolvedParams = EmailParameterResolver.resolve(user, eventParams);

              EmailContent emailContent =
                  EmailContent.builder()
                      .subject(template.getSubject())
                      .to(user.getEmail())
                      .sentDate(new Date())
                      .build();

              return emailSenderService.sendEmail(template, resolvedParams, emailContent);
            });
  }
}
