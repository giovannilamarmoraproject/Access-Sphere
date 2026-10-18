package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
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
public class OAuth2Client extends GenericDTO {

  @UpperCase private String clientId;
  private String externalClientId;
  private String clientSecret;
  private List<String> scopes;
  @JsonProperty("redirect_uris")
  @JsonAlias({"redirectUris", "redirect_uri", "redirectUri"})
  private List<String> redirectUris;

  @JsonProperty("post_logout_redirect_uris")
  @JsonAlias({"postLogoutRedirectUris", "post_logout_redirect_uri", "fallback_uri", "fallbackUri"})
  private List<String> postLogoutRedirectUris;
  private AccessType accessType;
  private OAuthType authType;
  private TokenType tokenType;
  private String jwtSecret;
  private Long jwtExpiration;
  private String jweSecret;
  private Long jweExpiration;
  private List<RoleDefinition> appRoles;
  private Boolean idToken;
  private Boolean accessToken;
  private HttpStatus authorize_redirect_status;
  private Boolean mfaEnabled;
  private Boolean enableRegistration;
  private List<Webhook> webhooks;
}
