package io.github.giovannilamarmora.accesssphere.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASetting;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserUpdateRequest {

  @UpperCamelCase
  @Schema(description = "First name of the user", example = "John")
  private String name;

  @UpperCamelCase
  @Schema(description = "Last name / family name of the user", example = "Doe")
  private String surname;

  @NotBlank(message = "The field 'email' is required and it cannot be null or blank")
  @LowerCase
  @Schema(description = "Primary email address of the user", example = "john.doe@example.com")
  private String email;

  @Schema(
      description = "Profile avatar URL or Base64 data URI",
      example = "https://bootdey.com/img/Content/avatar/avatar7.png")
  private String profilePhoto;

  @Schema(
      description = "Contact phone number including international prefix",
      example = "+39 3401234567")
  private String phoneNumber;

  @Schema(
      description = "Date of birth of the user (ISO-8601 format YYYY-MM-DD)",
      example = "1990-01-15")
  private LocalDate birthDate;

  @UpperCase
  @Schema(
      description = "Gender of the user",
      example = "MALE",
      allowableValues = {"MALE", "FEMALE", "OTHER"})
  private String gender;

  @UpperCamelCase
  @Schema(description = "Current job occupation or profession", example = "Software Engineer")
  private String occupation;

  @UpperCamelCase
  @Schema(description = "Educational qualification level", example = "Master Degree")
  private String education;

  @Schema(description = "Nationality or country of citizenship", example = "Italian")
  private String nationality;

  @Schema(
      description = "Social Security Number or national tax identifier (e.g., Codice Fiscale)",
      example = "RSSMRA90A01H501Z")
  private String ssn;

  @Schema(
      description = "Custom dynamic key-value attributes stored as a JSON object",
      example = "{\"department\": \"Engineering\", \"badge\": \"EMP-1024\"}")
  private Map<String, Object> attributes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Multi-Factor Authentication (MFA) settings and registered authentication methods")
  private MFASetting mfaSettings;
}
