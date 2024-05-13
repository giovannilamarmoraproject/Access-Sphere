package io.github.giovannilamarmora.accesssphere.data.address.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "ADDRESS")
public class AddressEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "STREET", nullable = false)
  private String street;

  @Column(name = "CITY", nullable = false)
  private String city;

  @Column(name = "STATE", nullable = false)
  private String state;

  @Column(name = "COUNTRY", nullable = false)
  private String country;

  @Column(name = "ZIP_CODE", nullable = false)
  private String zipCode;

  @Column(name = "PRIMARY_ADDRESS")
  private Boolean primary = true;

  @ManyToOne
  @JoinColumn(name = "USER_ID", nullable = true)
  private UserEntity user;
}
