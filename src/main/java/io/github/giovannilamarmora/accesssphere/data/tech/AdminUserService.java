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

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean isTechnicalUser(User user) {
    if (ObjectToolkit.isNullOrEmpty(user)) return false;
    return isTechnicalUser(user.getUsername(), user.getEmail());
  }

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
