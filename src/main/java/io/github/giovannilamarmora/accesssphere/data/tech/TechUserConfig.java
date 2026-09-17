package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Configurazione centralizzata per l'utente tecnico IAM e per il client tecnico di sistema.
 * Raccoglie e incapsula tutte le proprieta applicative (prefisso 'app.tech-user') e i bean di sicurezza,
 * eliminando la necessita di iniettare singole annotazioni @Value nei vari servizi del sistema.
 */
@Getter
@Setter
@Primary
@Service
public class TechUserConfig {

  public final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value(value = "${app.tech-user.username:tech_admin}")
  public String tech_username;

  @Value(value = "${app.tech-user.password:tech_password}")
  public String tech_password;

  @Value(value = "${app.tech-user.client-id:ACCESS-SPHERE-TECH}")
  public String tech_client_id;

  @Value(value = "${app.tech-user.email:admin@accesssphere.local}")
  public String tech_email;

  @Value(value = "${app.tech-user.jwt-secret:${app.jwt-secret:}}")
  public String tech_jwt_secret;

  @Autowired public TokenService tokenService;
  @Autowired public AccessTokenData accessTokenData;

  public static final String TECH_ROLE_LOG = "The subject {} has Technical Roles";
}
