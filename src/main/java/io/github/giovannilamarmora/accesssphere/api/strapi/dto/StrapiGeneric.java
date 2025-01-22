package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrapiGeneric {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocalDateTime createdAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocalDateTime updatedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocalDateTime publishedAt;
}
