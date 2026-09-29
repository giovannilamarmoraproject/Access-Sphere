package io.github.giovannilamarmora.accesssphere.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDataResponse extends GenericDTO {

  private String identifier;

  @UpperCamelCase private String name;

  @UpperCamelCase private String surname;

  @LowerCase private String email;

  @LowerCase private String username;

  private String password;
  private List<UserClientRole> roles;
  private String profilePhoto;
  private String phoneNumber;
  private LocalDate birthDate;

  @UpperCase private String gender;

  @UpperCamelCase private String occupation;

  @UpperCamelCase private String education;

  private String nationality;

  private String ssn; // Social Security Number

  private String tokenReset;
  private Boolean confirmed;
  private Boolean blocked;

  private Map<String, Object> attributes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private MFASetting mfaSettings;
}
