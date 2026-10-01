package io.github.giovannilamarmora.accesssphere.user;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.user.dto.UserDataResponse;
import io.github.giovannilamarmora.accesssphere.user.dto.UserRequest;
import io.github.giovannilamarmora.accesssphere.user.dto.UserUpdateRequest;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public static List<UserDataResponse> toResponseList(List<User> users) {
    if (ObjectToolkit.isNullOrEmpty(users)) return List.of();
    return users.stream().map(UserMapper::toResponse).toList();
  }

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
   * alias che delega a {@link #toModelList(List)} per garantire uniformità di nomenclatura tra
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
    User user = new User();
    BeanUtils.copyProperties(userEntity, user);

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getAttributes()))
      user.setAttributes(Mapper.readObject(userEntity.getAttributes(), new TypeReference<>() {}));

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getMfaSettings()))
      user.setMfaSettings(Mapper.readObject(userEntity.getMfaSettings(), new TypeReference<>() {}));
    return user;
  }

  public static User toModel(UserRequest userRequest) {
    User user = new User();
    BeanUtils.copyProperties(userRequest, user);
    user.setRoles(UserClientRoleMapper.requestToModelList(userRequest.getRoles()));
    return user;
  }

  public static User toModel(UserUpdateRequest userUpdateRequest) {
    User user = new User();
    BeanUtils.copyProperties(userUpdateRequest, user);
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
    UserEntity userEntity = new UserEntity();
    BeanUtils.copyProperties(user, userEntity);

    if (!ObjectToolkit.isNullOrEmpty(user.getAttributes()))
      userEntity.setAttributes(Mapper.writeObjectToString(user.getAttributes()));

    if (!ObjectToolkit.isNullOrEmpty(user.getMfaSettings()))
      userEntity.setMfaSettings(Mapper.writeObjectToString(user.getMfaSettings()));

    return userEntity;
  }

  /**
   * Aggiorna il modello {@link User} da persistere preservando i campi critici, di sicurezza e di
   * audit dell'utente esistente (username, password, credenziali, ruoli, stato account, token reset
   * e MFA).
   *
   * @param existingUser Utente persistito esistente recuperato dal database.
   * @param userToUpdate Modello contenente i valori anagrafici aggiornati.
   * @return Il modello {@link User} con i dati anagrafici aggiornati e i campi di sicurezza
   *     preservati.
   */
  public static User updateUser(User existingUser, User userToUpdate) {
    userToUpdate.setId(existingUser.getId());
    userToUpdate.setIdentifier(existingUser.getIdentifier());
    userToUpdate.setUsername(existingUser.getUsername());
    userToUpdate.setPassword(existingUser.getPassword());
    userToUpdate.setConfirmed(existingUser.getConfirmed());
    userToUpdate.setBlocked(existingUser.getBlocked());
    userToUpdate.setRoles(existingUser.getRoles());
    userToUpdate.setTokenReset(existingUser.getTokenReset());

    if (ObjectToolkit.isNullOrEmpty(userToUpdate.getAttributes())) {
      userToUpdate.setAttributes(existingUser.getAttributes());
    }

    if (ObjectToolkit.isNullOrEmpty(userToUpdate.getMfaSettings())) {
      userToUpdate.setMfaSettings(existingUser.getMfaSettings());
    }

    return userToUpdate;
  }
}
