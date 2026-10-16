package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.RoleUtils;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionResponse;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper per la conversione bidirezionale tra l'entita di persistenza {@link RoleDefinitionEntity}
 * (tabella ROLE_DEFINITIONS), il modello di dominio {@link RoleDefinition} e i DTO REST {@link
 * RoleDefinitionRequest} / {@link RoleDefinitionResponse}.
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

  public static RoleDefinition toModel(RoleDefinitionRequest roleDefinitionRequest) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionRequest)) return null;
    return toModel(roleDefinitionRequest, roleDefinitionRequest.getClientId());
  }

  public static RoleDefinition toModel(
      RoleDefinitionRequest roleDefinitionRequest, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionRequest)) return null;
    RoleDefinition roleDefinition = new RoleDefinition();
    BeanUtils.copyProperties(roleDefinitionRequest, roleDefinition);
    String effectiveClientId =
        !ObjectToolkit.isNullOrEmpty(clientId) ? clientId : roleDefinitionRequest.getClientId();
    roleDefinition.setClientId(RoleUtils.normalizeUpperCase(effectiveClientId));
    roleDefinition.setRole(RoleUtils.normalizeUpperCase(roleDefinitionRequest.getRole()));
    roleDefinition.setType(RoleUtils.normalizeRoleType(roleDefinitionRequest.getType()));
    return roleDefinition;
  }

  public static List<RoleDefinition> entitiesToModelList(
      List<RoleDefinitionEntity> roleDefinitionEntities) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionEntities)) return List.of();
    return roleDefinitionEntities.stream().map(RoleDefinitionMapper::toModel).toList();
  }

  public static RoleDefinition toModel(RoleDefinitionEntity roleDefinitionEntity) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitionEntity)) return null;
    RoleDefinition roleDefinition = new RoleDefinition();
    BeanUtils.copyProperties(roleDefinitionEntity, roleDefinition);
    return roleDefinition;
  }

  public static RoleDefinitionEntity toEntity(RoleDefinition roleDefinition) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return null;
    RoleDefinitionEntity roleDefinitionEntity = new RoleDefinitionEntity();
    BeanUtils.copyProperties(roleDefinition, roleDefinitionEntity);
    return roleDefinitionEntity;
  }

  public static List<UserSummaryDTO> toUserSummaryList(List<User> users) {
    if (ObjectToolkit.isNullOrEmpty(users)) return List.of();
    return users.stream().map(RoleDefinitionMapper::toUserSummary).toList();
  }

  public static UserSummaryDTO toUserSummary(User user) {
    if (ObjectToolkit.isNullOrEmpty(user)) return null;
    return new UserSummaryDTO(
        user.getIdentifier(),
        user.getUsername(),
        user.getName(),
        user.getSurname(),
        user.getEmail());
  }
}
