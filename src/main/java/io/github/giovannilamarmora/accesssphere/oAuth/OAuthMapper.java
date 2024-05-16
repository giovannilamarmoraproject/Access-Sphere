package io.github.giovannilamarmora.accesssphere.oAuth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthMapper {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthMapper.class);
}
