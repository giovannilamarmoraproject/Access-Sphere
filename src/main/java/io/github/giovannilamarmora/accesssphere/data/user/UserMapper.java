package io.github.giovannilamarmora.accesssphere.data.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class UserMapper {

  /**
   * Converte il DTO {@link User} nella corrispondente entità di persistenza {@link UserEntity}.
   * Serializza le liste di ruoli in stringa separata da spazi e converte attributi e impostazioni
   * MFA in JSON.
   *
   * @param user Oggetto di trasferimento dati (DTO) dell'utente.
   * @return L'entità {@link UserEntity} mappata e pronta per il salvataggio su database.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static UserEntity toEntity(User user) {
    UserEntity userEntity = new UserEntity();
    BeanUtils.copyProperties(user, userEntity, "roles");
    if (!ObjectUtils.isEmpty(user.getRoles()))
      userEntity.setRoles(
          user.getRoles().stream()
              .map(io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO::getRole)
              .filter(java.util.Objects::nonNull)
              .collect(java.util.stream.Collectors.joining(" ")));

    if (!ObjectToolkit.isNullOrEmpty(user.getAttributes()))
      userEntity.setAttributes(Mapper.writeObjectToString(user.getAttributes()));

    if (!ObjectToolkit.isNullOrEmpty(user.getMfaSettings()))
      userEntity.setMfaSettings(Mapper.writeObjectToString(user.getMfaSettings()));

    return userEntity;
  }

  /**
   * Converte un'entità di persistenza {@link UserEntity} nel DTO {@link User}. Deserializza la
   * stringa dei ruoli in una lista e riconverte le stringhe JSON di attributi e MFA nei rispettivi
   * oggetti.
   *
   * @param userEntity Entità del database da convertire.
   * @return Il DTO {@link User} popolato per i layer di servizio e controller.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User toDTO(UserEntity userEntity) {
    User user = new User();
    BeanUtils.copyProperties(userEntity, user, "roles");
    user.setPassword(null);
    if (!ObjectUtils.isEmpty(userEntity.getRoles()))
      user.setRoles(
          Arrays.stream(userEntity.getRoles().split(" "))
              .filter(r -> !r.isBlank())
              .map(
                  r ->
                      new io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO(
                          null, r))
              .toList());

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getAttributes()))
      user.setAttributes(Mapper.readObject(userEntity.getAttributes(), new TypeReference<>() {}));

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getMfaSettings()))
      user.setMfaSettings(Mapper.readObject(userEntity.getMfaSettings(), new TypeReference<>() {}));
    return user;
  }

  /**
   * Converte una lista di entità {@link UserEntity} in una lista di DTO {@link User}.
   *
   * @param userEntities Lista di entità recuperate dal database.
   * @return Lista di DTO mappati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<User> toDTOList(List<UserEntity> userEntities) {
    return userEntities.stream().map(UserMapper::toDTO).toList();
  }

  /**
   * Aggiorna lo stato di un'entità esistente {@link UserEntity} con i nuovi valori presenti nel DTO
   * {@link User}. Mantiene invariati i dati strutturali o di audit e serializza opportunamente
   * attributi e impostazioni MFA.
   *
   * @param existingUser Entità persistita esistente da aggiornare.
   * @param user DTO contenente i valori aggiornati forniti dall'utente o dal sistema.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static void updateEntity(UserEntity existingUser, User user) {
    // Aggiorna i campi dell'entita' esistente con i valori del User
    existingUser.setIdentifier(user.getIdentifier());
    existingUser.setName(user.getName());
    existingUser.setSurname(user.getSurname());
    existingUser.setEmail(user.getEmail());
    existingUser.setUsername(user.getUsername());
    existingUser.setRoles(
        ObjectUtils.isEmpty(user.getRoles())
            ? null
            : user.getRoles().stream()
                .map(io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO::getRole)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" ")));
    existingUser.setProfilePhoto(user.getProfilePhoto());
    existingUser.setPhoneNumber(user.getPhoneNumber());
    existingUser.setBirthDate(user.getBirthDate());
    existingUser.setGender(user.getGender());
    existingUser.setOccupation(user.getOccupation());
    existingUser.setEducation(user.getEducation());
    existingUser.setNationality(user.getNationality());
    existingUser.setSsn(user.getSsn());
    existingUser.setTokenReset(user.getTokenReset());
    existingUser.setAttributes(
        ObjectUtils.isEmpty(user.getAttributes())
            ? null
            : Mapper.writeObjectToString(user.getAttributes()));
    existingUser.setMfaSettings(
        ObjectUtils.isEmpty(user.getMfaSettings())
            ? null
            : Mapper.writeObjectToString(user.getMfaSettings()));
  }

  /**
   * Crea un'istanza di {@link User} per un utente tecnico autenticato, associandovi i ruoli
   * definiti nel client.
   *
   * @param username Username dell'utente tecnico.
   * @param oAuth2Client Credenziali del client OAuth che definiscono gli AppRole concessi.
   * @return DTO {@link User} configurato per l'utente tecnico.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User getTechUser(String username, OAuth2Client oAuth2Client) {
    User user = new User();
    user.setUsername(username);
    user.setRoles(
        oAuth2Client.getAppRoles() == null
            ? null
            : oAuth2Client.getAppRoles().stream()
                .map(
                    ar ->
                        new io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO(
                            oAuth2Client.getClientId(), ar.getRole(), ar.getType(), null, null))
                .toList());
    return user;
  }

  /**
   * Ricostruisce un DTO {@link User} a partire dai claim presenti nei dati del JWT decifrato.
   * Utilizzato nei flussi di introspezione token e autenticazione stateless.
   *
   * @param jwtData Dati estratti dal token JWT.
   * @return DTO {@link User} popolato con i claim del token.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User getTechUser(JWTData jwtData) {
    User user = new User();
    user.setIdentifier(jwtData.getIdentifier());
    user.setUsername(jwtData.getSub());
    user.setName(jwtData.getGiven_name());
    user.setSurname(jwtData.getFamily_name());
    user.setEmail(jwtData.getEmail());
    user.setRoles(
        jwtData.getRoles() != null
            ? jwtData.getRoles().stream()
                .map(
                    r ->
                        new io.github.giovannilamarmora.accesssphere.roles.dto.UserClientRoleDTO(
                            jwtData.getClient_id(), r))
                .toList()
            : null);
    user.setAttributes(jwtData.getAttributes());
    return user;
  }
}
