package io.github.giovannilamarmora.accesssphere.user;

import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;

public class UserUtils {

  /**
   * Verifica se l'utente target appartiene ad almeno uno dei Client OAuth2 per cui l'operatore
   * possiede ruoli con il permesso richiesto.
   *
   * @param targetUser Utente target recuperato dal database con i rispettivi ruoli assegnati.
   * @param authorizedRoles Ruoli dell'operatore che concedono il permesso richiesto.
   * @return true se targetUser possiede almeno un ruolo associato a uno dei client autorizzati
   *     dell'operatore, false altrimenti.
   */
  public static boolean hasMatchingClient(User targetUser, List<RoleDefinition> authorizedRoles) {
    if (ObjectToolkit.isNullOrEmpty(targetUser)
        || ObjectToolkit.isNullOrEmpty(targetUser.getRoles())
        || ObjectToolkit.isNullOrEmpty(authorizedRoles)) {
      return false;
    }

    return targetUser.getRoles().stream()
        .anyMatch(
            userRole ->
                authorizedRoles.stream()
                    .anyMatch(
                        correctRole ->
                            !ObjectToolkit.isNullOrEmpty(userRole.getClientId())
                                && !ObjectToolkit.isNullOrEmpty(correctRole.getClientId())
                                && correctRole
                                    .getClientId()
                                    .trim()
                                    .equalsIgnoreCase(userRole.getClientId().trim())));
  }
}
