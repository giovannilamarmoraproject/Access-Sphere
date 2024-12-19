package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.utilities.Utilities;
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
  private String name;
  private String email;
  private String picture;
  private String given_name;
  private String family_name;
  private String at_hash;
  private boolean email_verified;
  private List<String> roles;
  private OAuthType type;
  private Map<String, Object> attributes;

  public static JWTData generateJWTData(
      User user, ClientCredential clientCredential, ServerHttpRequest request) {
    AppRole defaultRole =
        !Utilities.isNullOrEmpty(clientCredential.getAppRoles())
            ? clientCredential.getAppRoles().stream()
                .filter(appRole -> appRole.getType().equalsIgnoreCase("default"))
                .toList()
                .getFirst()
            : null;
    return new JWTData(
        user.getIdentifier(),
        ObjectUtils.isEmpty(request.getRemoteAddress())
            ? null
            : clientCredential.getClientId() + "." + request.getRemoteAddress().getHostName(),
        ObjectUtils.isEmpty(request.getRemoteAddress())
            ? null
            : clientCredential.getClientId() + "." + request.getRemoteAddress().getHostName(),
        0,
        System.currentTimeMillis(),
        ObjectUtils.isEmpty(request.getRemoteAddress())
            ? null
            : "https://" + request.getRemoteAddress().getHostName(),
        user.getUsername(),
        ObjectUtils.isEmpty(user.getSurname()) || ObjectUtils.isEmpty(user.getName())
            ? null
            : Joiner.on(" ").join(user.getSurname(), user.getName()),
        user.getEmail(),
        user.getProfilePhoto(),
        user.getName(),
        user.getSurname(),
        null,
        true,
        ObjectUtils.isEmpty(defaultRole) ? null : List.of(defaultRole.getRole()),
        //   : clientCredential.getDefaultRoles().stream().map(AppRole::getRole).toList(),
        OAuthType.BEARER,
        null);
  }
}
