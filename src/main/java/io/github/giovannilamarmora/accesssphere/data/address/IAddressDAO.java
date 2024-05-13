package io.github.giovannilamarmora.accesssphere.data.address;

import io.github.giovannilamarmora.accesssphere.data.address.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
//public interface IAddressDAO extends R2dbcRepository<AddressEntity, Long>  {}
public interface IAddressDAO extends JpaRepository<AddressEntity, Long> {}
