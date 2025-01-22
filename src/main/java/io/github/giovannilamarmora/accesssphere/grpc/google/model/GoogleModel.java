package io.github.giovannilamarmora.accesssphere.grpc.google.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleModel {
  private GoogleTokenResponse tokenResponse;
  private GoogleIdToken.Payload userInfo;
  private JWTData jwtData;
}
