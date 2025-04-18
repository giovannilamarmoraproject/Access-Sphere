package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenExchange;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthValidator {

  private static final Logger LOG = LoggerFilter.getLogger(OAuthValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateTokenExchange(
      String token, TokenExchange tokenExchange, ClientCredential clientCredential) {
    validateSubjectToken(token, tokenExchange.getSubject_token());

    validateGrantType(GrantType.TOKEN_EXCHANGE.type(), tokenExchange.getGrant_type());

    validateRequestedTokenType(tokenExchange.getRequested_token_type());

    validateScopes(clientCredential.getScopes(), tokenExchange.getScope());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClientRoles(ClientCredential clientCredential) {
    if (ObjectToolkit.isNullOrEmpty(clientCredential.getAppRoles())) {
      LOG.error("The Client must have a valid role configuration to proceed");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_ROLES, "Invalid Roles Configuration!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUserRoles(ClientCredential clientCredential, List<String> userRoles) {
    if (ObjectToolkit.isNullOrEmpty(userRoles)) {
      LOG.error("The User must have a role to proceed {}", userRoles);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    validateClientRoles(clientCredential);

    if (clientCredential.getAppRoles().stream()
        .noneMatch(appRole -> userRoles.contains(appRole.getRole()))) {
      LOG.error(
          "The User must have a valid role as {} to proceed {}",
          clientCredential.getAppRoles(),
          userRoles);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClient(
      ClientCredential clientCredential,
      String responseType,
      String accessType,
      String redirectUri,
      String scope) {

    validateResponseType("code", responseType);

    validateAccessType(clientCredential.getAccessType().value(), accessType);

    validateRedirectUri(
        clientCredential.getRedirect_uri(), redirectUri, clientCredential.getClientId());

    validateScopes(clientCredential.getScopes(), scope);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateBasicAuth(
      String basic, String grantType, String redirect_uri, ClientCredential clientCredential) {
    if (ObjectUtils.isEmpty(basic) || !basic.contains("Basic")) {
      LOG.error("No Basic Auth found");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_AUTHORIZATION,
          "Invalid basic authorization!");
    }

    validateGrantType(GrantType.PASSWORD.type(), grantType);

    validateRedirectUri(
        clientCredential.getRedirect_uri(), redirect_uri, clientCredential.getClientId());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateOAuthToken(String client_id, String grant_type) {
    if (ObjectUtils.isEmpty(client_id)) {
      LOG.error("No Client ID found");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_CLIENT_ID, "No client_id provided!");
    }

    if (ObjectUtils.isEmpty(grant_type)) {
      LOG.error("No Grant Type found");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_GRANT_TYPE, "No grant_type provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateOAuthGoogle(
      ClientCredential clientCredential,
      String code,
      String scope,
      String redirect_uri,
      String grantType) {

    validateClientRoles(clientCredential);

    validateGrantType(GrantType.AUTHORIZATION_CODE.type(), grantType);

    if (ObjectUtils.isEmpty(code)) {
      LOG.error("No Code found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "The param code is a required field!");
    }

    validateRedirectUri(
        clientCredential.getRedirect_uri(), redirect_uri, clientCredential.getClientId());

    validateScopes(clientCredential.getScopes(), scope);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRefreshToken(String refresh_token, String grant_type) {
    if (ObjectUtils.isEmpty(refresh_token)) {
      LOG.error("You must provide a valid refresh_token!");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, "Invalid request, you must provide a valid refresh_token!");
    }

    validateGrantType(GrantType.REFRESH_TOKEN.type(), grant_type);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRefreshTokenData(
      AccessTokenData accessTokenData, ClientCredential clientCredential, OAuthType oAuthType) {
    validateClientId(accessTokenData.getClientId(), clientCredential.getClientId());

    if (!accessTokenData.getType().equals(oAuthType)) {
      LOG.error(
          "The OAuthType should be {} instead of {}",
          accessTokenData.getType(),
          clientCredential.getAuthType());
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_CLIENT_ID,
          "Invalid client_id provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateGrantType(String expected_grant_type, String actual_grant_type) {
    if (ObjectToolkit.isNullOrEmpty(expected_grant_type)
        || ObjectToolkit.isNullOrEmpty(actual_grant_type)) {
      LOG.error("You must have a valid grant_type {} to proceed", expected_grant_type);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_GRANT_TYPE, "No grant_type provided!");
    }

    if (!actual_grant_type.equalsIgnoreCase(expected_grant_type)) {
      LOG.error(
          "The grant_type provided should be {} instead of {}",
          expected_grant_type,
          actual_grant_type);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_GRANT_TYPE,
          "Invalid grant_type provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateScopes(List<String> expected_scopes, String actual_scope) {
    if (ObjectToolkit.isNullOrEmpty(expected_scopes) || ObjectToolkit.isNullOrEmpty(actual_scope)) {
      LOG.error("You must have a valid scopes {} to proceed", expected_scopes);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "No scopes provided!");
    }

    List<String> scopes = List.of(actual_scope.split(" "));
    if (!new HashSet<>(expected_scopes).containsAll(scopes)) {
      LOG.error("Scopes provided should be {} instead of {}", expected_scopes, actual_scope);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid scope provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClientId(String expected_client_id, String actual_client_id) {
    if (ObjectToolkit.isNullOrEmpty(expected_client_id)
        || ObjectToolkit.isNullOrEmpty(actual_client_id)) {
      LOG.error("You must have a valid client_id {} to proceed", expected_client_id);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400, ExceptionType.INVALID_CLIENT_ID, "No client_id provided!");
    }

    if (!actual_client_id.equalsIgnoreCase(expected_client_id)) {
      LOG.error(
          "The Client ID provided should be {} instead of {}",
          expected_client_id,
          actual_client_id);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_CLIENT_ID,
          "Invalid client_id provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateResponseType(
      String expected_responseType, String actual_responseType) {
    if (ObjectToolkit.isNullOrEmpty(actual_responseType)) {
      LOG.error("The Response Type should be a valid response_type");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid response_type provided!");
    }

    if (!actual_responseType.equalsIgnoreCase(expected_responseType)) {
      LOG.error(
          "The Response Type should be {} instead of {}",
          expected_responseType,
          actual_responseType);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid response_type provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateAccessType(String expected_access_type, String actual_access_type) {
    if (ObjectToolkit.isNullOrEmpty(expected_access_type)
        || ObjectToolkit.isNullOrEmpty(actual_access_type)) {
      LOG.error("You must have a valid access_type {} to proceed", expected_access_type);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "No access_type provided!");
    }

    if (!expected_access_type.equalsIgnoreCase(actual_access_type)) {
      LOG.error(
          "The Access Type provided should be {} instead of {}",
          expected_access_type,
          actual_access_type);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid access_type provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRedirectUri(
      Map<String, String> expected_redirectUri, String actual_redirectUri, String client_id) {
    if (ObjectToolkit.isNullOrEmpty(expected_redirectUri)
        || ObjectToolkit.isNullOrEmpty(actual_redirectUri)) {
      if (ObjectToolkit.isNullOrEmpty(expected_redirectUri)) {
        LOG.error(
            "Missing configuration for the client {} on the redirect uri, miss match expected {} found {}",
            client_id,
            actual_redirectUri,
            expected_redirectUri);
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_400,
            ExceptionType.INVALID_REDIRECT_URI,
            "Invalid redirect_uri provided!");
      }
      LOG.error("You must have a valid redirect_uri {} to proceed", expected_redirectUri);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_REDIRECT_URI,
          "No redirect_uri provided!");
    }

    List<String> redirect_uris = List.of(expected_redirectUri.get("redirect_uri").split(" "));
    if (!redirect_uris.contains(actual_redirectUri)) {
      LOG.error(
          "The redirect_uri provided should be {} instead of {}",
          expected_redirectUri,
          actual_redirectUri);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_REDIRECT_URI,
          "Invalid redirect_uri provided!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateSubjectToken(String bearer, String subject_token) {
    if (!bearer.equalsIgnoreCase("Bearer " + subject_token)) {
      LOG.error("You must have a valid bearer token to proceed");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid Bearer subject_token!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRequestedTokenType(String actual_token_type) {
    if (!actual_token_type.equalsIgnoreCase("urn:ietf:params:oauth:token-mfaMethod:access_token")
        && !actual_token_type.equalsIgnoreCase("urn:ietf:params:oauth:token-mfaMethod:id_token")) {
      LOG.error(
          "You must have a valid requested_token_type like urn:ietf:params:oauth:token-mfaMethod:access_token or urn:ietf:params:oauth:token-mfaMethod:id_token to proceed {}",
          actual_token_type);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }
}
