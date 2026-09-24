package io.github.giovannilamarmora.accesssphere.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.RoleDefinitionEntity;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupDataDTO {
  private String version;
  private String timestamp;
  private String exportedBy;
  private AppSettingsEntity settings;
  private List<OAuth2ClientEntity> clients;
  private List<UserEntity> users;
  private List<RoleDefinitionEntity> roles;
  private List<UserClientRoleEntity> userClientRoles;
}
