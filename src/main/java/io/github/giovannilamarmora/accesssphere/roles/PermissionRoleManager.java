package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserConfig;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.database.UserClientRoleRepository;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
public class PermissionRoleManager extends AdminUserConfig {

  private static final Logger LOG = LoggerFilter.getLogger(PermissionRoleManager.class);

  private static final String PERMISSION_ROLE_LOG = "(Security)";

  @Autowired private UserClientRoleRepository userClientRoleRepository;
  @Autowired private RoleDefinitionRepository roleDefinitionRepository;

  /**
   * Verifica le autorizzazioni dell'utente corrente e restituisce l'elenco delle definizioni di
   * ruolo ({@link RoleDefinition}) associate che concedono il permesso specificato o il permesso
   * globale {@link SystemPermission#ALL}.
   *
   * <p>La logica di autorizzazione segue questo flusso:
   *
   * <ol>
   *   <li>Se il client chiamante corrisponde al client amministrativo ({@code adminClientId}) e
   *       l'utente dispone del permesso richiesto o del permesso {@code ALL}, viene restituita
   *       immediatamente un'istanza sintetica di {@link RoleDefinition} associata al client di
   *       amministrazione.
   *   <li>Per i client standard, vengono recuperate le assegnazioni dell'utente dal database
   *       ({@link UserClientRole}) e confrontate con l'intero catalogo di ruoli ({@link
   *       RoleDefinition}) tramite una chiave composta {@code clientId::role}.
   *   <li>Vengono filtrati esclusivamente i ruoli che includono il permesso {@code
   *       permissionToCheck} oppure {@code SystemPermission.ALL}.
   * </ol>
   *
   * @param permissionToCheck il permesso di sistema ({@link SystemPermission}) da validare.
   * @return una lista non vuota di {@link RoleDefinition} assegnate all'utente che contengono il
   *     permesso richiesto.
   * @throws OAuthException con codice {@code ERR_OAUTH_403} se:
   *     <ul>
   *       <li>L'utente non ha ruoli assegnati a sistema;
   *       <li>Non sono presenti definizioni di ruolo censite;
   *       <li>Nessuno dei ruoli assegnati all'utente contiene il permesso richiesto o il permesso
   *           globale {@code ALL}.
   *     </ul>
   */
  public List<RoleDefinition> checkPermissionAndReturnUserRoles(
      SystemPermission permissionToCheck) {
    LOG.info(
        "🔐 Check Permission for user: {} and client: {}",
        oAuth2Authorization.getIdentifier(),
        oAuth2Authorization.getClientId());

    SystemPermission allPermission = SystemPermission.ALL;

    /**
     * Se l'utente ha il clientId di Admin e i permessi per accedere, torno direttamente il ruolo
     * senza scorrere il database.
     */
    if (hasAdminPermission(permissionToCheck)) {
      return List.of(
          new RoleDefinition(
              oAuth2Authorization.getClientId(), oAuth2Authorization.getPermissions()));
    }

    /** Recupero i ruoli associati all'utente su DB */
    String identifier = oAuth2Authorization.getIdentifier();
    Optional<List<UserClientRole>> optionalUserClientRoles =
        userClientRoleRepository.findByUserIdentifier(identifier);

    if (optionalUserClientRoles.isEmpty() || optionalUserClientRoles.get().isEmpty()) {
      LOG.error(
          "⛔ {} Access denied: missing permission [{}] on client [{}] for identifier [{}]",
          PERMISSION_ROLE_LOG,
          permissionToCheck,
          oAuth2Authorization.getClientId(),
          identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    List<UserClientRole> userClientRoles = optionalUserClientRoles.get();

    Optional<List<RoleDefinition>> optionalRoleDefinitions = roleDefinitionRepository.findAll();

    if (optionalRoleDefinitions.isEmpty() || optionalRoleDefinitions.get().isEmpty()) {
      LOG.error(
          "⛔ {} Access denied: missing permission [{}] on client [{}] for identifier [{}]",
          PERMISSION_ROLE_LOG,
          permissionToCheck,
          oAuth2Authorization.getClientId(),
          identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    List<RoleDefinition> allRoleDefinitions = optionalRoleDefinitions.get();

    // 1. Set delle chiavi univoche (clientId::role) assegnate all'utente
    Set<String> userAssignedKeys =
        userClientRoles.stream()
            .filter(ucr -> ucr.getClientId() != null && ucr.getRole() != null)
            .map(
                ucr ->
                    ucr.getClientId().trim().toUpperCase()
                        + "::"
                        + ucr.getRole().trim().toUpperCase())
            .collect(Collectors.toSet());

    // 2. Filtra i RoleDefinition assegnati all'utente CHE HANNO permissionToCheck o ALL
    List<RoleDefinition> roles =
        allRoleDefinitions.stream()
            // Appartiene all'utente?
            .filter(
                roleDefinition ->
                    roleDefinition.getClientId() != null
                        && roleDefinition.getRole() != null
                        && userAssignedKeys.contains(
                            roleDefinition.getClientId().trim().toUpperCase()
                                + "::"
                                + roleDefinition.getRole().trim().toUpperCase()))
            // Ha il permesso richiesto o ALL?
            .filter(
                roleDefinition ->
                    !ObjectToolkit.isNullOrEmpty(roleDefinition.getPermissions())
                        && (roleDefinition.getPermissions().contains(allPermission.getValue())
                            || roleDefinition
                                .getPermissions()
                                .contains(permissionToCheck.getValue())))
            .toList();

    // 3. Se nessun ruolo soddisfa i permessi, nega l'accesso
    if (roles.isEmpty()) {
      LOG.error(
          "⛔ {} User [{}] denied access: No roles found with permission [{}]",
          PERMISSION_ROLE_LOG,
          identifier,
          permissionToCheck.getValue());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    return roles;
  }

  public boolean hasAdminPermission(SystemPermission permissionToCheck) {
    LOG.info(
        "🔐 Check Full Permission for user: {} and client: {}",
        oAuth2Authorization.getIdentifier(),
        oAuth2Authorization.getClientId());

    SystemPermission allPermission = SystemPermission.ALL;
    List<String> permissions = oAuth2Authorization.getPermissions();

    boolean hasAdminOrRequiredPermission =
        !ObjectToolkit.isNullOrEmpty(permissions)
            && (permissions.contains(allPermission.getValue())
                || permissions.contains(permissionToCheck.getValue()));

    return adminClientId.equalsIgnoreCase(oAuth2Authorization.getClientId())
        && hasAdminOrRequiredPermission;
  }
}
