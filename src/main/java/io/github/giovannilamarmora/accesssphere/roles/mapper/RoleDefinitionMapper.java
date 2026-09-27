package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class RoleDefinitionMapper {

  public static List<RoleDefinition> toModelList(
      List<RoleDefinitionEntity> roleDefinitionEntities) {
    return roleDefinitionEntities.stream().map(RoleDefinitionMapper::toModel).toList();
  }

  public static RoleDefinition toModel(RoleDefinitionEntity roleDefinitionEntity) {
    RoleDefinition roleDefinition = new RoleDefinition();
    BeanUtils.copyProperties(roleDefinitionEntity, roleDefinition);
    return roleDefinition;
  }
}
