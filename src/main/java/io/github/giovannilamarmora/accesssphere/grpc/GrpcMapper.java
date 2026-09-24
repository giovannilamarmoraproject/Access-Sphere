package io.github.giovannilamarmora.accesssphere.grpc;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.token.model.SubjectType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class GrpcMapper {

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static JWTData fromGoogleDataToJWTData(
      GoogleIdToken.Payload payload, OAuth2Client oAuth2Client) {
    AppRole defaultRole =
        (oAuth2Client.getAppRoles() == null)
            ? null
            : oAuth2Client.getAppRoles().stream()
                .filter(appRole -> appRole != null && appRole.isUser())
                .findFirst()
                .orElseGet(
                    () ->
                        oAuth2Client.getAppRoles().isEmpty()
                            ? null
                            : oAuth2Client.getAppRoles().getFirst());
    return new JWTData(
        null,
        payload.getAudience().toString(),
        payload.getAudience().toString(),
        payload.getExpirationTimeSeconds(),
        payload.getIssuedAtTimeSeconds(),
        payload.getIssuer(),
        payload.getSubject(),
        SubjectType.CUSTOMER,
        getUserInfoValue(payload, "name"),
        payload.getEmail(),
        // getUserInfoValue(payload, "picture"),
        getUserInfoValue(payload, "given_name"),
        getUserInfoValue(payload, "family_name"),
        (String) payload.get("at_hash"),
        payload.getEmailVerified(),
        ObjectUtils.isEmpty(defaultRole) ? null : List.of(defaultRole.getRole()),
        //    : clientCredential.getDefaultRole().stream().map(AppRole::getRole).toList(),
        OAuthType.GOOGLE,
        oAuth2Client.getClientId(),
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
