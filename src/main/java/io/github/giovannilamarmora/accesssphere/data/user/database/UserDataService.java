package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;

public interface UserDataService {

  UserEntity findUserEntityByUsernameOrEmail(String username, String email);

  UserEntity findUserEntityByEmail(String email);

  UserEntity saveAndFlush(UserEntity user);

  UserEntity findUserEntityByTokenReset(String token);

  UserEntity updateUserEntityByIdentifier(UserEntity userEntity, String identifier);
}
