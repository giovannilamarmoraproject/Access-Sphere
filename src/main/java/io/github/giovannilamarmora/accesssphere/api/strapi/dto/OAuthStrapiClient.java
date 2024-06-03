package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthStrapiClient extends StrapiGeneric {

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
  private String registrationToken;
  private DefaultRoles default_roles;

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DefaultRoles {
    private List<StrapiData> data;
  }

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StrapiData {
    private Long id;
    private AppRole attributes;
  }
}
