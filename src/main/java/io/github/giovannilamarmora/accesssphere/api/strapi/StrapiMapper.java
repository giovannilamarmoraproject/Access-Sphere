package io.github.giovannilamarmora.accesssphere.api.strapi;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.OAuthStrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Component
public class StrapiMapper {

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential mapFromStrapiResponseToClientCredential(
      StrapiResponse strapiResponse) {
    OAuthStrapiClient strapiClient = strapiResponse.getData().getFirst().getAttributes();
    return new ClientCredential(
        strapiClient.getClientId(),
        strapiClient.getExternalClientId(),
        strapiClient.getClientSecret(),
        ObjectUtils.isEmpty(strapiClient.getScopes())
            ? null
            : List.of(strapiClient.getScopes().split(" ")),
        ObjectUtils.isEmpty(strapiClient.getRedirectUri())
            ? null
            : List.of(strapiClient.getRedirectUri().split(" ")),
        ObjectUtils.isEmpty(strapiClient.getAccessType())
            ? null
            : AccessType.valueOf(strapiClient.getAccessType()),
        ObjectUtils.isEmpty(strapiClient.getType())
            ? null
            : OAuthType.valueOf(strapiClient.getType()),
        ObjectUtils.isEmpty(strapiClient.getTokenType())
            ? null
            : TokenType.valueOf(strapiClient.getTokenType()),
        strapiClient.getJwtSecret(),
        strapiClient.getJwtExpiration(),
        strapiClient.getJweSecret(),
        strapiClient.getJweExpiration(),
        strapiClient.getRegistrationToken());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static StrapiUser mapFromUserToStrapiUser(User user) {
    return new StrapiUser(
        user.getId(),
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        true,
        false,
        user.getUsername(),
        user.getPassword(),
        user.getRole(),
        user.getProfilePhoto(),
        user.getPhoneNumber(),
        user.getBirthDate(),
        user.getGender(),
        user.getOccupation(),
        user.getEducation(),
        user.getNationality(),
        user.getSsn(),
        user.getTokenReset(),
        user.getAttributes());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User mapFromStrapiUserToUser(StrapiUser user) {
    return new User(
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        user.getUsername(),
        null,
        user.getRole(),
        user.getProfilePhoto(),
        null,
        user.getPhoneNumber(),
        user.getBirthDate(),
        user.getGender(),
        user.getOccupation(),
        user.getEducation(),
        user.getNationality(),
        user.getSsn(),
        user.getTokenReset(),
        user.getAttributes(),
        null);
  }
}
