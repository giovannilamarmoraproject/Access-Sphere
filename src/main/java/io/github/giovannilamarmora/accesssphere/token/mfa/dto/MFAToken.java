package io.github.giovannilamarmora.accesssphere.token.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
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
public class MFAToken extends GenericDTO {

  private String tempToken;

  private LoginStatus loginStatus;

  private List<MFAType> mfaMethods;

  private String subject;

  private String clientId;

  private String sessionId;

  private String identifier;

  private Long issueDate;

  private Long expireDate;

  private Object payload;

  private TokenStatus status;
}
