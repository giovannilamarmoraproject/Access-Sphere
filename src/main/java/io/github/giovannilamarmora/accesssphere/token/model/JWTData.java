package io.github.giovannilamarmora.accesssphere.token.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.utilities.URLUtils;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.WebManager;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JWTData {
  private String identifier;
  private String aud;
  private String azp;
  private long exp;
  private long iat;
  private String iss;
  private String sub;
  private SubjectType subject_type;
  private String name;
  private String email;
  // private String picture;
  private String given_name;
  private String family_name;
  private String at_hash;
  private boolean email_verified;
  private List<String> roles;
  private List<String> permissions;
  private OAuthType type;
  private String client_id;
  private Map<String, Object> attributes;

  public JWTData(
      String identifier,
      String aud,
      String azp,
      long exp,
      long iat,
      String iss,
      String sub,
      SubjectType subject_type,
      String name,
      String email,
      String given_name,
      String family_name,
      String at_hash,
      boolean email_verified,
      List<String> roles,
      OAuthType type,
      String client_id,
      Map<String, Object> attributes) {
    this(
        identifier,
        aud,
        azp,
        exp,
        iat,
        iss,
        sub,
        subject_type,
        name,
        email,
        given_name,
        family_name,
        at_hash,
        email_verified,
        roles,
        null,
        type,
        client_id,
        attributes);
  }

  public static JWTData generateJWTData(
      User user,
      ClientCredential clientCredential,
      SubjectType subject_type,
      ServerHttpRequest request) {
    String remoteAddress = WebManager.getRemoteAddress(request);

    List<String> effectiveRoles = null;
    List<String> effectivePermissions = null;

    if (user != null && user.getClientRoles() != null && !user.getClientRoles().isEmpty()) {
      String targetClientId = clientCredential != null ? clientCredential.getClientId() : null;
      List<io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO>
          matchingClientRoles =
              targetClientId == null
                  ? List.of()
                  : user.getClientRoles().stream()
                      .filter(
                          cr ->
                              cr.getClientId() != null
                                  && cr.getClientId().equalsIgnoreCase(targetClientId))
                      .toList();

      if (!matchingClientRoles.isEmpty()) {
        effectiveRoles =
            matchingClientRoles.stream()
                .map(io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO::getRole)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        effectivePermissions =
            matchingClientRoles.stream()
                .filter(cr -> cr.getPermissions() != null)
                .flatMap(cr -> cr.getPermissions().stream())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
      }
    }

    // Fallback retrocompatibile su user.getRoles()
    if (ObjectToolkit.isNullOrEmpty(effectiveRoles) && user != null) {
      effectiveRoles = ObjectToolkit.isNullOrEmpty(user.getRoles()) ? null : user.getRoles();
    }

    // Risoluzione permessi da clientCredential.appRoles se non definiti a livello di UserClientRole
    if (ObjectToolkit.isNullOrEmpty(effectivePermissions)
        && clientCredential != null
        && clientCredential.getAppRoles() != null
        && effectiveRoles != null) {
      List<String> finalRoles = effectiveRoles;
      effectivePermissions =
          clientCredential.getAppRoles().stream()
              .filter(
                  ar ->
                      ar != null
                          && ar.getRole() != null
                          && finalRoles.stream().anyMatch(ar::matches))
              .filter(ar -> ar.getPermissions() != null)
              .flatMap(ar -> ar.getPermissions().stream())
              .filter(java.util.Objects::nonNull)
              .distinct()
              .toList();
    }

    return new JWTData(
        user != null ? user.getIdentifier() : null,
        ObjectToolkit.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        ObjectToolkit.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        clientCredential.getJwtExpiration(),
        System.currentTimeMillis(),
        URLUtils.resolveIssuer(remoteAddress, request),
        user != null ? user.getUsername() : null,
        subject_type,
        user == null
                || ObjectUtils.isEmpty(user.getSurname())
                || ObjectUtils.isEmpty(user.getName())
            ? null
            : Joiner.on(" ").join(user.getSurname(), user.getName()),
        user != null ? user.getEmail() : null,
        user != null ? user.getName() : null,
        user != null ? user.getSurname() : null,
        null,
        true,
        ObjectToolkit.isNullOrEmpty(effectiveRoles) ? null : effectiveRoles,
        ObjectToolkit.isNullOrEmpty(effectivePermissions) ? null : effectivePermissions,
        OAuthType.BEARER,
        clientCredential.getClientId(),
        null);
  }
}
