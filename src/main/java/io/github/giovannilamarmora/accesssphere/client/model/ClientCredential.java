package io.github.giovannilamarmora.accesssphere.client.model;

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

  private String externalClientId;
  private String clientSecret;
  private List<String> scopes;
  private Map<String, String> redirect_uri;
  private AccessType accessType;
  private OAuthType authType;
  private TokenType tokenType;
  private String jwtSecret;
  private Long jwtExpiration;
  private String jweSecret;
  private Long jweExpiration;
  private String registrationToken;
  private List<AppRole> appRoles;
  private Boolean idToken;
  private Boolean accessToken;
  private Boolean strapiToken;
  private HttpStatus authorize_redirect_status;
  private Boolean mfaEnabled;
  private List<Webhook> webhooks;
}
