package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.TOTPLabel;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Mapper e trasformatore di modelli per la gestione dei profili e metodi Multi-Factor Authentication.
 */
@Component
@Logged
public class MFAMapper {

  private static final Logger LOG = LoggerFilter.getLogger(MFAMapper.class);

  /**
   * Genera o aggiorna una configurazione MFA temporanea (non confermata) nell'anagrafica utente.
   * Preserva eventuali metodi preesistenti già confermati e rimuove setup pendenti obsoleti.
   *
   * @param user Utente per cui configurare il metodo.
   * @param setupRequest Payload della richiesta con tipo e label.
   * @param secret Chiave segreta (Base32 o codice:scadenza).
   * @return {@link MFASetting} aggiornato con il nuovo metodo non ancora confermato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASetting generateTempMFA(User user, MFASetupRequest setupRequest, String secret) {
    MFASetting current = user.getMfaSettings();
    TOTPLabel effectiveLabel = setupRequest.resolvedLabel();

    MFAMethod newMethod =
        new MFAMethod(
            setupRequest.type(),
            effectiveLabel,
            secret,
            false,
            LocalDateTime.now(),
            LocalDateTime.now());

    if (!ObjectToolkit.isNullOrEmpty(current)) {
      List<MFAMethod> methods =
          current.getMfaMethods() != null ? new ArrayList<>(current.getMfaMethods()) : new ArrayList<>();

      // Rimuove eventuali configurazioni non confermate dello stesso tipo o label
      methods.removeIf(
          m -> !m.isConfirmed() && m.getType() == setupRequest.type() && m.getLabel() == effectiveLabel);

      // Verifica se il metodo è già confermato ed attivo
      boolean alreadyConfigured =
          methods.stream()
              .anyMatch(
                  m -> m.isConfirmed() && m.getType() == setupRequest.type() && m.getLabel() == effectiveLabel);

      if (alreadyConfigured) {
        LOG.error("❌ MFA method {} ({}) is already configured and active for this user", setupRequest.type(), effectiveLabel);
        throw new MFAException(
            ExceptionMap.ERR_MFA_400,
            ExceptionType.OTP_ALREADY_CONFIGURED,
            "MFA method %s with label %s is already configured and active.".formatted(setupRequest.type(), effectiveLabel));
      }

      methods.add(newMethod);
      current.setMfaMethods(methods);
      return current;
    }

    return new MFASetting(false, new ArrayList<>(List.of(newMethod)));
  }

  /**
   * Ricerca nell'anagrafica utente il metodo MFA corrispondente alla richiesta di conferma.
   *
   * @param mfaSetting Impostazioni MFA dell'utente.
   * @param confirmationRequest Richiesta di conferma contenente tipo, label e codice OTP.
   * @return {@link MFAMethod} corrispondente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFAMethod getMFAMethod(
      MFASetting mfaSetting, MFAConfirmationRequest confirmationRequest) {
    if (mfaSetting == null || ObjectToolkit.isNullOrEmpty(mfaSetting.getMfaMethods())) {
      LOG.error("❌ No MFA methods configured for user");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "No MFA methods configured");
    }

    TOTPLabel targetLabel = confirmationRequest.resolvedLabel();

    return mfaSetting.getMfaMethods().stream()
        .filter(
            method ->
                method.getType() == confirmationRequest.type()
                    && (targetLabel == null || method.getLabel() == null || method.getLabel() == targetLabel))
        .findFirst()
        .orElseThrow(
            () ->
                new MFAException(
                    ExceptionMap.ERR_MFA_400,
                    "No matching %s MFA method found for confirmation".formatted(confirmationRequest.type())));
  }

  /**
   * Conclude l'attivazione del metodo impostandolo come confermato e abilitando lo stato globale MFA.
   *
   * @param user Utente da abilitare.
   * @param mfaMethod Metodo verificato con successo.
   * @return {@link MFASetting} con il metodo confermato e 'enabled = true'.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASetting generateFinalMFA(User user, MFAMethod mfaMethod) {
    MFASetting current = user.getMfaSettings();
    if (current == null) {
      current = new MFASetting(true, new ArrayList<>(List.of(mfaMethod)));
    }
    current.setEnabled(true);

    if (current.getMfaMethods() != null) {
      current.getMfaMethods().stream()
          .filter(
              method ->
                  method.getType() == mfaMethod.getType()
                      && (method.getLabel() == mfaMethod.getLabel()
                          || (mfaMethod.getSecretKey() != null
                              && mfaMethod.getSecretKey().equals(method.getSecretKey()))))
          .findFirst()
          .ifPresent(
              method -> {
                method.setConfirmed(true);
                method.setUpdateDate(LocalDateTime.now());
                mfaMethod.setConfirmed(true);
                mfaMethod.setUpdateDate(LocalDateTime.now());
              });
    }

    return current;
  }
}
