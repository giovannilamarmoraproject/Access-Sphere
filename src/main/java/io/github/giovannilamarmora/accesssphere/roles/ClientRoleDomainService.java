package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.roles.database.RoleDefinitionRepository;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servizio di dominio dedicato al catalogo e alle definizioni applicative dei ruoli contestualizzati
 * per Client OAuth2 ({@link RoleDefinition}).
 *
 * <p>Interconnessioni logiche:
 * <ul>
 *   <li>Incapsula l'accesso a {@link RoleDefinitionRepository} (tabella {@code ROLE_DEFINITIONS}).</li>
 *   <li>Consumato da {@link UserRoleDomainService} per validare l'esistenza dei ruoli da associare agli utenti e per arricchirli.</li>
 *   <li>Destinato ad accogliere in futuro la logica di dominio CRUD del catalogo ruoli (attualmente gestita ad alto livello in {@code RolesService}).</li>
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class ClientRoleDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String CLIENT_ROLE_DOMAIN = "Client Role Domain";

  @Autowired private RoleDefinitionRepository roleDefinitionRepository;

  /**
   * Recupera l'intero catalogo globale delle definizioni di ruoli censiti a sistema.
   *
   * @return Lista di tutte le {@link RoleDefinition}, o lista vuota se nessuna definizione è presente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<RoleDefinition> getAllRoleDefinitions() {
    LOG.debug("🔍 {} Fetching all role definitions", CLIENT_ROLE_DOMAIN);
    return roleDefinitionRepository.findAll().orElse(List.of());
  }

  /**
   * Recupera la definizione puntuale di un ruolo associato a un determinato Client OAuth2.
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   * @param role Nome del ruolo (es. ROLE_ADMIN).
   * @return {@link Optional} contenente il {@link RoleDefinition} se presente, altrimenti {@link Optional#empty()}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Optional<RoleDefinition> getRoleDefinition(String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || ObjectToolkit.isNullOrEmpty(role)) {
      return Optional.empty();
    }
    String normClientId = clientId.trim().toUpperCase();
    String normRole = role.trim().toUpperCase();
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
  public Optional<List<RoleDefinition>> getRoleDefinitionsByClientId(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId)) {
      return Optional.empty();
    }
    String normClientId = clientId.trim().toUpperCase();
    LOG.debug("🔍 {} Fetching role definitions for client: {}", CLIENT_ROLE_DOMAIN, normClientId);
    return roleDefinitionRepository.findByClientId(normClientId);
  }

  /**
   * Verifica se un determinato ruolo esiste ed è censito a catalogo per uno specifico Client OAuth2.
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
        clientId.trim().toUpperCase(), role.trim().toUpperCase());
  }
}
