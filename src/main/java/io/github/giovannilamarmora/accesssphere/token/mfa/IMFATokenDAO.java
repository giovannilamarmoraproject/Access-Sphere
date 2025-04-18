package io.github.giovannilamarmora.accesssphere.token.mfa;

import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.entity.MFATokenEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IMFATokenDAO extends JpaRepository<MFATokenEntity, Long> {

  @Modifying
  @Transactional
  @Query(
      "UPDATE MFATokenEntity a SET a.status = :status WHERE a.tempToken != :tempToken AND a.identifier = :identifier AND a.expireDate < :currentTime")
  void revokeTokensExcept(
      @Param("status") TokenStatus status,
      @Param("tempToken") String tempToken,
      @Param("identifier") String identifier,
      @Param("currentTime") long currentTime);

  @Query("SELECT a FROM MFATokenEntity a WHERE a.tempToken = :token OR a.deviceToken = :token")
  List<MFATokenEntity> findByTempTokenOrDeviceToken(@Param("token") String token);
}
