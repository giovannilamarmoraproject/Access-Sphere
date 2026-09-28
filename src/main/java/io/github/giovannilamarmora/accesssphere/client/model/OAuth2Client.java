package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class OAuth2Client extends GenericDTO {

  @Column(name = "CLIENT_ID", nullable = false, unique = true)
  @NotNull(message = "Client ID is required")
  @NotBlank(message = "Client ID is required")
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
      description = "Map of allowed redirect URIs and the fallback landing URI",
      example =
          "{\"redirect_uri\":\"https://my-app.example.com/callback\",\"fallback_uri\":\"https://my-app.example.com/login\"}")
  private Map<String, String> redirect_uri;

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
  private List<RoleDefinition> appRoles;

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
