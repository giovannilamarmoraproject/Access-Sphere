package io.github.giovannilamarmora.accesssphere.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

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
  @LowerCase private List<String> scopes;
  @LowerCase private List<String> redirect_uri;
  private AccessType accessType;
  private OAuthType authType;
  private TokenType tokenType;
  private String jwtSecret;
  private Long jwtExpiration;
  private String jweSecret;
  private Long jweExpiration;
  private String registrationToken;
  private List<AppRole> defaultRoles;
}
