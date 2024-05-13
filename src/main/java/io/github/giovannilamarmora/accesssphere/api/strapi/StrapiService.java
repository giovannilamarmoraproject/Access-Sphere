package io.github.giovannilamarmora.accesssphere.api.strapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiError;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StrapiService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private StrapiClient strapiClient;

  public Mono<ResponseEntity<StrapiResponse>> registerUserToStrapi(User user) {
    StrapiUser strapiUser = StrapiMapper.mapFromUserToStrapiUser(user);
    return strapiClient
        .saveUser(strapiUser)
        .doOnError(
            throwable -> {
              String messageBody = throwable.getMessage().split("and body message ")[1];
              if (Utils.isInstanceOf(messageBody, new TypeReference<StrapiError>() {})) {
                StrapiError response;
                try {
                  response = Utils.mapper.readValue(messageBody, StrapiError.class);
                } catch (JsonProcessingException e) {
                  LOG.error(
                      "An error happen during read value from strapi, message is {}",
                      e.getMessage());
                  throw new OAuthException(
                      ExceptionMap.ERR_STRAPI_500, ExceptionMap.ERR_STRAPI_500.getMessage());
                }
                if (response
                    .getError()
                    .getMessage()
                    .equalsIgnoreCase("Email or Username are already taken")) {
                  LOG.error(
                      "An error happen during registration on strapi, message is {}",
                      response.getError().getMessage());
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_400, response.getError().getMessage());
                }
              }
            });
  }
}
