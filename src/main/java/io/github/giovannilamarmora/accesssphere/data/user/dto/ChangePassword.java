package io.github.giovannilamarmora.accesssphere.data.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePassword {
  private String templateId;
  private String email;
  private String password;
  private String token;
  private Map<String, String> params;
}
