package io.github.giovannilamarmora.accesssphere.client.database;

import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// public interface IClientDAO extends R2dbcRepository<ClientCredentialEntity, Long> {
public interface IOAuth2ClientDAO extends JpaRepository<OAuth2ClientEntity, Long> {

  // Mono<ClientCredentialEntity> findByClientId(String clientId);
  OAuth2ClientEntity findByClientId(String clientId);

  boolean existsByClientId(String clientId);

  void deleteByClientId(String clientId);
}
