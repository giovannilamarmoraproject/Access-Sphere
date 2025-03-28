package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TechUserConfig {

  public final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value(value = "${app.tech-user.username}")
  public String tech_username;

  @Value(value = "${app.tech-user.password}")
  public String tech_password;

  @Getter
  @Value(value = "${app.tech-user.client-id}")
  public String tech_client_id;

  @Getter
  @Value(value = "${app.tech-user.strapi}")
  public String tech_token;

  @Autowired public TokenService tokenService;
  @Autowired public AccessTokenData accessTokenData;

  public static final String TECH_ROLE_LOG = "The subject {} has Technical Roles";
}
