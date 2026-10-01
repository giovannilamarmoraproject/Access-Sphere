package io.github.giovannilamarmora.accesssphere.roles.mapper;

import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleResponse;
import io.github.giovannilamarmora.accesssphere.roles.entity.UserClientRoleEntity;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    UserClientRole userClientRole = new UserClientRole();
    BeanUtils.copyProperties(roleDefinition, userClientRole, "id", "creationDate", "updateDate");
    userClientRole.setUserIdentifier(identifier);
    return userClientRole;
  }

  /**
   * Unisce tutti i campi presenti tra i ruoli persistiti a database ({@code savedRoles}, contenenti
   * ID generato ed entita gestita) e i ruoli arricchiti in memoria ({@code userClientRoles},
   * contenenti tipologia applicativa, descrizione e permessi granulari derivati da {@code
   * ROLE_DEFINITIONS}).
   *
   * <p>Interconnessioni logiche:
   *
   * <ul>
   *   <li>Indicizza i ruoli di {@code userClientRoles} per chiave univoca composita
   *       "CLIENT_ID::ROLE".
   *   <li>Scorre i ruoli di {@code savedRoles} arricchendoli con le proprieta non persistite in
   *       tabella: {@code type}, {@code description} e {@code permissions}.
   *   <li>Preserva le proprieta di persistenza come {@code id} e i timestamp di audit.
   * </ul>
   *
   * @param savedRoles Lista dei ruoli restituiti dal layer di persistenza relazionale.
   * @param userClientRoles Lista dei ruoli arricchiti con le definizioni del catalogo ruoli.
   * @return Lista di ruoli unificata con tutti i campi valorizzati.
   */
  public static List<UserClientRole> mergeUserClientRoles(
      List<UserClientRole> savedRoles, List<UserClientRole> userClientRoles) {
    if (ObjectToolkit.isNullOrEmpty(savedRoles)) {
      return userClientRoles != null ? userClientRoles : List.of();
    }
    if (ObjectToolkit.isNullOrEmpty(userClientRoles)) {
      return savedRoles;
    }

    // Indicizzazione dei ruoli arricchiti per chiave univoca normalizzata "CLIENT_ID::ROLE"
    Map<String, UserClientRole> enrichedMap =
        userClientRoles.stream()
            .filter(
                r ->
                    !ObjectToolkit.isNullOrEmpty(r.getClientId())
                        && !ObjectToolkit.isNullOrEmpty(r.getRole()))
            .collect(
                Collectors.toMap(
                    r ->
                        r.getClientId().trim().toUpperCase()
                            + "::"
                            + r.getRole().trim().toUpperCase(),
                    r -> r,
                    (existing, replacement) -> existing));

    // Per ciascun ruolo persistito, unisce i metadati informativi (type, description, permissions)
    savedRoles.forEach(
        savedRole -> {
          String key =
              (!ObjectToolkit.isNullOrEmpty(savedRole.getClientId())
                      ? savedRole.getClientId().trim().toUpperCase()
                      : "")
                  + "::"
                  + (!ObjectToolkit.isNullOrEmpty(savedRole.getRole())
                      ? savedRole.getRole().trim().toUpperCase()
                      : "");
          UserClientRole enriched = enrichedMap.get(key);
          if (!ObjectToolkit.isNullOrEmpty(enriched)) {
            if (ObjectToolkit.isNullOrEmpty(savedRole.getType())) {
              savedRole.setType(enriched.getType());
            }
            if (ObjectToolkit.isNullOrEmpty(savedRole.getDescription())) {
              savedRole.setDescription(enriched.getDescription());
            }
            if (ObjectToolkit.isNullOrEmpty(savedRole.getPermissions())) {
              savedRole.setPermissions(enriched.getPermissions());
            }
            if (ObjectToolkit.isNullOrEmpty(savedRole.getUserIdentifier())) {
              savedRole.setUserIdentifier(enriched.getUserIdentifier());
            }
          }
        });

    return savedRoles;
  }
}
