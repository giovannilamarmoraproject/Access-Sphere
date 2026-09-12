package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
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

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientCredential extends GenericDTO {

  @Column(name = "CLIENT_ID", nullable = false, unique = true)
  @NotNull(message = "Client ID is required")
  @NotBlank(message = "Client ID is required")
  @UpperCase
  private String clientId;

  @JsonAlias({"externalClientId", "external_client_id"})
  private String externalClientId;

  @JsonAlias({"clientSecret", "client_secret"})
  private String clientSecret;

  private List<String> scopes;

  @JsonAlias({"redirect_uri", "redirectUri", "redirectUris"})
  private Map<String, String> redirect_uri;

  @JsonAlias({"accessType", "access_type"})
  private AccessType accessType;

  @JsonAlias({"authType", "type", "oAuthType", "oauth_type"})
  private OAuthType authType;

  @JsonAlias({"tokenType", "token_type"})
  private TokenType tokenType;

  @JsonAlias({"jwtSecret", "jwt_secret"})
  private String jwtSecret;

  @JsonAlias({"jwtExpiration", "jwt_expiration"})
  private Long jwtExpiration;

  @JsonAlias({"jweSecret", "jwe_secret"})
  private String jweSecret;

  @JsonAlias({"jweExpiration", "jwe_expiration"})
  private Long jweExpiration;

  @JsonAlias({"registrationToken", "registration_token"})
  private String registrationToken;

  @JsonAlias({"appRoles", "app_roles"})
  private List<AppRole> appRoles;

  @JsonAlias({"idToken", "id_token"})
  private Boolean idToken;

  @JsonAlias({"accessToken", "access_token"})
  private Boolean accessToken;

  @JsonAlias({"strapiToken", "strapi_token"})
  private Boolean strapiToken;

  @JsonAlias({"authorize_redirect_status", "authorizeRedirectStatus"})
  private HttpStatus authorize_redirect_status;

  @JsonAlias({"mfaEnabled", "mfa_enabled"})
  private Boolean mfaEnabled;

  private List<Webhook> webhooks;
}
