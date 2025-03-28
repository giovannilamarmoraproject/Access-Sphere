package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserDAO extends JpaRepository<UserEntity, Long> {

  UserEntity findUserEntityByUsernameOrEmail(String username, String email);

  UserEntity findUserEntityByEmail(String email);

  UserEntity findUserEntityByTokenReset(String token);

  UserEntity findUserEntityByIdentifier(String identifier);

  @Transactional
  void deleteUserEntityByIdentifier(String identifier);
}
