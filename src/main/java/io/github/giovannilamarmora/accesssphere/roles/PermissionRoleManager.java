package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.database.IRoleDefinitionDAO;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import io.github.giovannilamarmora.accesssphere.roles.dto.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manager centralizzato per il controllo accessi basato su permessi granulari (RBAC) e perimetro
 * multi-tenant (Data Scoping).
 *
 * <p>Regole di sicurezza implementate:
 *
 * <ul>
 *   <li>Super Admin (Tech User): possiede tutti i permessi (wildcard "*") e accesso illimitato a
 *       tutti i client registrati e impostazioni di sistema.
 *   <li>Accesso alla Console UI: consentito esclusivamente agli utenti che possiedono almeno un
 *       ruolo associato al client di piattaforma {@code ACCESS-SPHERE-AUTH}.
 *   <li>Perimetro dei Client Delegati (Managed Clients): un operatore delegato puo visualizzare e
 *       amministrare unicamente i client applicativi assegnati nella tabella {@code
 *       USER_CLIENT_ROLE}.
 *   <li>Enforcement dei Permessi per-client: le azioni consentite su un client derivano dai
 *       permessi configurati nei ruoli posseduti su quello specifico client.
 * </ul>
 */
@Component
public class PermissionRoleManager {

  private static final Logger LOG = LoggerFilter.getLogger(PermissionRoleManager.class);

  @Autowired private AdminUserService techUserService;
  @Autowired private IUserClientRoleDAO userClientRoleDAO;
  @Autowired private IRoleDefinitionDAO roleDefinitionDAO;

  /**
   * Verifica se l'operatore corrente corrisponde al Super Admin / Utenza Tecnica di sistema.
   *
   * @param auth Dati del token di autorizzazione dell'operatore.
   * @return true se e' utente tecnico di sistema, false altrimenti.
   */
  public boolean isSuperAdmin(OAuth2Authorization auth) {
    return techUserService.isTechUser(auth);
  }

  /**
   * Verifica se l'operatore possiede il diritto di accedere alla Console di amministrazione di
   * Access-Sphere (ovvero possiede almeno un ruolo su {@code ACCESS-SPHERE-AUTH}).
   *
   * @param auth Dati del token di autorizzazione dell'operatore.
   * @return true se autorizzato all'uso della console, false altrimenti.
   */
  public boolean hasConsoleAccess(OAuth2Authorization auth) {
    if (isSuperAdmin(auth)) {
      return true;
    }
    if (auth == null || auth.getIdentifier() == null) {
      return false;
    }
    List<UserClientRoleEntity> consoleRoles =
        userClientRoleDAO.findByUserIdentifierAndClientId(
            auth.getIdentifier(), techUserService.getAdminClientId());
    return consoleRoles != null && !consoleRoles.isEmpty();
  }

  /**
   * Asserisce che l'operatore abbia accesso alla Console, lanciando HTTP 403 Forbidden in caso
   * contrario.
   *
   * @param auth Dati del token dell'operatore.
   * @throws OAuthException se l'accesso alla console non e' autorizzato.
   */
  public void assertHasConsoleAccess(OAuth2Authorization auth) {
    if (!hasConsoleAccess(auth)) {
      LOG.warn(
          "⛔ [SECURITY] User [{}] denied console access: No role found on technical client [{}]",
          auth != null ? auth.getSubject() : "unknown",
          techUserService.getAdminClientId());
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_403,
          "Access to Access-Sphere Console is not authorized for this user");
    }
  }

  /**
   * Restituisce la lista degli identificativi dei client di competenza assegnati all'operatore.
   *
   * @param auth Dati del token dell'operatore.
   * @return null se Super Admin (accesso illimitato a tutti i client), oppure la lista dei {@code
   *     clientId} assegnati (escluso il client tecnico).
   */
  public List<String> getManagedClientIds(OAuth2Authorization auth) {
    if (isSuperAdmin(auth)) {
      return null; // null convenzionalmente indica perimetro globale per il Super Admin
    }
    if (auth == null || auth.getIdentifier() == null) {
      return List.of();
    }
    List<UserClientRoleEntity> roles = userClientRoleDAO.findByUserIdentifier(auth.getIdentifier());
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    return roles.stream()
        .map(UserClientRoleEntity::getClientId)
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(String::toUpperCase)
        .filter(cid -> !cid.equalsIgnoreCase(techUserService.getAdminClientId()))
        .distinct()
        .toList();
  }

  /**
   * Determina se l'operatore e autorizzato ad accedere e operare sul perimetro del client indicato.
   *
   * @param auth Dati del token dell'operatore.
   * @param targetClientId Identificativo univoco del client da verificare.
   * @return true se autorizzato, false altrimenti.
   */
  public boolean canAccessClient(OAuth2Authorization auth, String targetClientId) {
    if (targetClientId == null) return false;
    if (isSuperAdmin(auth)) return true;

    // Gli operatori non-tech non possono in alcun caso accedere direttamente al client tecnico di
    // sistema
    if (targetClientId.equalsIgnoreCase(techUserService.getAdminClientId())) {
      return false;
    }

    List<String> managed = getManagedClientIds(auth);
    return managed != null && managed.contains(targetClientId.trim().toUpperCase());
  }

  /**
   * Verifica se l'operatore possiede uno specifico permesso granulare sul client indicato.
   *
   * @param auth Dati del token dell'operatore.
   * @param targetClientId Identificativo del client di riferimento.
   * @param requiredPermission Nome del permesso richiesto (es. "clients:read", "users:write").
   * @return true se l'operatore possiede il permesso o wildcard "*", false altrimenti.
   */
  public boolean hasPermission(
      OAuth2Authorization auth, String targetClientId, String requiredPermission) {
    if (isSuperAdmin(auth)) return true;
    if (!canAccessClient(auth, targetClientId)) return false;
    if (requiredPermission == null || requiredPermission.isBlank()) return true;

    List<UserClientRoleEntity> roles =
        userClientRoleDAO.findByUserIdentifierAndClientId(
            auth.getIdentifier(), targetClientId.trim().toUpperCase());
    if (roles == null || roles.isEmpty()) return false;

    for (UserClientRoleEntity r : roles) {
      if (r == null || r.getRole() == null) continue;
      Optional<RoleDefinitionEntity> def =
          roleDefinitionDAO.findByClientIdAndRole(
              targetClientId.trim().toUpperCase(), r.getRole().trim().toUpperCase());
      if (def.isPresent() && def.get().getPermissions() != null) {
        boolean match =
            def.get().getPermissions().stream()
                .anyMatch(
                    p ->
                        p.equals("*")
                            || p.equalsIgnoreCase(requiredPermission)
                            || (p.endsWith(":*")
                                && requiredPermission
                                    .toLowerCase()
                                    .startsWith(p.substring(0, p.length() - 1).toLowerCase())));
        if (match) return true;
      }
    }
    return false;
  }

  /**
   * Asserisce che l'operatore possieda il permesso richiesto sul client specificato, lanciando HTTP
   * 403 Forbidden in caso di mancanza di autorizzazione.
   *
   * @param auth Dati del token dell'operatore.
   * @param targetClientId Identificativo del client bersaglio.
   * @param requiredPermission Permesso da validare.
   * @throws OAuthException se il permesso e assente o il client e fuori perimetro.
   */
  public void assertPermission(
      OAuth2Authorization auth, String targetClientId, String requiredPermission) {
    if (!hasPermission(auth, targetClientId, requiredPermission)) {
      LOG.warn(
          "⛔ [SECURITY] User [{}] lacks required permission [{}] on client [{}]",
          auth != null ? auth.getSubject() : "unknown",
          requiredPermission,
          targetClientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_403,
          "Access denied: missing permission ["
              + requiredPermission
              + "] on client ["
              + targetClientId
              + "]");
    }
  }

  /**
   * Determina se l'operatore autenticato possiede i permessi per la creazione/amministrazione di
   * utenti IAM (Super Admin, utente tecnico o possesso del permesso 'users:write' / '*').
   *
   * @param auth Dati del token di autorizzazione dell'operatore.
   * @param targetClientId Identificativo opzionale del client bersaglio su cui si intende operare.
   * @return true se l'operatore possiede i privilegi di scrittura utenti, false altrimenti.
   */
  public boolean canWriteUsers(OAuth2Authorization auth, String targetClientId) {
    if (auth == null) return false;
    if (isSuperAdmin(auth)) return true;

    // 1. Verifica se i permessi diretti presenti nel token contengono '*' o 'users:write'
    if (auth.getPermissions() != null) {
      boolean match =
          auth.getPermissions().stream()
              .filter(Objects::nonNull)
              .anyMatch(
                  p ->
                      p.equals("*")
                          || p.equalsIgnoreCase(SystemPermission.ALL.getValue())
                          || p.equalsIgnoreCase(SystemPermission.USERS_WRITE.getValue())
                          || p.equalsIgnoreCase("users:*")
                          || p.equalsIgnoreCase("users:write"));
      if (match) return true;
    }

    // 2. Verifica se l'operatore possiede ruoli con permessi di scrittura utenti sulla Console (ACCESS-SPHERE-AUTH)
    String adminClientId = techUserService.getAdminClientId();
    if (auth.getIdentifier() != null) {
      List<UserClientRoleEntity> consoleRoles =
          userClientRoleDAO.findByUserIdentifierAndClientId(auth.getIdentifier(), adminClientId);
      if (consoleRoles != null && !consoleRoles.isEmpty()) {
        for (UserClientRoleEntity ucr : consoleRoles) {
          if (ucr == null || ucr.getRole() == null) continue;
          Optional<RoleDefinitionEntity> def =
              roleDefinitionDAO.findByClientIdAndRole(adminClientId, ucr.getRole());
          if (def.isPresent() && def.get().getPermissions() != null) {
            boolean match =
                def.get().getPermissions().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(
                        p ->
                            p.equals("*")
                                || p.equalsIgnoreCase(SystemPermission.ALL.getValue())
                                || p.equalsIgnoreCase(SystemPermission.USERS_WRITE.getValue())
                                || p.equalsIgnoreCase("users:*")
                                || p.equalsIgnoreCase("users:write"));
            if (match) return true;
          }
        }
      }

      // 3. Verifica sullo specifico client target (se specificato)
      if (targetClientId != null && !targetClientId.isBlank()) {
        List<UserClientRoleEntity> targetRoles =
            userClientRoleDAO.findByUserIdentifierAndClientId(
                auth.getIdentifier(), targetClientId.trim().toUpperCase());
        if (targetRoles != null && !targetRoles.isEmpty()) {
          for (UserClientRoleEntity ucr : targetRoles) {
            if (ucr == null || ucr.getRole() == null) continue;
            Optional<RoleDefinitionEntity> def =
                roleDefinitionDAO.findByClientIdAndRole(
                    targetClientId.trim().toUpperCase(), ucr.getRole());
            if (def.isPresent() && def.get().getPermissions() != null) {
              boolean match =
                  def.get().getPermissions().stream()
                      .filter(Objects::nonNull)
                      .anyMatch(
                          p ->
                              p.equals("*")
                                  || p.equalsIgnoreCase(SystemPermission.ALL.getValue())
                                  || p.equalsIgnoreCase(SystemPermission.USERS_WRITE.getValue())
                                  || p.equalsIgnoreCase("users:*")
                                  || p.equalsIgnoreCase("users:write"));
              if (match) return true;
            }
          }
        }
      }
    }

    return false;
  }
}
