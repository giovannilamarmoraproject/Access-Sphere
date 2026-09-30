package io.github.giovannilamarmora.accesssphere.user.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio primario che rappresenta l'entita Utente IAM con anagrafica,
 * credenziali, ruoli contestualizzati per Client OAuth2 e configurazioni di sicurezza.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Primary IAM User domain model with credentials, profile, and client-scoped roles")
public class User extends GenericDTO {

  @Schema(
      description = "Unique UUID identifier of the IAM user",
      example = "d18124b1-ba15-4b51-a344-78e5a6c0a205")
  private String identifier;

  @JsonAlias({"firstName", "first_name"})
  @UpperCamelCase
  @Schema(description = "First name of the user", example = "John")
  private String name;

  @JsonAlias({"lastName", "last_name"})
  @UpperCamelCase
  @Schema(description = "Last name / family name of the user", example = "Doe")
  private String surname;

  @NotNull(message = "Email is required")
  @NotBlank(message = "Email is required")
  @LowerCase
  @Schema(description = "Primary email address of the user", example = "john.doe@example.com")
  private String email;

  @NotNull(message = "Username is required")
  @NotBlank(message = "Username is required")
  @LowerCase
  @Schema(description = "Unique username used for login and identification", example = "johndoe")
  private String username;

  @Schema(
      description = "User password (hashed on storage, ignored or null in responses)",
      example = "P@ssw0rd2026!")
  private String password;

  /** Ruoli contestualizzati per Client OAuth2 con tipologia e permessi granulari aggregati. */
  @JsonAlias({"clientRoles", "client_roles"})
  @Schema(description = "Contextualized application roles assigned to the user across OAuth2 clients")
  private List<UserClientRole> roles;

  @JsonAlias({"profile", "profile_photo"})
  @Schema(
      description = "Profile avatar URL or Base64 data URI",
      example = "https://bootdey.com/img/Content/avatar/avatar7.png")
  private String profilePhoto;

  @JsonAlias({"phone", "phone_number"})
  @Schema(description = "Contact phone number including international prefix", example = "+39 3401234567")
  private String phoneNumber;

  @JsonAlias({"birthdate", "birth_date"})
  @Schema(description = "Date of birth of the user (ISO-8601 format YYYY-MM-DD)", example = "1990-01-15")
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
  private String ssn; // Social Security Number

  @Schema(
      description = "Password reset verification token (null unless reset process is active)",
      example = "null")
  private String tokenReset;

  @Schema(description = "Flag indicating whether the user email/account is confirmed", example = "true")
  private Boolean confirmed;

  @Schema(description = "Flag indicating whether the user account is locked/suspended", example = "false")
  private Boolean blocked;

  @Schema(
      description = "Custom dynamic key-value attributes stored as a JSON object",
      example = "{\"department\": \"Engineering\", \"badge\": \"EMP-1024\"}")
  private Map<String, Object> attributes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Multi-Factor Authentication (MFA) settings and registered authentication methods")
  private MFASetting mfaSettings;
}
