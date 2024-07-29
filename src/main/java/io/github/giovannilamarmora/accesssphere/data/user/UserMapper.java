package io.github.giovannilamarmora.accesssphere.data.user;

import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.data.address.entity.AddressEntity;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
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

    // TODO: Missing attributes
    return userEntity;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static User mapUserEntityToUser(UserEntity userEntity) {
    User user = new User();
    BeanUtils.copyProperties(userEntity, user);
    if (!ObjectUtils.isEmpty(userEntity.getStrapiId())) user.setId(userEntity.getStrapiId());
    if (!ObjectUtils.isEmpty(userEntity.getRoles()))
      user.setRoles(Arrays.stream(userEntity.getRoles().split(" ")).toList());
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
    existingUser.setAddresses(
        ObjectUtils.isEmpty(user.getAddresses())
            ? null
            : user.getAddresses().stream()
                .map(
                    address ->
                        new AddressEntity(
                            address.getId(),
                            address.getStreet(),
                            address.getCity(),
                            address.getState(),
                            address.getCountry(),
                            address.getZipCode(),
                            true,
                            existingUser))
                .toList());
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
}
