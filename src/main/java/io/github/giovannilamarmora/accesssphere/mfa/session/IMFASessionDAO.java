package io.github.giovannilamarmora.accesssphere.mfa.session;

import io.github.giovannilamarmora.accesssphere.mfa.session.entity.MFASessionEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IMFASessionDAO extends JpaRepository<MFASessionEntity, Long> {

  @Modifying
  @Transactional
  @Query(
      "UPDATE MFASessionEntity a SET a.status = :status WHERE a.tempToken != :tempToken AND a.identifier = :identifier AND a.expireDate < :currentTime")
  void revokeTokensExcept(
      @Param("status") TokenStatus status,
      @Param("tempToken") String tempToken,
      @Param("identifier") String identifier,
      @Param("currentTime") long currentTime);

  @Query("SELECT a FROM MFASessionEntity a WHERE a.tempToken = :token OR a.deviceToken = :token")
  List<MFASessionEntity> findByTempTokenOrDeviceToken(@Param("token") String token);

  @Modifying
  @Transactional
  @Query("DELETE FROM MFASessionEntity a WHERE a.status = 'EXPIRED' OR a.status = 'REVOKED'")
  void deleteExpiredToken();
}
