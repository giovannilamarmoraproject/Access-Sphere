package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
// public interface IUserDAO extends R2dbcRepository<UserEntity, Long> {
public interface IUserDAO extends JpaRepository<UserEntity, Long> {

  // Mono<UserEntity> findUserEntityByUsernameOrEmail(String username, String email);

  // Mono<UserEntity> findUserEntityByEmail(String email);

  //  Mono<UserEntity> findUserEntityByTokenReset(String token);

  // Mono<UserEntity> save(UserEntity userEntity);

  UserEntity findUserEntityByUsernameOrEmail(String username, String email);

  UserEntity findUserEntityByEmail(String email);

  UserEntity findUserEntityByTokenReset(String token);

  UserEntity save(UserEntity userEntity);
}
