package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
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
      User user,
      ClientCredential clientCredential,
      SubjectType subject_type,
      ServerHttpRequest request) {
    String remoteAddress = WebManager.getRemoteAddress(request);
    return new JWTData(
        user.getIdentifier(),
        ObjectToolkit.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        ObjectToolkit.isNullOrEmpty(remoteAddress)
            ? null
            : clientCredential.getClientId() + "." + remoteAddress,
        0,
        System.currentTimeMillis(),
        ObjectToolkit.isNullOrEmpty(remoteAddress) ? null : "https://" + remoteAddress,
        user.getUsername(),
        subject_type,
        ObjectUtils.isEmpty(user.getSurname()) || ObjectUtils.isEmpty(user.getName())
            ? null
            : Joiner.on(" ").join(user.getSurname(), user.getName()),
        user.getEmail(),
        user.getProfilePhoto(),
        user.getName(),
        user.getSurname(),
        null,
        true,
        ObjectToolkit.isNullOrEmpty(user.getRoles()) ? null : user.getRoles(),
        // ObjectUtils.isEmpty(defaultRole) ? null : List.of(defaultRole.getRole()),
        //   : clientCredential.getDefaultRoles().stream().map(AppRole::getRole).toList(),
        OAuthType.BEARER,
        clientCredential.getClientId(),
        null);
  }
}
