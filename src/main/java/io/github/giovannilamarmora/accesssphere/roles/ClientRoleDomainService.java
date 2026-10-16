package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.client.database.OAuth2ClientRepository;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servizio di dominio dedicato al catalogo e alle definizioni applicative dei ruoli
 * contestualizzati per Client OAuth2 ({@link RoleDefinition}).
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Incapsula l'accesso a {@link RoleDefinitionRepository} (tabella {@code ROLE_DEFINITIONS}).
 *   <li>Consumato da {@link UserRoleDomainService} per validare l'esistenza dei ruoli da associare
 *       agli utenti e per arricchirli.
 *   <li>Consumato da {@link RolesService} per il CRUD atomico del catalogo ruoli ({@code
 *       /v1/roles}).
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class ClientRoleDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String CLIENT_ROLE_DOMAIN = "Client Role Domain";

  @Autowired private RoleDefinitionRepository roleDefinitionRepository;
  @Autowired private OAuth2ClientRepository oAuth2ClientRepository;

  /**
   * Arricchisce una lista di {@link OAuth2Client} associando a ciascun client le relative
   * definizioni di ruolo ({@link RoleDefinition}) censite a database.
   *
   * @param clients Lista dei client da arricchire.
   * @return Lista di client con {@code appRoles} popolato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<OAuth2Client> enrichClientsWithRoles(List<OAuth2Client> clients) {
    if (ObjectToolkit.isNullOrEmpty(clients)) return List.of();
    List<RoleDefinition> allRoles = getAllRoleDefinitions();
    Map<String, List<RoleDefinition>> rolesByClient =
        allRoles.stream()
            .filter(roleDefinition -> !ObjectToolkit.isNullOrEmpty(roleDefinition.getClientId()))
            .collect(
                Collectors.groupingBy(
                    roleDefinition -> roleDefinition.getClientId().toUpperCase()));
    clients.forEach(
        oAuth2Client ->
            oAuth2Client.setAppRoles(
                rolesByClient.getOrDefault(oAuth2Client.getClientId().toUpperCase(), List.of())));
    return clients;
  }

  /**
   * Recupera l'intero catalogo globale delle definizioni di ruoli censiti a sistema.
   *
   * @return Lista di tutte le {@link RoleDefinition}, o lista vuota se nessuna definizione e
   *     presente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<RoleDefinition> getAllRoleDefinitions() {
    LOG.debug("🔍 {} Fetching all role definitions", CLIENT_ROLE_DOMAIN);
    return roleDefinitionRepository.findAll().orElse(List.of());
  }

  /**
   * Recupera il catalogo delle definizioni di ruoli filtrando opzionalmente per Client ID.
   *
   * @param clientId Filtro opzionale per Client OAuth2.
   * @return Lista di {@link RoleDefinition} corrispondenti al filtro.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<RoleDefinition> getRolesByClientFilter(String clientId) {
    if (!ObjectToolkit.isNullOrEmpty(clientId)) {
      return getRoleByClientId(clientId).orElse(List.of());
    }
    return getAllRoleDefinitions();
  }

  /**
   * Recupera la definizione puntuale di un ruolo associato a un determinato Client OAuth2.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   * @param role Nome del ruolo (es. ROLE_ADMIN).
   * @return {@link Optional} contenente il {@link RoleDefinition} se presente, altrimenti {@link
   *     Optional#empty()}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Optional<RoleDefinition> getRoleDefinition(String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || ObjectToolkit.isNullOrEmpty(role)) {
      return Optional.empty();
    }
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);
    LOG.debug(
        "🔍 {} Fetching role definition for client: {} and role: {}",
        CLIENT_ROLE_DOMAIN,
        normClientId,
        normRole);
    return roleDefinitionRepository.findByClientIdAndRole(normClientId, normRole);
  }

  /**
   * Recupera tutte le definizioni di ruoli configurate per uno specifico Client OAuth2.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   * @return {@link Optional} contenente la lista dei ruoli configurati per il client specificato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Optional<List<RoleDefinition>> getRoleByClientId(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId)) {
      return Optional.empty();
    }
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    LOG.debug("🔍 {} Fetching role definitions for client: {}", CLIENT_ROLE_DOMAIN, normClientId);
    return roleDefinitionRepository.findByClientId(normClientId);
  }

  /**
   * Verifica se un determinato ruolo esiste ed e censito a catalogo per uno specifico Client
   * OAuth2.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   * @param role Nome del ruolo applicativo.
   * @return true se il ruolo esiste, false altrimenti.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public boolean existsByClientIdAndRole(String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || ObjectToolkit.isNullOrEmpty(role)) {
      return false;
    }
    return roleDefinitionRepository.existsByClientIdAndRole(
        RoleUtils.normalizeUpperCase(clientId), RoleUtils.normalizeUpperCase(role));
  }

  /**
   * Persiste o aggiorna le definizioni dei ruoli per uno specifico Client OAuth2. Se un ruolo
   * esiste gia a catalogo per quel client, non viene duplicato.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   * @param appRoles Lista di definizioni ruoli da associare/salvare.
   * @return Lista di definizioni ruoli salvate o esistenti.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<RoleDefinition> saveClientRoles(String clientId, List<RoleDefinition> appRoles) {
    if (ObjectToolkit.isNullOrEmpty(appRoles) || ObjectToolkit.isNullOrEmpty(clientId)) {
      return List.of();
    }
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    LOG.debug(
        "💾 {} Saving/updating {} roles for client: {}",
        CLIENT_ROLE_DOMAIN,
        appRoles.size(),
        normClientId);
    return appRoles.stream()
        .map(
            roleDefinition -> {
              String roleName = RoleUtils.normalizeUpperCase(roleDefinition.getRole());
              Boolean roleExists =
                  roleDefinitionRepository.existsByClientIdAndRole(normClientId, roleName);
              if (roleExists) {
                LOG.warn(
                    "❌ {} The role {} for client ID: {} already exists",
                    CLIENT_ROLE_DOMAIN,
                    roleName,
                    normClientId);
                return roleDefinition;
              }
              roleDefinition.setClientId(normClientId);
              roleDefinition.setRole(roleName);
              roleDefinition.setType(RoleUtils.normalizeRoleType(roleDefinition.getType()));
              return roleDefinitionRepository.save(roleDefinition);
            })
        .toList();
  }

  /**
   * Crea una nuova definizione di ruolo applicativo nel catalogo del Client OAuth2 specificato.
   *
   * @param roleDefinition Modello di dominio con i parametri del nuovo ruolo.
   * @return La {@link RoleDefinition} salvata.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public RoleDefinition createRoleDefinition(RoleDefinition roleDefinition) {
    String clientId = RoleUtils.normalizeUpperCase(roleDefinition.getClientId());
    String roleName = RoleUtils.normalizeUpperCase(roleDefinition.getRole());

    verifyClientExists(clientId);

    if (roleDefinitionRepository.existsByClientIdAndRole(clientId, roleName)) {
      LOG.error("❌ {} Role [{}] already exists for client [{}]", CLIENT_ROLE_DOMAIN, roleName, clientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          "Role " + roleName + " already exists for client " + clientId);
    }

    roleDefinition.setClientId(clientId);
    roleDefinition.setRole(roleName);
    roleDefinition.setType(RoleUtils.normalizeRoleType(roleDefinition.getType()));
    if (roleDefinition.getPermissions() == null) {
      roleDefinition.setPermissions(List.of());
    }

    RoleDefinition saved = roleDefinitionRepository.save(roleDefinition);
    saved.setAssignedUsersCount(0);
    saved.setAssignedUsers(List.of());
    LOG.info("✨ {} Created role [{}] for client [{}]", CLIENT_ROLE_DOMAIN, roleName, clientId);
    return saved;
  }

  /**
   * Aggiorna una definizione di ruolo esistente nel catalogo di un Client OAuth2.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param currentRole Nome attuale del ruolo da modificare.
   * @param updatedData Nuovi attributi del ruolo.
   * @return La {@link RoleDefinition} aggiornata.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public RoleDefinition updateRoleDefinition(
      String clientId, String currentRole, RoleDefinition updatedData) {
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String currentRoleName = RoleUtils.normalizeUpperCase(currentRole);
    String newRoleName =
        !ObjectToolkit.isNullOrEmpty(updatedData.getRole())
            ? RoleUtils.normalizeUpperCase(updatedData.getRole())
            : currentRoleName;

    verifyClientExists(normClientId);

    RoleDefinition targetRole =
        roleDefinitionRepository
            .findByClientIdAndRole(normClientId, currentRoleName)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "❌ {} Role [{}] not found for client [{}]",
                      CLIENT_ROLE_DOMAIN,
                      currentRoleName,
                      normClientId);
                  return new OAuthException(
                      ExceptionMap.ERR_OAUTH_404,
                      "Role " + currentRoleName + " not found for client " + normClientId);
                });

    if (!currentRoleName.equalsIgnoreCase(newRoleName)) {
      if (roleDefinitionRepository.existsByClientIdAndRole(normClientId, newRoleName)) {
        LOG.error(
            "❌ {} Duplicate role [{}] on rename for client [{}]",
            CLIENT_ROLE_DOMAIN,
            newRoleName,
            normClientId);
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_400,
            "Role " + newRoleName + " already exists for client " + normClientId);
      }
      targetRole.setRole(newRoleName);
    }

    if (!ObjectToolkit.isNullOrEmpty(updatedData.getType())) {
      targetRole.setType(RoleUtils.normalizeRoleType(updatedData.getType()));
    }
    targetRole.setDescription(updatedData.getDescription());
    if (updatedData.getPermissions() != null) {
      targetRole.setPermissions(updatedData.getPermissions());
    }

    RoleDefinition saved = roleDefinitionRepository.save(targetRole);
    LOG.info(
        "🔄 {} Updated role [{}] -> [{}] for client [{}]",
        CLIENT_ROLE_DOMAIN,
        currentRoleName,
        newRoleName,
        normClientId);
    return saved;
  }

  /**
   * Elimina una singola definizione di ruolo dal catalogo di un Client OAuth2, garantendo che il
   * client mantenga almeno un ruolo configurato.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo da eliminare.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteRoleDefinition(String clientId, String role) {
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);

    verifyClientExists(normClientId);

    List<RoleDefinition> currentRoles =
        roleDefinitionRepository.findByClientId(normClientId).orElse(List.of());
    boolean exists =
        currentRoles.stream().anyMatch(r -> r.getRole().equalsIgnoreCase(normRole));
    if (!exists) {
      LOG.error("❌ {} Role [{}] not found for client [{}]", CLIENT_ROLE_DOMAIN, normRole, normClientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_404,
          "Role " + normRole + " not found for client " + normClientId);
    }

    if (currentRoles.size() <= 1) {
      LOG.error(
          "❌ {} Cannot delete the only remaining role [{}] for client [{}]",
          CLIENT_ROLE_DOMAIN,
          normRole,
          normClientId);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          "Cannot delete role "
              + normRole
              + ". Every OAuth2 client must have at least one role configured.");
    }

    roleDefinitionRepository.deleteByClientIdAndRole(normClientId, normRole);
    LOG.info("🗑️ {} Deleted role definition [{}] for client [{}]", CLIENT_ROLE_DOMAIN, normRole, normClientId);
  }

  /**
   * Elimina dal catalogo tutte le definizioni dei ruoli associate a uno specifico Client OAuth2.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteRoleByClientId(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId)) return;
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    LOG.info(
        "🗑️ {} Deleting all role definitions for client: {}", CLIENT_ROLE_DOMAIN, normClientId);
    roleDefinitionRepository.deleteByClientId(normClientId);
  }

  private void verifyClientExists(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || !oAuth2ClientRepository.existsByClientId(clientId)) {
      LOG.error("❌ {} Client [{}] not found", CLIENT_ROLE_DOMAIN, clientId);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, "Client not found: " + clientId);
    }
  }
}
