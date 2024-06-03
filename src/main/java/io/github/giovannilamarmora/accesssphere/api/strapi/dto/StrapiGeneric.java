package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrapiGeneric {

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime publishedAt;
}
