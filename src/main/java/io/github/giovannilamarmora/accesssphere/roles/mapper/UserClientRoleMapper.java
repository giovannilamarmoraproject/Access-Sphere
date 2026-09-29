package io.github.giovannilamarmora.accesssphere.roles.mapper;

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
    BeanUtils.copyProperties(userClientRole, userClientRoleResponse);
    userClientRoleResponse.setId(null);
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

  public static UserClientRoleEntity toEntity(UserClientRole userClientRole) {
    // if (ObjectToolkit.isNullOrEmpty(userClientRole)) return null;
    UserClientRoleEntity userClientRoleEntity = new UserClientRoleEntity();
    BeanUtils.copyProperties(userClientRole, userClientRoleEntity);
    return userClientRoleEntity;
  }

  public static List<UserClientRole> toUserClientModels(List<RoleDefinition> roleDefinitions) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) return List.of();
    return roleDefinitions.stream().map(UserClientRoleMapper::toUserClientModel).toList();
  }

  public static UserClientRole toUserClientModel(RoleDefinition roleDefinition) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return null;
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(roleDefinition, userClientRole);
    return userClientRole;
  }
}
