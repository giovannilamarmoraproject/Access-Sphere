package io.github.giovannilamarmora.accesssphere.token.data;

import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAccessTokenDAO extends JpaRepository<AccessTokenEntity, Long> {}
