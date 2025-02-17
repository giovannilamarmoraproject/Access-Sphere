package io.github.giovannilamarmora.accesssphere.token.data;

import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IAccessTokenDAO extends JpaRepository<AccessTokenEntity, Long> {

  @Query(
      "SELECT a FROM AccessTokenEntity a WHERE a.accessTokenHash = :tokenHash OR a.idTokenHash = :tokenHash OR a.refreshTokenHash = :tokenHash")
  List<AccessTokenEntity> findByTokenHash(@Param("tokenHash") String tokenHash);

  // @Modifying
  // @Transactional
  // @Query(
  //    "UPDATE AccessTokenEntity a SET a.status = :status WHERE a.accessTokenHash !=
  // :accessTokenHash AND a.identifier = :identifier")
  // void revokeTokensExcept(
  //    @Param("status") TokenStatus status,
  //    @Param("accessTokenHash") String accessTokenHash,
  //    @Param("identifier") String identifier);
  @Modifying
  @Transactional
  @Query(
      "UPDATE AccessTokenEntity a SET a.status = :status WHERE a.accessTokenHash != :accessTokenHash AND a.identifier = :identifier AND a.refreshExpireDate < :currentTime")
  void revokeTokensExcept(
      @Param("status") TokenStatus status,
      @Param("accessTokenHash") String accessTokenHash,
      @Param("identifier") String identifier,
      @Param("currentTime") long currentTime);

  @Modifying
  @Transactional
  @Query("UPDATE AccessTokenEntity a SET a.status = :status WHERE a.identifier = :identifier")
  void revokeToken(@Param("status") TokenStatus status, @Param("identifier") String identifier);

  @Modifying
  @Transactional
  @Query("DELETE FROM AccessTokenEntity a WHERE a.status = 'EXPIRED' OR a.status = 'REVOKED'")
  void deleteExpiredToken();
}
