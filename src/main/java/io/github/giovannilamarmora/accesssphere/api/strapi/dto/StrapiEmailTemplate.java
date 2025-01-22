package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrapiEmailTemplate {

  private String identifier;
  private String name;
  private String template;
  private String locale;
  private String description;
  private Map<String, String> params;
  private String subject;
  private String createdAt;
  private String updatedAt;
  private String publishedAt;
  private Double version;
}
