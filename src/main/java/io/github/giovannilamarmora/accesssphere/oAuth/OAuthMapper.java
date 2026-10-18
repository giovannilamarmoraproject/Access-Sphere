package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.net.URI;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthMapper {

  private static final Logger LOG = LoggerFilter.getLogger(OAuthMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static OAuthType getOAuthType(
      OAuth2Client oAuth2Client,
      OAuth2Authorization oAuth2Authorization,
      String grant_type,
      String code) {
    if (!ObjectUtils.isEmpty(grant_type) || !ObjectUtils.isEmpty(code))
      return oAuth2Client.getAuthType().equals(OAuthType.ALL_TYPE) && !ObjectUtils.isEmpty(code)
          ? OAuthType.GOOGLE
          : (oAuth2Client.getAuthType().equals(OAuthType.ALL_TYPE)
                  && grant_type.equalsIgnoreCase(GrantType.PASSWORD.type())
              ? OAuthType.BEARER
              : oAuth2Client.getAuthType());
    else return oAuth2Authorization.getType();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static URI getFinalRedirectURI(OAuth2Client oAuth2Client, String defaultRedirectUri) {
    return URI.create(defaultRedirectUri);
  }
}
