package io.github.giovannilamarmora.accesssphere.api.strapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.*;
import io.github.giovannilamarmora.accesssphere.client.model.AccessType;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.TokenType;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetting;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.utilities.CryptoUtils;
import io.github.giovannilamarmora.accesssphere.webhooks.dto.Webhook;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
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
    List<String> scopes =
        ObjectToolkit.isNullOrEmpty(strapiClient.getScopes())
            ? null
            : List.of(strapiClient.getScopes().split("\\s+"));
    return new ClientCredential(
        strapiClient.getClientId(),
        strapiClient.getExternalClientId(),
        strapiClient.getClientSecret(),
        scopes,
        ObjectUtils.isEmpty(strapiClient.getRedirectUri()) ? null : strapiClient.getRedirectUri(),
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
        strapiClient.getApp_roles(),
        strapiClient.getId_token(),
        strapiClient.getAccess_token(),
        strapiClient.getStrapi_token(),
        strapiClient.getAuthorize_redirect_status(),
        strapiClient.getMfa_enabled(),
        mapFromStrapiWebhooksToWebhooks(strapiClient.getWebhooks()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static StrapiUser mapFromUserToStrapiUser(User user, ClientCredential clientCredential) {
    return new StrapiUser(
        user.getId(),
        user.getIdentifier(),
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        ObjectToolkit.getOrDefault(user.getConfirmed(), true),
        ObjectToolkit.getOrDefault(user.getBlocked(), false),
        user.getUsername(),
        user.getPassword(),
        ObjectUtils.isEmpty(clientCredential)
            ? null
            : clientCredential.getAppRoles().stream()
                .filter(appRole -> user.getRoles().contains(appRole.getRole()))
                .toList(),
        user.getProfilePhoto(),
        user.getPhoneNumber(),
        user.getBirthDate(),
        user.getGender(),
        user.getOccupation(),
        user.getEducation(),
        user.getNationality(),
        user.getSsn(),
        user.getTokenReset(),
        user.getAttributes(),
        mapMFASettingsIntoStrapiMFASettings(user.getMfaSettings()));
  }

  public static StrapiMFASetting mapMFASettingsIntoStrapiMFASettings(MFASetting mfaSetting) {
    if (ObjectToolkit.isNullOrEmpty(mfaSetting)) return null;
    StrapiMFASetting strapiMFASetting = new StrapiMFASetting();
    BeanUtils.copyProperties(mfaSetting, strapiMFASetting);
    strapiMFASetting.setMfaMethods(
        mfaSetting.getMfaMethods().stream()
            .map(
                mfaMethod -> {
                  StrapiMFAMethod method = new StrapiMFAMethod();
                  BeanUtils.copyProperties(mfaMethod, method);
                  method.setSecretKey(CryptoUtils.encrypt(mfaMethod.getSecretKey()));
                  return method;
                })
            .toList());
    return strapiMFASetting;
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
        user.getPhoneNumber(),
        user.getBirthdate(),
        user.getGender(),
        user.getOccupation(),
        user.getEducation(),
        user.getNationality(),
        user.getSsn(),
        user.getTokenReset(),
        user.getConfirmed(),
        user.getBlocked(),
        user.getAttributes(),
        mapStrapiMFASettingsIntoMFASettings(user.getMfa_settings()));
  }

  public static MFASetting mapStrapiMFASettingsIntoMFASettings(StrapiMFASetting strapiMFASetting) {
    if (ObjectToolkit.isNullOrEmpty(strapiMFASetting)) return null;
    MFASetting mfaSetting = new MFASetting();
    BeanUtils.copyProperties(strapiMFASetting, mfaSetting);
    mfaSetting.setMfaMethods(
        strapiMFASetting.getMfaMethods().stream()
            .map(
                strapiMFAMethod -> {
                  MFAMethod method = new MFAMethod();
                  if (ObjectToolkit.isNullOrEmpty(strapiMFAMethod)) return null;
                  BeanUtils.copyProperties(strapiMFAMethod, method);
                  method.setSecretKey(CryptoUtils.decrypt(strapiMFAMethod.getSecretKey()));
                  return method;
                })
            .toList());
    return mfaSetting;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<User> mapFromStrapiUsersToUsers(List<StrapiUser> user) {
    return user.stream().map(StrapiMapper::mapFromStrapiUserToUser).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<String> getAppRoles(List<AppRole> appRoles) {
    if (ObjectUtils.isEmpty(appRoles)) return null;
    return appRoles.stream().map(AppRole::getRole).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<Webhook> mapFromStrapiWebhooksToWebhooks(List<StrapiWebhook> strapiWebhooks) {
    return strapiWebhooks.stream().map(StrapiMapper::mapStrapiWebhookIntoWebhook).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static Webhook mapStrapiWebhookIntoWebhook(StrapiWebhook strapiWebhook) {
    if (ObjectToolkit.isNullOrEmpty(strapiWebhook)) return null;
    Webhook webhook = new Webhook();
    BeanUtils.copyProperties(strapiWebhook, webhook);
    return webhook;
  }
}
