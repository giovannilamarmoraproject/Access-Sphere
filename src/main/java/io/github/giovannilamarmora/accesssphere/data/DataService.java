package io.github.giovannilamarmora.accesssphere.data;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class DataService {

  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private StrapiService strapiService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByEmail(String email) {
    if (isStrapiEnabled) {
      LOG.info("Strapi is active");
      return strapiService
          .getUserByEmail(email)
          .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
          .onErrorResume(
              throwable -> {
                if (throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                  LOG.info(
                      "Error on strapi, finding user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByEmail(email);
                  DataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(email);
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> registerUser(User user) {
    // Se l'utente non ha password?
    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

    if (isStrapiEnabled) {
      return strapiService
          .registerUserToStrapi(user)
          .map(
              strapiResponseRes -> {
                if (ObjectUtils.isEmpty(strapiResponseRes.getBody())) {
                  LOG.error("Strapi response on user is null");
                  throw new OAuthException(
                      ExceptionMap.ERR_STRAPI_400, ExceptionMap.ERR_STRAPI_400.getMessage());
                }
                userEntity.setStrapiId(strapiResponseRes.getBody().getUser().getId());
                // Registro l'utente a prescindere che strapi funzioni o meno
                UserEntity userSaved = userDataService.saveAndFlush(userEntity);
                return UserMapper.mapUserEntityToUser(userSaved);
              })
          .onErrorResume(
              throwable -> {
                LOG.info(
                    "Error on strapi, register user into database, message is {}",
                    throwable.getMessage());
                UserEntity userSaved = userDataService.saveAndFlush(userEntity);
                DataValidator.validateUser(userEntity);
                return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
              })
          .doOnSuccess(user1 -> LOG.info("User {} saved", user1.getUsername()));
    }
    UserEntity userSaved = userDataService.saveAndFlush(userEntity);
    DataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userSaved));
  }
}
