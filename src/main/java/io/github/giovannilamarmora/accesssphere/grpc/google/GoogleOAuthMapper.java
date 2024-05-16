package io.github.giovannilamarmora.accesssphere.grpc.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class GoogleOAuthMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleOAuthMapper.class);

  public static User generateGoogleUser(GoogleIdToken.Payload userInfo) {

    return new User(
        getUserInfoValue(userInfo, "given_name"),
        getUserInfoValue(userInfo, "family_name"),
        userInfo.getEmail(),
        userInfo.getSubject(),
        null,
        UserRole.USER,
        getUserInfoValue(userInfo, "picture"),
        null,
        getUserInfoValue(userInfo, "phone_number"),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        userInfo,
        null);
  }

  private static String getUserInfoValue(GoogleIdToken.Payload userInfo, String value) {
    String toReturn =
        ObjectUtils.isEmpty(userInfo.get(value)) ? null : userInfo.get(value).toString();
    if (!ObjectUtils.isEmpty(toReturn)) userInfo.remove(value);
    return toReturn;
  }
}
