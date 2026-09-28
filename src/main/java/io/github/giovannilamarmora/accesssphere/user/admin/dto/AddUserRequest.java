package io.github.giovannilamarmora.accesssphere.user.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleRequest;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddUserRequest {

  private String identifier;

  @UpperCamelCase private String name;

  @UpperCamelCase private String surname;

  @NotNull(message = "Email is required")
  @NotBlank(message = "Email is required")
  @LowerCase
  private String email;

  @NotNull(message = "Username is required")
  @NotBlank(message = "Username is required")
  @LowerCase
  private String username;

  private String password;
  private List<UserClientRoleRequest> roles;
  private String profilePhoto;
  private String phoneNumber;
  private LocalDate birthDate;

  @UpperCase private String gender;

  @UpperCamelCase private String occupation;

  @UpperCamelCase private String education;

  private String nationality;

  private String ssn;

  private Map<String, Object> attributes;
  private MFASetting mfaSettings;
}
