package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private StrapiService strapiService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> register(User user) throws UtilsException {
    LOG.info(
        "Registration process started, username: {}, email: {}",
        user.getUsername(),
        user.getEmail());
    user.setRole(UserRole.USER);
    // user.setPassword(new String(Base64.getDecoder().decode(user.getPassword())));

    if (!Utils.checkCharacterAndRegexValid(user.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("Invalid regex for field password for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, ExceptionMap.ERR_USER_400.getMessage());
    }

    if (!Utils.checkCharacterAndRegexValid(user.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("Invalid regex for field email for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, ExceptionMap.ERR_USER_400.getMessage());
    }

    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

    return strapiService
        .registerUserToStrapi(user)
        .flatMap(
            strapiResponse ->
                userDataService
                    .save(userEntity)
                    .map(
                        userEn -> {
                          String message =
                              "User: " + user.getUsername() + " Successfully registered!";

                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  message,
                                  CorrelationIdUtils.getCorrelationId(),
                                  UserMapper.mapUserEntityToUser(userEn));

                          return ResponseEntity.ok(response);
                        }));
  }
}
