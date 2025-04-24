package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.dto.*;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Logged
public class MFAMapper {

  private static final Logger LOG = LoggerFilter.getLogger(MFAMapper.class);

  /**
   * Metodo per generare MFA Temporaneo. Se ci sono altri MFA presenti nell'utente, non si altera ma
   * si aggiunge la nova configurazione
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASetting generateTempMFA(User user, MFASetupRequest setupRequest, String secret) {
    MFASetting current = user.getMfaSettings();
    MFAMethod method =
        new MFAMethod(
            setupRequest.type(),
            ObjectToolkit.getOrDefault(setupRequest.label(), null),
            secret,
            false,
            LocalDateTime.now(),
            LocalDateTime.now());
    if (!ObjectToolkit.isNullOrEmpty(current)) {
      if (!ObjectToolkit.isNullOrEmpty(current.getMfaMethods())) {
        // Predicate<MFAMethod> removeUnverified =
        //    type ->
        //        !type.isConfirmed()
        //            && Duration.between(
        //
        // type.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
        //                        Instant.now())
        //                    .toHours()
        //                >= 24;

        Predicate<MFAMethod> removeUnverified = mfaMethod -> !mfaMethod.isConfirmed();
        List<MFAMethod> mfaMethods = new ArrayList<>(current.getMfaMethods());
        mfaMethods.removeIf(removeUnverified);

        if (mfaMethods.stream()
            .anyMatch(mfaMethod -> mfaMethod.getLabel().equals(setupRequest.label()))) {
          LOG.error("TOTP already configured for this provider.");
          throw new MFAException(
              ExceptionMap.ERR_MFA_400,
              ExceptionType.OTP_ALREADY_CONFIGURED,
              "TOTP already configured for this provider.");
        }

        current.setMfaMethods(mfaMethods);

        current.getMfaMethods().add(method);
      } else current.setMfaMethods(List.of(method));
      return current;
    }
    return new MFASetting(false, List.of(method));
  }

  /** Metodo per cercare l'MFA che l'utente ha precedentemente aggiungto */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFAMethod getMFAMethod(
      MFASetting mfaSetting, MFAConfirmationRequest confirmationRequest) {
    return mfaSetting.getMfaMethods().stream()
        .filter(
            method ->
                method.getType() == confirmationRequest.type()
                    && method.getLabel()
                        == confirmationRequest.label()) // Usa il tipo di MFA dinamico
        .findFirst()
        .orElseThrow(
            () -> new MFAException(ExceptionMap.ERR_MFA_400, "No matching MFA method found"));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASetting generateFinalMFA(User user, MFAMethod mfaMethod) {

    MFASetting current = user.getMfaSettings();
    current.setEnabled(true);

    current.getMfaMethods().stream()
        .filter(method -> method.getSecretKey().equals(mfaMethod.getSecretKey()))
        .findFirst()
        .ifPresent(
            method -> {
              mfaMethod.setConfirmed(true);
              mfaMethod.setUpdateDate(LocalDateTime.now());
            });

    return current;
  }
}
