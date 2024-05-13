package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.data.address.model.Address;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.utils.jsonSerialize.LowerCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrapiUser {
    @UpperCamelCase
    private String name;
    @UpperCamelCase private String surname;

    @NotNull(message = "Email is required")
    @NotBlank(message = "Email is required")
    @LowerCase
    private String email;
    private Boolean confirmed;
    private Boolean blocked;

    @NotNull(message = "Username is required")
    @NotBlank(message = "Username is required")
    @LowerCase
    private String username;

    private String password;

    private UserRole role = UserRole.USER;

    private String profilePhoto;

    private String phoneNumber;

    private LocalDate birthDate;

    @UpperCase
    private String gender;

    @UpperCamelCase private String occupation;

    @UpperCamelCase private String education;

    @UpperCamelCase private String nationality;

    private String ssn; // Social Security Number

    private String tokenReset;

    private Map<String, Object> attributes;

}
