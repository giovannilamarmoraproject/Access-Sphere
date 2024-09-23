package io.github.giovannilamarmora.accesssphere.grpc.google.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleTokenResponse extends TokenResponse {
  @Key("strapi_token")
  private String strapiToken;

  public static GoogleTokenResponse setTokenResponse(TokenResponse response) {
    GoogleTokenResponse googleTokenResponse = new GoogleTokenResponse();
    if (ObjectUtils.isEmpty(response)) return googleTokenResponse;
    BeanUtils.copyProperties(response, googleTokenResponse);
    return googleTokenResponse;
  }
}
