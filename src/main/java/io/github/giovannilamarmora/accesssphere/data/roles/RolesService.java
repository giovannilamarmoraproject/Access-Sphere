package io.github.giovannilamarmora.accesssphere.data.roles;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RolesService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private ClientService clientService;
  @Autowired private StrapiService strapiService;
  @Autowired private TechUserService techUserService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> changeRoles(String identifier, UserRoles roles) {
    LOG.info(
        "\uD83E\uDD37\u200D♂\uFE0F Changing user roles process started, identifier: {}",
        identifier);
    if (!techUserService.isTechUser()) {
      LOG.error("Only a tech user can change the user roles {}", identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }

    Mono<List<ClientCredential>> getClients = clientService.getClientCredentials();

    return getClients
        .flatMap(
            clientCredentials -> {
              List<AppRole> appRoles =
                  clientCredentials.stream()
                      .flatMap(clientCredential -> clientCredential.getAppRoles().stream())
                      .filter(appRole -> roles.getRoles().contains(appRole.getRole()))
                      .toList();
              return strapiService
                  .getUserByIdentifier(identifier)
                  .flatMap(
                      strapiUser -> {
                        strapiUser.setApp_roles(appRoles);
                        return strapiService
                            .updateUser(strapiUser)
                            .flatMap(
                                strapiUser1 -> {
                                  User userRes = StrapiMapper.mapFromStrapiUserToUser(strapiUser1);
                                  Response response =
                                      new Response(
                                          HttpStatus.OK.value(),
                                          "User roles for "
                                              + userRes.getUsername()
                                              + " successfully changed!",
                                          TraceUtils.getSpanID(),
                                          userRes);
                                  return Mono.just(ResponseEntity.ok(response));
                                });
                      });
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "\uD83E\uDD37\u200D♂\uFE0F Changing user roles process ended, identifier: {}",
                    identifier));
  }
}
