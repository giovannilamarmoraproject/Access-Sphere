package io.github.giovannilamarmora.accesssphere.token.data;

import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IAccessTokenDAO extends JpaRepository<AccessTokenEntity, Long> {

  @Query(
      "SELECT a FROM AccessTokenEntity a WHERE a.accessTokenHash = :tokenHash OR a.idTokenHash = :tokenHash")
  AccessTokenEntity findByTokenHash(@Param("tokenHash") String tokenHash);
}
