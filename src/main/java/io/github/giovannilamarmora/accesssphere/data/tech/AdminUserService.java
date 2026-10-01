package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.token.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService extends AdminUserConfig {

  /**
   * Verifica se l'utente specificato corrisponde all'utenza tecnica di sistema censita.
   *
   * @param user DTO dell'utente da verificare.
   * @return true se l'utente corrisponde all'utenza tecnica (per username o email), false
   *     altrimenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechnicalUser(User user) {
    if (ObjectToolkit.isNullOrEmpty(user)) return false;
    return isTechnicalUser(user.getUsername(), user.getEmail());
  }

  /**
   * Verifica se la combinazione username ed email corrisponde all'utenza tecnica di sistema.
   *
   * @param username Username da verificare.
   * @param email Indirizzo email da verificare.
   * @return true se coincide con l'utenza tecnica configurata, false altrimenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechnicalUser(String username, String email) {
    boolean matchUsername =
        org.springframework.util.StringUtils.hasText(adminUsername)
            && adminUsername.equalsIgnoreCase(username);
    boolean matchEmail =
        org.springframework.util.StringUtils.hasText(adminEmail)
            && org.springframework.util.StringUtils.hasText(email)
            && adminEmail.equalsIgnoreCase(email);
    return matchUsername || matchEmail;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechUser() {
    return isTechUser(oAuth2Authorization);
  }

  /**
   * Determina se i dati del token di accesso appartengono all'operatore tecnico IAM di sistema.
   *
   * <p>Interconnessioni logiche:
   *
   * <ul>
   *   <li>Riconosce il subject type esplicito {@link SubjectType#TECHNICAL}.
   *   <li>Verifica tramite {@link #isTechnicalUser(String, String)} se username o email
   *       corrispondono all'utenza tecnica configurata in {@link AdminUserConfig}.
   *   <li>Verifica la presenza del ruolo di sistema infrastrutturale {@code ROLE_TECH}.
   *   <li>Per chiamate puramente M2M / client-credentials (senza anagrafica utente associata),
   *       riconosce il client tecnico {@link #getAdminClientId()}.
   * </ul>
   *
   * @param tokenData Dati del token di accesso da valutare.
   * @return true se l'operatore e' l'utenza tecnica di sistema, false per utenti ordinari/delegati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechUser(OAuth2Authorization tokenData) {
    if (ObjectToolkit.isNullOrEmpty(tokenData) || ObjectToolkit.fieldsNullOrEmpty(tokenData))
      return false;

    // 1. SubjectType esplicitamente impostato come TECHNICAL
    if (SubjectType.TECHNICAL.equals(tokenData.getSubjectType())) {
      return true;
    }

    // 2. Corrispondenza anagrafica con l'utenza tecnica configurata (username o email)
    if (isTechnicalUser(tokenData.getSubject(), tokenData.getEmail())) {
      return true;
    }

    // 3. Possesso del ruolo infrastrutturale di sistema ROLE_TECH
    if (tokenData.getRoles() != null && tokenData.getRoles().contains("ROLE_TECH")) {
      return true;
    }

    // 4. Token puramente client-credentials M2M (senza anagrafica utente) per il client tecnico
    if (ObjectToolkit.isNullOrEmpty(tokenData.getSubject())
        && ObjectToolkit.isNullOrEmpty(tokenData.getEmail())
        && org.springframework.util.StringUtils.hasText(tokenData.getClientId())
        && tokenData.getClientId().equalsIgnoreCase(adminClientId)) {
      return true;
    }

    return false;
  }
}
