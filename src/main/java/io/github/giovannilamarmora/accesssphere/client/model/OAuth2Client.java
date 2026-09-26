package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
 * Modello DTO che rappresenta le credenziali, configurazioni e ruoli applicativi di un Client OAuth 2.0.
 *
 * <p>Mappato con annotazioni OpenAPI {@link Schema} per garantire che la documentazione Swagger UI
 * esponga dettagli, vincoli e valori di esempio accurati e realistici per tutti i campi.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Modello completo di configurazione e credenziali di un Client OAuth 2.0")
public class OAuth2Client extends GenericDTO {

  @Column(name = "CLIENT_ID", nullable = false, unique = true)
  @NotNull(message = "Client ID is required")
  @NotBlank(message = "Client ID is required")
  @UpperCase
  @Schema(
      description = "Identificativo univoco del client OAuth 2.0 (convertito automaticamente in maiuscolo)",
      example = "CLIENT-SAMPLE-001")
  private String clientId;

  @JsonAlias({"externalClientId", "external_client_id"})
  @Schema(
      description = "Identificativo opzionale di correlazione per sistemi terzi integrati",
      example = "EXTERNAL-CRM-99")
  private String externalClientId;

  @JsonAlias({"clientSecret", "client_secret"})
  @Schema(
      description = "Secret del client utilizzato per l'autenticazione OAuth 2.0",
      example = "sec_abc123xyz789demo")
  private String clientSecret;

  @Schema(
      description = "Elenco degli scope OAuth 2.0 autorizzati per il client",
      example = "[\"openid\", \"profile\", \"email\"]")
  private List<String> scopes;

  @JsonDeserialize(using = RedirectUriDeserializer.class)
  @JsonAlias({"redirect_uri", "redirectUri", "redirectUris"})
  @Schema(
      description = "Mappa degli URI di redirect consentiti e dell'URI di fallback di atterraggio",
      example = "{\"redirect_uri\":\"https://my-app.example.com/callback\",\"fallback_uri\":\"https://my-app.example.com/login\"}")
  private Map<String, String> redirect_uri;

  @JsonAlias({"accessType", "access_type"})
  @Schema(
      description = "Modalita di accesso OAuth (ONLINE o OFFLINE per richiedere refresh token)",
      example = "OFFLINE")
  private AccessType accessType;

  @JsonAlias({"authType", "type", "oAuthType", "oauth_type"})
  @Schema(
      description = "Tipologia di grant OAuth 2.0 supportata (es. AUTHORIZATION_CODE, CLIENT_CREDENTIALS)",
      example = "AUTHORIZATION_CODE")
  private OAuthType authType;

  @JsonAlias({"tokenType", "token_type"})
  @Schema(
      description = "Formato del token emesso (JWT o OPAQUE)",
      example = "JWT")
  private TokenType tokenType;

  @JsonAlias({"jwtSecret", "jwt_secret"})
  @Schema(
      description = "Chiave segreta per la firma e verifica crittografica dei token JWT",
      example = "default-jwt-secret-for-access-sphere-authorization-server-256bits")
  private String jwtSecret;

  @JsonAlias({"jwtExpiration", "jwt_expiration"})
  @Schema(
      description = "Durata di validita dell'Access Token espressa in millisecondi (es. 3600000 = 1 ora)",
      example = "3600000")
  private Long jwtExpiration;

  @JsonAlias({"jweSecret", "jwe_secret"})
  @Schema(
      description = "Chiave segreta per la cifratura del payload JWE (se abilitata)",
      example = "optional-jwe-encryption-secret-key-32bytes")
  private String jweSecret;

  @JsonAlias({"jweExpiration", "jwe_expiration"})
  @Schema(
      description = "Durata di validita del token JWE crittografato in millisecondi (es. 86400000 = 24 ore)",
      example = "86400000")
  private Long jweExpiration;

  @JsonAlias({"registrationToken", "registration_token"})
  @Schema(
      description = "Token opzionale per la registrazione dinamica o federata degli utenti sul client",
      example = "reg_tok_abc123xyz")
  private String registrationToken;

  @JsonDeserialize(using = AppRolesDeserializer.class)
  @JsonAlias({"appRoles", "app_roles"})
  @Schema(description = "Catalogo dei ruoli applicativi e permessi definiti per questo specifico client")
  private List<RoleDefinition> appRoles;

  @JsonAlias({"idToken", "id_token"})
  @Schema(
      description = "Indica se emettere il token OIDC ID Token durante il grant di autorizzazione",
      example = "true")
  private Boolean idToken;

  @JsonAlias({"accessToken", "access_token"})
  @Schema(
      description = "Indica se emettere l'Access Token OAuth 2.0 durante il grant",
      example = "true")
  private Boolean accessToken;

  @JsonAlias({"authorize_redirect_status", "authorizeRedirectStatus"})
  @Schema(
      description = "Stato HTTP utilizzato per eseguire il reindirizzamento al redirect_uri",
      example = "FOUND")
  private HttpStatus authorize_redirect_status;

  @JsonAlias({"mfaEnabled", "mfa_enabled"})
  @Schema(
      description = "Indica se l'autenticazione a due fattori (MFA) e obbligatoria per questo client",
      example = "false")
  private Boolean mfaEnabled;

  @JsonAlias({
    "enableRegistration",
    "enable_registration",
    "registrationEnabled",
    "registration_enabled"
  })
  @Schema(
      description = "Indica se la registrazione autonoma o federata degli utenti e abilitata per questo client",
      example = "true")
  private Boolean enableRegistration;

  @Schema(description = "Elenco degli endpoint webhook registrati per notifiche su eventi del client")
  private List<Webhook> webhooks;

  /**
   * Verifica se la registrazione utenti e consentita per questo client (abilitata di default a meno che
   * non sia esplicitamente configurata a false).
   *
   * @return true se la registrazione e abilitata o non specificata, false altrimenti.
   */
  public boolean isRegistrationEnabled() {
    return !Boolean.FALSE.equals(this.enableRegistration);
  }
}
