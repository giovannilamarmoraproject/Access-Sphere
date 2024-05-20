package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCamelCase;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrapiAddress {

  private Long id;

  @NotNull(message = "Street is required")
  @NotBlank(message = "Street is required")
  @UpperCamelCase
  private String street;

  @NotNull(message = "City is required")
  @NotBlank(message = "City is required")
  @UpperCamelCase
  private String city;

  @NotNull(message = "State is required")
  @NotBlank(message = "State is required")
  @UpperCamelCase
  private String state;

  @NotNull(message = "Country is required")
  @NotBlank(message = "Country is required")
  @UpperCase
  // TODO: Usare libreria
  private String country;

  @NotNull(message = "Zip Code is required")
  @NotBlank(message = "Zip Code is required")
  @UpperCase
  private String zipCode;

  private Boolean primary = true;
}
