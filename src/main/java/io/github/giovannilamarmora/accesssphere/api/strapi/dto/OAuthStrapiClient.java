package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthStrapiClient {

  private String clientId;
  private String externalClientId;
  private String clientSecret;
  private String scopes;
  private String redirectUri;
  private String accessType;
  private String type;
  private String tokenType;
  private String jwtSecret;
  private Long jwtExpiration;
  private String jweSecret;
  private Long jweExpiration;
  private String createdAt;
  private String updatedAt;
  private String publishedAt;
  private String registrationToken;
}
