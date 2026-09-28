package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class UserClientRoleMapper {

  public static List<UserClientRole> toModelList(
      List<UserClientRoleEntity> userClientRoleEntities) {
    return userClientRoleEntities.stream().map(UserClientRoleMapper::toModel).toList();
  }

  public static UserClientRole toModel(UserClientRoleEntity userClientRoleEntity) {
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(userClientRoleEntity, userClientRole);
    return userClientRole;
  }

  public static UserClientRoleEntity toEntity(UserClientRole userClientRole) {
    UserClientRoleEntity userClientRoleEntity = new UserClientRoleEntity();
    BeanUtils.copyProperties(userClientRole, userClientRoleEntity);
    return userClientRoleEntity;
  }

  public static List<UserClientRole> toUserClientModels(List<RoleDefinition> roleDefinitions) {
    return roleDefinitions.stream().map(UserClientRoleMapper::toUserClientModel).toList();
  }

  public static UserClientRole toUserClientModel(RoleDefinition roleDefinition) {
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(roleDefinition, userClientRole);
    return userClientRole;
  }
}
