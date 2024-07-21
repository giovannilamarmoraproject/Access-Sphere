package io.github.giovannilamarmora.accesssphere.api.strapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.*;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.address.model.Address;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class StrapiMapper {

  private static final ObjectMapper mapper = MapperUtils.mapper().failOnEmptyBean().build();

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential mapFromStrapiResponseToClientCredential(
      StrapiResponse strapiResponse) {
    OAuthStrapiClient strapiClient =
        mapper.convertValue(
            strapiResponse.getData().getFirst().getAttributes(), OAuthStrapiClient.class);
    return mapFromOAuthStrapiClientToClientCredential(strapiClient);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<ClientCredential> mapFromStrapiResponseToClientCredentials(
      StrapiResponse strapiResponse) {
    List<StrapiResponse.StrapiData> strapiDataList = strapiResponse.getData();

    return strapiDataList.stream()
        .map(
            strapiData ->
                mapFromOAuthStrapiClientToClientCredential(
                    mapper.convertValue(strapiData.getAttributes(), OAuthStrapiClient.class)))
        .toList();
  }

  private static ClientCredential mapFromOAuthStrapiClientToClientCredential(
      OAuthStrapiClient strapiClient) {
    List<AppRole> defaultRole = getRoles(strapiClient.getDefault_role());
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
        strapiClient.getRegistrationToken(),
        ObjectUtils.isEmpty(defaultRole) ? null : defaultRole.getFirst(),
        getRoles(strapiClient.getApp_roles()),
        strapiClient.getId_token(),
        strapiClient.getAccess_token(),
        strapiClient.getStrapi_token());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  private static List<AppRole> getRoles(OAuthStrapiClient.Roles defaultRoles) {
    if (ObjectUtils.isEmpty(defaultRoles) || ObjectUtils.isEmpty(defaultRoles.getData()))
      return null;
    return defaultRoles.getData().stream()
        .map(
            strapiData ->
                new AppRole(
                    ObjectUtils.isEmpty(strapiData.getAttributes().getId())
                        ? strapiData.getId()
                        : strapiData.getAttributes().getId(),
                    strapiData.getAttributes().getRole()))
        .toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static StrapiUser mapFromUserToStrapiUser(User user, ClientCredential clientCredential) {
    return new StrapiUser(
        user.getId(),
        user.getIdentifier(),
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        true,
        false,
        user.getUsername(),
        user.getPassword(),
        fromAddressesToStrapiAddresses(user.getAddresses()),
        ObjectUtils.isEmpty(clientCredential) ? null : List.of(clientCredential.getDefaultRole()),
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
        user.getIdentifier(),
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        user.getUsername(),
        null,
        getAppRoles(user.getApp_roles()),
        user.getProfilePhoto(),
        fromStrapiAddressesToAddresses(user.getAddresses()),
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
  public static List<User> mapFromStrapiUsersToUsers(List<StrapiUser> user) {
    return user.stream().map(StrapiMapper::mapFromStrapiUserToUser).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  private static List<String> getAppRoles(List<AppRole> appRoles) {
    if (ObjectUtils.isEmpty(appRoles)) return null;
    return appRoles.stream().map(AppRole::getRole).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<StrapiAddress> fromAddressesToStrapiAddresses(List<Address> addresses) {
    if (ObjectUtils.isEmpty(addresses)) return null;
    return addresses.stream().map(StrapiMapper::fromAddressToStrapiAddress).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static StrapiAddress fromAddressToStrapiAddress(Address addresses) {
    return new StrapiAddress(
        addresses.getId(),
        addresses.getStreet(),
        addresses.getCity(),
        addresses.getState(),
        addresses.getCountry(),
        addresses.getZipCode(),
        addresses.getPrimary());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<Address> fromStrapiAddressesToAddresses(List<StrapiAddress> addresses) {
    if (ObjectUtils.isEmpty(addresses)) return null;
    return addresses.stream().map(StrapiMapper::fromStrapiAddressToAddress).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static Address fromStrapiAddressToAddress(StrapiAddress addresses) {
    return new Address(
        addresses.getStreet(),
        addresses.getCity(),
        addresses.getState(),
        addresses.getCountry(),
        addresses.getZipCode(),
        addresses.getPrimary());
  }
}
