package io.github.giovannilamarmora.accesssphere.api.strapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrapiResponse {
  private List<StrapiData> data;
  private String jwt;
  private String refresh_token;
  private StrapiUser user;

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StrapiData {
    private String id;
    private OAuthStrapiClient attributes;
  }

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Meta {
    private Pagination pagination;
  }

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Pagination {
    private Integer page;
    private Integer pageSize;
    private Integer pageCount;
    private Integer total;
  }
}
