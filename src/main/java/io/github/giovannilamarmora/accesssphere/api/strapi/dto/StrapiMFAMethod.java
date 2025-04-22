package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFALabel;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrapiMFAMethod extends GenericDTO {
  private MFAType type;
  private MFALabel label;

  // @JsonIgnore
  // @Convert(converter = SecretKeyConverter.class)
  private String secretKey;

  private boolean confirmed;

  public StrapiMFAMethod(
      MFAType type,
      MFALabel label,
      String secretKey,
      boolean confirmed,
      LocalDateTime creationDate,
      LocalDateTime updateDate) {
    super(null, creationDate, updateDate, null);
    this.type = type;
    this.label = label;
    this.secretKey = secretKey;
    this.confirmed = confirmed;
  }

  // public String getSecretKey() {
  //  return CryptoUtils.decrypt(secretKey);
  // }

  // public void setSecretKey(String secretKey) {
  //  this.secretKey = CryptoUtils.encrypt(secretKey);
  // }
}
