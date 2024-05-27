package io.github.giovannilamarmora.accesssphere.api.strapi;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.*;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.address.AddressMapper;
import io.github.giovannilamarmora.accesssphere.data.address.model.Address;
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
        strapiClient.getRegistrationToken(),
        getRoles(strapiClient.getDefault_roles()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  private static List<AppRole> getRoles(OAuthStrapiClient.DefaultRoles defaultRoles) {
    if (ObjectUtils.isEmpty(defaultRoles) || ObjectUtils.isEmpty(defaultRoles.getData()))
      return null;
    return defaultRoles.getData().stream()
        .map(
            strapiData ->
                new AppRole(
                    strapiData.getAttributes().getId(), strapiData.getAttributes().getRole()))
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
        ObjectUtils.isEmpty(clientCredential) ? null : clientCredential.getDefaultRoles(),
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
