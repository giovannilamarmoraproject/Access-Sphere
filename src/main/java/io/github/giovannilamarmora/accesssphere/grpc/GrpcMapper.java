package io.github.giovannilamarmora.accesssphere.grpc;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class GrpcMapper {

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static JWTData fromGoogleDataToJWTData(
      GoogleIdToken.Payload payload, ClientCredential clientCredential) {
    AppRole defaultRole =
        clientCredential.getAppRoles().stream()
            .filter(appRole -> appRole.getType().equalsIgnoreCase("default"))
            .toList()
            .getFirst();
    return new JWTData(
        null,
        payload.getAudience().toString(),
        payload.getAudience().toString(),
        payload.getExpirationTimeSeconds(),
        payload.getIssuedAtTimeSeconds(),
        payload.getIssuer(),
        payload.getSubject(),
        getUserInfoValue(payload, "name"),
        payload.getEmail(),
        getUserInfoValue(payload, "picture"),
        getUserInfoValue(payload, "given_name"),
        getUserInfoValue(payload, "family_name"),
        (String) payload.get("at_hash"),
        payload.getEmailVerified(),
        ObjectUtils.isEmpty(defaultRole) ? null : List.of(defaultRole.getRole()),
        //    : clientCredential.getDefaultRole().stream().map(AppRole::getRole).toList(),
        OAuthType.GOOGLE,
        clientCredential.getClientId(),
        null);
  }

  public static JWTData setIdentifier(JWTData jwtData, String identifier) {
    jwtData.setIdentifier(identifier);
    return jwtData;
  }

  private static String getUserInfoValue(GoogleIdToken.Payload userInfo, String value) {
    String toReturn =
        ObjectUtils.isEmpty(userInfo.get(value)) ? null : userInfo.get(value).toString();
    if (!ObjectUtils.isEmpty(toReturn)) userInfo.remove(value);
    return toReturn;
  }
}
