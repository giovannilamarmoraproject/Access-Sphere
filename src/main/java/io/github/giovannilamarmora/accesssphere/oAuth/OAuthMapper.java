package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
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
      ClientCredential clientCredential,
      AccessTokenData accessTokenData,
      String grant_type,
      String code) {
    if (!ObjectUtils.isEmpty(grant_type) || !ObjectUtils.isEmpty(code))
      return clientCredential.getAuthType().equals(OAuthType.ALL_TYPE) && !ObjectUtils.isEmpty(code)
          ? OAuthType.GOOGLE
          : (clientCredential.getAuthType().equals(OAuthType.ALL_TYPE)
                  && grant_type.equalsIgnoreCase(GrantType.PASSWORD.type())
              ? OAuthType.BEARER
              : clientCredential.getAuthType());
    else return accessTokenData.getType();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static URI getFinalRedirectURI(
      ClientCredential clientCredential, RedirectUris redirectUris, String defaultRedirectUri) {
    if (!ObjectToolkit.isNullOrEmpty(clientCredential.getRedirect_uri())
        && clientCredential.getRedirect_uri().containsKey(redirectUris.url()))
      return URI.create(clientCredential.getRedirect_uri().get(redirectUris.url()));
    else return URI.create(defaultRedirectUri);
  }
}
