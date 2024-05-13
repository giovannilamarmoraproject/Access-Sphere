package io.github.giovannilamarmora.accesssphere.data.user.database;

import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import reactor.core.publisher.Mono;

public interface UserDataService {

    Mono<UserEntity> findUserEntityByUsernameOrEmail(String username, String email);

    Mono<UserEntity> findUserEntityByEmail(String email);

    Mono<UserEntity> save(UserEntity user);

    Mono<UserEntity> findUserEntityByTokenReset(String token);
}
