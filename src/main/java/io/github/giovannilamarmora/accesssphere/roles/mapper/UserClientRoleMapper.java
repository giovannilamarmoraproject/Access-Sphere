package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleResponse;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class UserClientRoleMapper {

  public static List<UserClientRoleResponse> toResponseList(List<UserClientRole> userClientRoles) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoles)) return List.of();
    return userClientRoles.stream().map(UserClientRoleMapper::toResponse).toList();
  }

  public static UserClientRoleResponse toResponse(UserClientRole userClientRole) {
    if (ObjectToolkit.isNullOrEmpty(userClientRole)) return null;
    UserClientRoleResponse userClientRoleResponse = new UserClientRoleResponse();
    BeanUtils.copyProperties(userClientRole, userClientRoleResponse, "id");
    return userClientRoleResponse;
  }

  public static List<UserClientRole> toModelList(
      List<UserClientRoleEntity> userClientRoleEntities) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoleEntities)) return List.of();
    return userClientRoleEntities.stream().map(UserClientRoleMapper::toModel).toList();
  }

  public static UserClientRole toModel(UserClientRoleEntity userClientRoleEntity) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoleEntity)) return null;
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(userClientRoleEntity, userClientRole);
    return userClientRole;
  }

  public static List<UserClientRole> requestToModelList(
      List<UserClientRoleRequest> userClientRoleRequests) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoleRequests)) return List.of();
    return userClientRoleRequests.stream().map(UserClientRoleMapper::toModel).toList();
  }

  public static UserClientRole toModel(UserClientRoleRequest userClientRoleRequest) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoleRequest)) return null;
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(userClientRoleRequest, userClientRole);
    return userClientRole;
  }

  public static List<UserClientRoleEntity> toEntityList(
      List<UserClientRole> userClientRoles, String identifier) {
    return userClientRoles.stream()
        .map(userClientRole -> toEntity(userClientRole, identifier))
        .toList();
  }

  public static UserClientRoleEntity toEntity(UserClientRole userClientRole, String identifier) {
    UserClientRoleEntity userClientRoleEntity = new UserClientRoleEntity();
    BeanUtils.copyProperties(userClientRole, userClientRoleEntity);
    userClientRoleEntity.setUserIdentifier(identifier);
    return userClientRoleEntity;
  }

  public static List<UserClientRole> toUserClientModels(
      List<RoleDefinition> roleDefinitions, String identifier) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) return List.of();
    return roleDefinitions.stream()
        .map(roleDefinition -> UserClientRoleMapper.toUserClientModel(roleDefinition, identifier))
        .toList();
  }

  public static UserClientRole toUserClientModel(RoleDefinition roleDefinition, String identifier) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return null;
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(roleDefinition, userClientRole, "id", "creationDate", "updateDate");
    userClientRole.setUserIdentifier(identifier);
    return userClientRole;
  }
}
