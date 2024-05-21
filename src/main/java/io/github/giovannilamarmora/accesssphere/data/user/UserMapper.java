package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;

@Component
public class UserMapper {

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static UserEntity mapUserToUserEntity(User user) {
    UserEntity userEntity = new UserEntity();
    BeanUtils.copyProperties(user, userEntity);
    if (!ObjectUtils.isEmpty(user.getRoles()))
      userEntity.setRoles(String.join(" ", user.getRoles()));
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
}
