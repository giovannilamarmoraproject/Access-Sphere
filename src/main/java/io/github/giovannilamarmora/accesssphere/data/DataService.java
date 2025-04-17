package io.github.giovannilamarmora.accesssphere.data;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@Deprecated
public class DataService {

  @Getter
  @Setter
  @Value(value = "${rest.client.strapi.active}")
  private Boolean isStrapiEnabled;

  @Autowired private TechUserService techUserService;

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private UserDataService userDataService;
  @Autowired private StrapiService strapiService;
  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenService accessTokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<User> getUserByEmail(String email) {
    if (isStrapiEnabled) {
      LOG.debug("Strapi is enabled, finding user, using email {}", email);
      return strapiService
          .getUserByEmail(email)
          .flatMap(strapiUser -> Mono.just(StrapiMapper.mapFromStrapiUserToUser(strapiUser)))
          .onErrorResume(
              throwable -> {
                if (!throwable
                    .getMessage()
                    .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                  LOG.info(
                      "Error on strapi, finding user into database, message is {}",
                      throwable.getMessage());
                  UserEntity userEntity = userDataService.findUserEntityByEmail(email);
                  UserDataValidator.validateUser(userEntity);
                  return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
                }
                return Mono.error(throwable);
              });
    }
    UserEntity userEntity = userDataService.findUserEntityByEmail(email);
    UserDataValidator.validateUser(userEntity);
    return Mono.just(UserMapper.mapUserEntityToUser(userEntity));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getUsers() {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting all users in database");
    List<UserEntity> userEntities = userDataService.findAll();
    return Mono.just(UserMapper.mapUserEntitiesToUsers(userEntities));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<User>> getStrapiUsers(String strapi_bearer) {
    LOG.info("\uD83E\uDD37\u200D♂\uFE0F Getting all users in strapi");

    return strapiService
        .getUsers(strapi_bearer)
        .flatMap(strapiUsers -> Mono.just(StrapiMapper.mapFromStrapiUsersToUsers(strapiUsers)));
  }

  public void saveUserIntoDatabase(User user) {
    UserEntity userEntity = UserMapper.mapUserToUserEntity(user);
    saveUserEntityIntoDatabase(userEntity);
  }

  public void updateUserIntoDatabase(User user) {
    UserEntity userSaved = userDataService.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(userSaved)) {
      // Aggiornare i campi dell'entità esistente con i valori del ClientCredential
      UserMapper.updateUserEntityFields(userSaved, user);
      saveUserEntityIntoDatabase(userSaved);
    } else {
      // Se il client non esiste, possiamo decidere di aggiungerlo o lanciare un'eccezione
      throw new IllegalArgumentException("User not found: " + userSaved.getIdentifier());
    }
    UserMapper.mapUserEntityToUser(userSaved);
  }

  @Transactional
  private User saveUserEntityIntoDatabase(UserEntity userEntity) {
    UserEntity userSaved = userDataService.saveAndFlush(userEntity);
    userSaved.setPassword(null);
    UserDataValidator.validateUser(userSaved);
    return UserMapper.mapUserEntityToUser(userSaved);
  }

  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserFromDatabase(User user) {
    // Trovare l'entità esistente nel database
    UserEntity existingUser = userDataService.findUserEntityByIdentifier(user.getIdentifier());
    if (!ObjectUtils.isEmpty(existingUser)) {
      userDataService.delete(existingUser);
    }
  }
}
