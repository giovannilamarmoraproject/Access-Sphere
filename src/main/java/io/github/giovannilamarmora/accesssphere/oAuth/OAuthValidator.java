package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.token.model.TokenExchange;
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
      String token, TokenExchange tokenExchange, OAuth2Client oAuth2Client) {
    validateSubjectToken(token, tokenExchange.getSubject_token());

    validateGrantType(GrantType.TOKEN_EXCHANGE.type(), tokenExchange.getGrant_type());

    validateRequestedTokenType(tokenExchange.getRequested_token_type());

    validateScopes(oAuth2Client.getScopes(), tokenExchange.getScope());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClientRoles(OAuth2Client oAuth2Client) {
    if (ObjectToolkit.isNullOrEmpty(oAuth2Client.getAppRoles())) {
      LOG.error("❌ The Client must have a valid role configuration to proceed");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_ROLES,
          "Invalid Roles Configuration! At least one role is required.");
    }
    oAuth2Client
        .getAppRoles()
        .forEach(
            role -> {
              if (ObjectToolkit.isNullOrEmpty(role.getRole())) {
                LOG.error("❌ The Client must have a valid role configuration to proceed");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400,
                    ExceptionType.INVALID_ROLES,
                    "Invalid Roles Configuration! At least one role is required.");
              }
              if (ObjectToolkit.isNullOrEmpty(role.getType())) {
                LOG.error("❌ The Client must have a valid role type configuration to proceed");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400,
                    ExceptionType.INVALID_ROLES,
                    "Invalid Roles Configuration! At least one role type is required.");
              }
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUserRoles(OAuth2Client oAuth2Client, List<?> userRoles) {
    if (ObjectToolkit.isNullOrEmpty(userRoles)) {
      LOG.error("The User must have a role to proceed {}", userRoles);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    validateClientRoles(oAuth2Client);

    List<String> roleNames =
        userRoles.stream()
            .map(
                r -> {
                  if (r instanceof UserClientRole ucr) {
                    return ucr.getRole();
                  } else if (r instanceof String s) {
                    return s;
                  }
                  return r != null ? r.toString() : null;
                })
            .filter(java.util.Objects::nonNull)
            .toList();

    if (oAuth2Client.getAppRoles().stream()
        .filter(java.util.Objects::nonNull)
        .noneMatch(appRole -> roleNames.stream().anyMatch(appRole::matches))) {
      LOG.error(
          "The User must have a valid role as {} to proceed {}",
          oAuth2Client.getAppRoles(),
          roleNames);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateClient(
      OAuth2Client oAuth2Client,
      String responseType,
      String accessType,
      String redirectUri,
      String scope) {

    validateResponseType("code", responseType);

    validateAccessType(oAuth2Client.getAccessType().value(), accessType);

    validateRedirectUri(oAuth2Client.getRedirect_uri(), redirectUri, oAuth2Client.getClientId());

    validateScopes(oAuth2Client.getScopes(), scope);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateBasicAuth(
      String basic, String grantType, String redirect_uri, OAuth2Client oAuth2Client) {
    if (ObjectUtils.isEmpty(basic) || !basic.contains("Basic")) {
      LOG.error("No Basic Auth found");
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_AUTHORIZATION,
          "Invalid basic authorization!");
    }

    validateGrantType(GrantType.PASSWORD.type(), grantType);

    validateRedirectUri(oAuth2Client.getRedirect_uri(), redirect_uri, oAuth2Client.getClientId());
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
      OAuth2Client oAuth2Client, String code, String scope, String redirect_uri, String grantType) {

    validateClientRoles(oAuth2Client);

    validateGrantType(GrantType.AUTHORIZATION_CODE.type(), grantType);

    if (ObjectUtils.isEmpty(code)) {
      LOG.error("No Code found");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "The param code is a required field!");
    }

    validateRedirectUri(oAuth2Client.getRedirect_uri(), redirect_uri, oAuth2Client.getClientId());

    validateScopes(oAuth2Client.getScopes(), scope);
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
      OAuth2Authorization oAuth2Authorization, OAuth2Client oAuth2Client, OAuthType oAuthType) {
    validateClientId(oAuth2Authorization.getClientId(), oAuth2Client.getClientId());

    if (!oAuth2Authorization.getType().equals(oAuthType)) {
      LOG.error(
          "The OAuthType should be {} instead of {}",
          oAuth2Authorization.getType(),
          oAuth2Client.getAuthType());
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
  public static void validatePrompt(String prompt) {
    if (ObjectToolkit.isNullOrEmpty(prompt)) {
      return;
    }
    List<String> allowedPrompts = List.of("none", "login", "consent", "select_account");
    for (String p : prompt.trim().split("\\s+")) {
      if (!allowedPrompts.contains(p)) {
        LOG.error("Invalid prompt '{}' provided", p);
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_400,
            "Invalid prompt provided! Allowed values are: none, login, consent, select_account");
      }
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

    String configuredUris = expected_redirectUri.get(RedirectUris.REDIRECT_URI.url());
    if (ObjectToolkit.isNullOrEmpty(configuredUris)) {
      configuredUris = expected_redirectUri.get("redirectUri");
    }
    if (ObjectToolkit.isNullOrEmpty(configuredUris)) {
      LOG.error(
          "No redirect_uri key found in client configuration map for client {}: {}",
          client_id,
          expected_redirectUri);
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_400,
          ExceptionType.INVALID_REDIRECT_URI,
          "No redirect_uri configured on client!");
    }

    List<String> redirect_uris = List.of(configuredUris.trim().split("[ ,]+"));
    boolean isValid = redirect_uris.contains(actual_redirectUri);
    if (!isValid) {
      for (String u : redirect_uris) {
        if (actual_redirectUri.equals(u)
            || actual_redirectUri.equals(u.replace("/app/users", "/app"))
            || actual_redirectUri.equals(u.replace("/app/users", "/app/dashboard"))
            || (u.endsWith("/app/users")
                && (actual_redirectUri.equals(u.replace("/app/users", ""))
                    || actual_redirectUri.startsWith(u.replace("/app/users", "/app"))))) {
          isValid = true;
          break;
        }
      }
    }
    if (!isValid) {
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
    if (!actual_token_type.equalsIgnoreCase("urn:ietf:params:oauth:token-type:access_token")
        && !actual_token_type.equalsIgnoreCase("urn:ietf:params:oauth:token-type:id_token")) {
      LOG.error(
          "You must have a valid requested_token_type like urn:ietf:params:oauth:token-type:access_token or urn:ietf:params:oauth:token-type:id_token to proceed {}",
          actual_token_type);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
    }
  }
}
