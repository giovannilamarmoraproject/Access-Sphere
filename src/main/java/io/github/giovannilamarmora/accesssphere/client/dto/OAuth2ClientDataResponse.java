package io.github.giovannilamarmora.accesssphere.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.config.GenericResponse;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.dto.RoleDefinitionResponse;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Modello DTO che rappresenta le credenziali, configurazioni e ruoli applicativi di un Client OAuth
 * 2.0.
 *
 * <p>Mappato con annotazioni OpenAPI {@link Schema} per garantire che la documentazione Swagger UI
 * esponga dettagli, vincoli e valori di esempio accurati e realistici per tutti i campi.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Complete configuration and credentials model for an OAuth 2.0 Client")
public class OAuth2ClientDataResponse extends GenericResponse {

  @UpperCase
  @Schema(
      description =
          "Unique identifier of the OAuth 2.0 client (automatically converted to uppercase)",
      example = "CLIENT-SAMPLE-001")
  private String clientId;

  @Schema(
      description = "Optional correlation identifier for integrated third-party systems",
      example = "EXTERNAL-CRM-99")
  private String externalClientId;

  @Schema(
      description = "Client secret used for OAuth 2.0 authentication",
      example = "sec_abc123xyz789demo")
  private String clientSecret;

  @Schema(
      description = "List of authorized OAuth 2.0 scopes for the client",
      example = "[\"openid\", \"profile\", \"email\"]")
  private List<String> scopes;

  @Schema(
      description = "List of allowed OAuth 2.0 redirect URIs",
      example = "[\"https://my-app.example.com/callback\"]")
  @JsonProperty("redirect_uris")
  @JsonAlias({"redirectUris", "redirect_uri", "redirectUri"})
  private List<String> redirectUris;

  @Schema(
      description = "List of allowed post-logout redirect URIs",
      example = "[\"https://my-app.example.com/login\"]")
  @JsonProperty("post_logout_redirect_uris")
  @JsonAlias({"postLogoutRedirectUris", "post_logout_redirect_uri", "fallback_uri", "fallbackUri"})
  private List<String> postLogoutRedirectUris;

  @Schema(
      description = "OAuth access type (ONLINE or OFFLINE to request a refresh token)",
      example = "OFFLINE")
  private AccessType accessType;

  @Schema(
      description = "Supported OAuth 2.0 grant type (e.g. AUTHORIZATION_CODE, CLIENT_CREDENTIALS)",
      example = "AUTHORIZATION_CODE")
  private OAuthType authType;

  @Schema(description = "Format of the issued token (JWT or OPAQUE)", example = "JWT")
  private TokenType tokenType;

  @Schema(
      description = "Secret key for signing and cryptographic verification of JWT tokens",
      example = "default-jwt-secret-for-access-sphere-authorization-server-256bits")
  private String jwtSecret;

  @Schema(
      description =
          "Access Token validity duration expressed in milliseconds (e.g. 3600000 = 1 hour)",
      example = "3600000")
  private Long jwtExpiration;

  @Schema(
      description = "Secret key for JWE payload encryption (if enabled)",
      example = "optional-jwe-encryption-secret-key-32bytes")
  private String jweSecret;

  @Schema(
      description =
          "Encrypted JWE token validity duration in milliseconds (e.g. 86400000 = 24 hours)",
      example = "86400000")
  private Long jweExpiration;

  @Schema(
      description = "Catalog of application roles and permissions defined for this specific client")
  private List<RoleDefinitionResponse> appRoles;

  @Schema(
      description = "Indicates whether to issue the OIDC ID Token during the authorization grant",
      example = "true")
  private Boolean idToken;

  @Schema(
      description = "Indicates whether to issue the OAuth 2.0 Access Token during the grant",
      example = "true")
  private Boolean accessToken;

  @Schema(
      description = "HTTP status code used to perform the redirection to the redirect_uri",
      example = "FOUND")
  private HttpStatus authorize_redirect_status;

  @Schema(
      description =
          "Indicates whether multi-factor authentication (MFA) is required for this client",
      example = "false")
  private Boolean mfaEnabled;

  @Schema(
      description =
          "Indicates whether self-service or federated user registration is enabled for this client",
      example = "true")
  private Boolean enableRegistration;

  @Schema(description = "List of registered webhook endpoints for client event notifications")
  private List<Webhook> webhooks;
}
