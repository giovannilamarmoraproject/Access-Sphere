package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import java.util.List;

public interface UserDataService {

  UserEntity findUserEntityByUsernameOrEmail(String username, String email);

  UserEntity findUserEntityByEmail(String email);

  UserEntity saveAndFlush(UserEntity user);

  UserEntity findUserEntityByTokenReset(String token);

  UserEntity findUserEntityByIdentifier(String identifier);

  List<UserEntity> findAll();

  void delete(UserEntity userEntity);

  void deleteByIdentifier(String identifier);
}
