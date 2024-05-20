package io.github.giovannilamarmora.accesssphere.grpc.google;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.api.client.auth.oauth2.TokenResponse;
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
    private TokenResponse tokenResponse;
    private GoogleIdToken.Payload userInfo;
    private JWTData jwtData;
}
