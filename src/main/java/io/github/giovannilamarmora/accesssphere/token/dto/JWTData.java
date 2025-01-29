package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.utilities.Utilities;
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
  private String name;
  private String email;
  private String picture;
  private String given_name;
  private String family_name;
  private String at_hash;
  private boolean email_verified;
  private List<String> roles;
  private OAuthType type;
  private String client_id;
  private Map<String, Object> attributes;

  public static JWTData generateJWTData(
      User user, ClientCredential clientCredential, ServerHttpRequest request) {
    String remoteAddress = WebManager.getRemoteAddress(request);
    return new JWTData(
        user.getIdentifier(),
        Utilities.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        Utilities.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        0,
        System.currentTimeMillis(),
        Utilities.isNullOrEmpty(remoteAddress) ? null : "https://" + remoteAddress,
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
        Utilities.isNullOrEmpty(user.getRoles()) ? null : user.getRoles(),
        // ObjectUtils.isEmpty(defaultRole) ? null : List.of(defaultRole.getRole()),
        //   : clientCredential.getDefaultRoles().stream().map(AppRole::getRole).toList(),
        OAuthType.BEARER,
        clientCredential.getClientId(),
        null);
  }
}
