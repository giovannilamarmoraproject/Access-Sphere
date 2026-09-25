package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
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
 * Raccoglie e incapsula tutte le proprieta applicative (prefisso 'app.admin-user') e i bean di
 * sicurezza, eliminando la necessita di iniettare singole annotazioni @Value nei vari servizi del
 * sistema.
 */
@Getter
@Setter
@Primary
@Service
public class AdminUserConfig {

  public final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value(value = "${app.admin-user.username:tech_admin}")
  public String adminUsername;

  @Value(value = "${app.admin-user.password:tech_password}")
  public String adminPassword;

  @Value(value = "${app.admin-user.client-id:ACCESS-SPHERE-AUTH}")
  public String adminClientId;

  @Value(value = "${app.admin-user.email:admin@accesssphere.local}")
  public String adminEmail;

  @Value(value = "${app.admin-user.jwt-secret:${app.jwt-secret:}}")
  public String adminJwtSecret;

  @Autowired public TokenService tokenService;
  @Autowired public OAuth2Authorization oAuth2Authorization;

  public static final String DEFAULT_RESET_PASSWORD_TEMPLATE_UUID =
      "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95";
  public static final String LEGACY_RESET_PASSWORD_TEMPLATE_IDENTIFIER =
      "ACCESS_SPHERE_CODE_RESET_PASSWORD";
}
