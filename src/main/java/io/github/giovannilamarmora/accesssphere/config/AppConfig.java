package io.github.giovannilamarmora.accesssphere.config;

import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = "io.github.giovannilamarmora.utils")
@Configuration
@EnableScheduling
@EnableCaching
@OpenAPIDefinition(
    info =
        @Info(
            title = "Access Sphere Swagger",
            version = "2.0.0",
            description = "Enterprise-ready IAM & OAuth 2.0 / OIDC Authorization Server"),
    security = {@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)},
    servers = {
      @Server(
          url = "https://access-sphere.giovannilamarmora.com",
          description = "Production Server URL"),
      @Server(url = "http://localhost:8081", description = "Local Dev Server (8081)"),
      @Server(url = "http://localhost:8080", description = "Docker / Container Server (8080)")
    },
    tags = {
      @Tag(name = OpenAPI.Tag.OAUTH, description = OpenAPI.Description.OAUTH),
      @Tag(name = OpenAPI.Tag.OIDC, description = OpenAPI.Description.OIDC),
      @Tag(name = OpenAPI.Tag.USERS, description = OpenAPI.Description.USERS),
      @Tag(name = OpenAPI.Tag.ACCOUNT, description = OpenAPI.Description.ACCOUNT),
      @Tag(name = OpenAPI.Tag.ROLES, description = OpenAPI.Description.ROLES),
      @Tag(name = OpenAPI.Tag.CLIENTS, description = OpenAPI.Description.CLIENTS),
      @Tag(name = OpenAPI.Tag.MFA, description = OpenAPI.Description.MFA),
      @Tag(name = OpenAPI.Tag.EMAIL_TEMPLATE, description = OpenAPI.Description.EMAIL_TEMPLATE),
      @Tag(name = OpenAPI.Tag.SETTINGS, description = OpenAPI.Description.SETTINGS)
    })
@SecurityScheme(
    type = SecuritySchemeType.APIKEY,
    name = HttpHeaders.AUTHORIZATION,
    in = SecuritySchemeIn.HEADER)
public class AppConfig {

  // @Bean
  // public OpenApiCustomizer applyStandardOpenAPIModifications() {
  //  return openApi -> {
  //    Paths paths = new Paths();
  //    openApi.getPaths().entrySet().stream()
  //        .sorted(Map.Entry.comparingByKey())
  //        .forEach(
  //            entry ->
  //                paths.addPathItem(
  //                    entry.getKey(),
  //                    OpenAPIConfig.addJSONExamplesOnResource(entry.getValue(),
  // AppConfig.class)));
  //    openApi.setPaths(paths);
  //  };
  // }

  @Bean
  public SessionID sessionID() {
    return new SessionID();
  }

  @Bean
  public OAuth2Authorization accessTokenData() {
    return new OAuth2Authorization();
  }
}
