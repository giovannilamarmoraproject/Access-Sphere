package io.github.giovannilamarmora.accesssphere.data.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiMapper;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
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

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static UserEntity mapUserToUserEntity(User user) {
    UserEntity userEntity = new UserEntity();
    BeanUtils.copyProperties(user, userEntity);
    if (!ObjectUtils.isEmpty(user.getRoles()))
      userEntity.setRoles(String.join(" ", user.getRoles()));

    if (!ObjectToolkit.isNullOrEmpty(user.getAttributes()))
      userEntity.setAttributes(Mapper.writeObjectToString(user.getAttributes()));

    return userEntity;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User mapUserEntityToUser(UserEntity userEntity) {
    User user = new User();
    BeanUtils.copyProperties(userEntity, user);
    if (!ObjectUtils.isEmpty(userEntity.getStrapiId())) user.setId(userEntity.getStrapiId());
    if (!ObjectUtils.isEmpty(userEntity.getRoles()))
      user.setRoles(Arrays.stream(userEntity.getRoles().split(" ")).toList());

    if (!ObjectToolkit.isNullOrEmpty(userEntity.getAttributes()))
      user.setAttributes(Mapper.readObject(userEntity.getAttributes(), new TypeReference<>() {}));
    return user;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<User> mapUserEntitiesToUsers(List<UserEntity> userEntities) {
    return userEntities.stream().map(UserMapper::mapUserEntityToUser).toList();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static void updateUserEntityFields(UserEntity existingUser, User user) {
    // Aggiorna i campi dell'entità esistente con i valori del User
    existingUser.setIdentifier(user.getIdentifier());
    existingUser.setName(user.getName());
    existingUser.setSurname(user.getSurname());
    existingUser.setEmail(user.getEmail());
    existingUser.setUsername(user.getUsername());
    existingUser.setRoles(
        ObjectUtils.isEmpty(user.getRoles()) ? null : Joiner.on(" ").join(user.getRoles()));
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
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User getTechUser(String username, ClientCredential clientCredential) {
    User user = new User();
    user.setUsername(username);
    user.setRoles(StrapiMapper.getAppRoles(clientCredential.getAppRoles()));
    return user;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User getTechUser(JWTData jwtData) {
    User user = new User();
    user.setIdentifier(jwtData.getIdentifier());
    user.setUsername(jwtData.getSub());
    user.setName(jwtData.getGiven_name());
    user.setSurname(jwtData.getFamily_name());
    user.setEmail(jwtData.getEmail());
    user.setRoles(jwtData.getRoles());
    user.setProfilePhoto(jwtData.getPicture());
    user.setAttributes(jwtData.getAttributes());
    return user;
  }
}
