/*package io.github.giovannilamarmora.accesssphere.token;

import com.nimbusds.jose.JOSEException;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Logged
@RestController
@RequestMapping("/v1/oAuth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "API for Authentication")
public class TokenControllerImpl implements TokenController {

  @Autowired private AuthService authService;

  public ResponseEntity<Response> userInfo(String authToken) throws UtilsException, JOSEException {
    return authService.userInfo(authToken);
  }

  public ResponseEntity<Response> refreshToken(RefreshToken authToken) throws JOSEException {
    return authService.refreshToken(authToken.getAccessToken());
  }
}
*/