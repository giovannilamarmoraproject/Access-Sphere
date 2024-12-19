package io.github.giovannilamarmora.accesssphere.token.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessTokenData extends GenericDTO {
  private String refreshTokenHash;
  private String accessTokenHash;
  private String idTokenHash;
  private String clientId;
  private String sessionId;
  private String subject;
  private String email;
  private String identifier;
  private OAuthType type;
  private Long issueDate;
  private Long refreshExpireDate;
  private Long accessExpireDate;
  private JsonNode payload;
  private TokenStatus status;
  private List<String> roles;
}
