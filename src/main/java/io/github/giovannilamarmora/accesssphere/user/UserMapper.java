package io.github.giovannilamarmora.accesssphere.user;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.admin.dto.AddUserRequest;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class UserMapper {

  public static UserDataResponse toResponse(User user) {
    if (ObjectToolkit.isNullOrEmpty(user)) return null;
    UserDataResponse userDataResponse = new UserDataResponse();
    BeanUtils.copyProperties(user, userDataResponse);
    userDataResponse.setId(null);
    userDataResponse.setRoles(UserClientRoleMapper.toResponseList(user.getRoles()));
    return userDataResponse;
  }

  /**
   * Converte una lista di entità {@link UserEntity} in una lista di modelli {@link User}. Metodo
   * alias che delega a {@link #toDTOList(List)} per garantire uniformità di nomenclatura tra
   * mapper.
   *
   * @param userEntities Lista di entità dal database.
   * @return Lista di modelli {@link User}.
   */
  public static List<User> toModelList(List<UserEntity> userEntities) {
    if (ObjectToolkit.isNullOrEmpty(userEntities)) return List.of();
    return userEntities.stream().map(UserMapper::toModel).toList();
  }

  /**
   * Converte un'entità di persistenza {@link UserEntity} nel DTO {@link User}. Deserializza la
   * stringa dei ruoli in una lista e riconverte le stringhe JSON di attributi e MFA nei rispettivi
   * oggetti.
   *
   * @param userEntity Entità del database da convertire.
   * @return Il DTO {@link User} popolato per i layer di servizio e controller.
   */
  public static User toModel(UserEntity userEntity) {
    if (ObjectToolkit.isNullOrEmpty(userEntity)) return null;
    User user = new User();
    BeanUtils.copyProperties(userEntity, user);
    user.setId(null);
    user.setPassword(null);

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getAttributes()))
      user.setAttributes(Mapper.readObject(userEntity.getAttributes(), new TypeReference<>() {}));

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getMfaSettings()))
      user.setMfaSettings(Mapper.readObject(userEntity.getMfaSettings(), new TypeReference<>() {}));
    return user;
  }

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
    if (user == null) return null;
    UserEntity userEntity = new UserEntity();
    BeanUtils.copyProperties(user, userEntity);

    if (!ObjectToolkit.isNullOrEmpty(user.getAttributes()))
      userEntity.setAttributes(Mapper.writeObjectToString(user.getAttributes()));

    if (!ObjectToolkit.isNullOrEmpty(user.getMfaSettings()))
      userEntity.setMfaSettings(Mapper.writeObjectToString(user.getMfaSettings()));

    return userEntity;
  }

  /**
   * Converte una lista di entità {@link UserEntity} in una lista di DTO {@link User}.
   *
   * @param userEntities Lista di entità recuperate dal database.
   * @return Lista di DTO mappati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<User> toDTOList(List<UserEntity> userEntities) {
    if (userEntities == null) return List.of();
    return userEntities.stream().map(UserMapper::toModel).toList();
  }

  public static User toModel(AddUserRequest addUserRequest) {
    if (addUserRequest == null) {
      return null;
    }
    User user = new User();
    BeanUtils.copyProperties(addUserRequest, user, "roles");
    if (addUserRequest.getRoles() != null) {
      user.setRoles(
          addUserRequest.getRoles().stream()
              .map(r -> new UserClientRole(r.getClientId(), r.getRole()))
              .toList());
    }
    return user;
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
}
