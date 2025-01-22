package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
// public interface IClientDAO extends R2dbcRepository<ClientCredentialEntity, Long> {
public interface IClientDAO extends JpaRepository<ClientCredentialEntity, Long> {

  // Mono<ClientCredentialEntity> findByClientId(String clientId);
  ClientCredentialEntity findByClientId(String clientId);
}
