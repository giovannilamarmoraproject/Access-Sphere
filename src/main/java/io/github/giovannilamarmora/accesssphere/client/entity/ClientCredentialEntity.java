package io.github.giovannilamarmora.accesssphere.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "OAUTH_CLIENT")
public class ClientCredentialEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "CLIENT_ID", nullable = false, unique = true)
  private String clientId;

  @Column(name = "EXTERNAL_CLIENT_ID", unique = true)
  private String externalClientId;

  @Column(name = "CLIENT_SECRET")
  private String clientSecret;

  @Lob
  @Column(name = "SCOPES", columnDefinition = "TEXT")
  private String scopes;

  @Lob
  @Column(name = "REDIRECT_URI", columnDefinition = "TEXT")
  private String redirect_uri;

  @Enumerated(EnumType.STRING)
  @Column(name = "ACCESS_TYPE")
  private AccessType accessType;

  @Enumerated(EnumType.STRING)
  @Column(name = "OAUTH_TYPE")
  private OAuthType authType;

  @Enumerated(EnumType.STRING)
  @Column(name = "TOKEN_TYPE", nullable = false)
  private TokenType tokenType;

  @Column(name = "JWT_SECRET")
  private String jwtSecret;

  @Column(name = "JWT_EXPIRATION")
  private Long jwtExpiration;

  @Column(name = "JWE_SECRET")
  private String jweSecret;

  @Column(name = "JWE_EXPIRATION")
  private Long jweExpiration;

  @Column(name = "REGISTRATION_TOKEN")
  private String registrationToken;

  @Lob
  @Column(name = "APP_ROLES", columnDefinition = "TEXT")
  private String appRoles;

  @Column(name = "ID_TOKEN")
  private Boolean idToken;

  @Column(name = "ACCESS_TOKEN")
  private Boolean accessToken;

  @Column(name = "STRAPI_TOKEN")
  private Boolean strapiToken;
}
