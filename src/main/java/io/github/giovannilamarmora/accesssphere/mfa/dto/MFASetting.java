package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MFASetting {
  private Boolean enabled;
  private List<MFAMethod> mfaMethods;
}
