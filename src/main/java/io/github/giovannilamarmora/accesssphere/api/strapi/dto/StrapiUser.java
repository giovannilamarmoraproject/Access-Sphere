package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrapiUser extends StrapiGeneric {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String identifier;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCamelCase
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCamelCase
  private String surname;

  @NotNull(message = "Email is required")
  @NotBlank(message = "Email is required")
  @LowerCase
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean confirmed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean blocked;

  @NotNull(message = "Username is required")
  @NotBlank(message = "Username is required")
  @LowerCase
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String username;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String password;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<StrapiAddress> addresses;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<AppRole> app_roles;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String profilePhoto;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String phoneNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocalDate birthDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCase
  private String gender;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCamelCase
  private String occupation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCamelCase
  private String education;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @UpperCamelCase
  private String nationality;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String ssn; // Social Security Number

  private String tokenReset;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> attributes;
}
