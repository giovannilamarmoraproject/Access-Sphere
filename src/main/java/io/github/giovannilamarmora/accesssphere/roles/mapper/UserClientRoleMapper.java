package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
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
}
