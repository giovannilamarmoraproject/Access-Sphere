package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionResponse;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper per la conversione bidirezionale tra l'entità di persistenza {@link RoleDefinitionEntity}
 * (tabella ROLE_DEFINITIONS) e il modello di dominio {@link RoleDefinition}.
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Utilizzato da {@code RoleDefinitionRepository} per mappare i risultati delle query DAO in
 *       modelli di dominio.
 *   <li>Utilizzato nei flussi di arricchimento ruoli e configurazione client in {@code UserService}
 *       e {@code RolesService}.
 * </ul>
 */
@Component
public class RoleDefinitionMapper {

  public static List<RoleDefinitionResponse> toResponseList(List<RoleDefinition> roleDefinitions) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) return List.of();
    return roleDefinitions.stream().map(RoleDefinitionMapper::toResponse).toList();
  }

  public static RoleDefinitionResponse toResponse(RoleDefinition roleDefinition) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return null;
    RoleDefinitionResponse roleDefinitionResponse = new RoleDefinitionResponse();
    BeanUtils.copyProperties(roleDefinition, roleDefinitionResponse);
    roleDefinitionResponse.setId(null);
    return roleDefinitionResponse;
  }

  public static List<RoleDefinition> requestsToModelList(
      List<RoleDefinitionRequest> roleDefinitionRequests, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionRequests)) return List.of();
    return roleDefinitionRequests.stream()
        .map(roleDefinitionRequest -> RoleDefinitionMapper.toModel(roleDefinitionRequest, clientId))
        .toList();
  }

  public static RoleDefinition toModel(
      RoleDefinitionRequest roleDefinitionRequest, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionRequest)) return null;
    RoleDefinition roleDefinition = new RoleDefinition();
    BeanUtils.copyProperties(roleDefinitionRequest, roleDefinition);
    roleDefinition.setClientId(clientId);
    return roleDefinition;
  }

  /**
   * Converte una lista di entità {@link RoleDefinitionEntity} nella corrispondente lista di modelli
   * di dominio {@link RoleDefinition}.
   *
   * @param roleDefinitionEntities Lista di entità recuperate dal database.
   * @return Lista di modelli di dominio, oppure una lista vuota se l'input è null.
   */
  public static List<RoleDefinition> entitiesToModelList(
      List<RoleDefinitionEntity> roleDefinitionEntities) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionEntities)) return List.of();
    return roleDefinitionEntities.stream().map(RoleDefinitionMapper::toModel).toList();
  }

  /**
   * Converte una singola entità {@link RoleDefinitionEntity} nel modello di dominio {@link
   * RoleDefinition}.
   *
   * @param roleDefinitionEntity Entità sorgente da convertire.
   * @return Il modello di dominio popolato, oppure null se l'entità sorgente è null.
   */
  public static RoleDefinition toModel(RoleDefinitionEntity roleDefinitionEntity) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionEntity)) return null;
    RoleDefinition roleDefinition = new RoleDefinition();
    BeanUtils.copyProperties(roleDefinitionEntity, roleDefinition);
    return roleDefinition;
  }

  /**
   * Converte un modello di dominio {@link RoleDefinition} nella corrispondente entità persistente
   * {@link RoleDefinitionEntity}.
   *
   * @param roleDefinition Modello di dominio sorgente.
   * @return L'entità persistente popolata per il database, oppure null se il modello sorgente è
   *     null.
   */
  public static RoleDefinitionEntity toEntity(RoleDefinition roleDefinition) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return null;
    RoleDefinitionEntity roleDefinitionEntity = new RoleDefinitionEntity();
    BeanUtils.copyProperties(roleDefinition, roleDefinitionEntity);
    return roleDefinitionEntity;
  }
}
