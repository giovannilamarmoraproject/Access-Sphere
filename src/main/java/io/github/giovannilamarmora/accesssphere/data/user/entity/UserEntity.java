package io.github.giovannilamarmora.accesssphere.data.user.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.giovannilamarmora.accesssphere.data.address.entity.AddressEntity;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "USERS")
public class UserEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "SURNAME")
  private String surname;

  @Column(name = "EMAIL", nullable = false, unique = true)
  private String email;

  @Column(name = "USERNAME", nullable = false, unique = true)
  private String username;

  @Column(name = "PASSWORD")
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "ROLE", nullable = false)
  private UserRole role;

  @Lob
  @Column(name = "PROFILE_PHOTO")
  private String profilePhoto;

  @OrderBy(value = "id")
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private List<AddressEntity> addresses;

  @Column(name = "PHONE_NUMBER")
  private String phoneNumber;

  @Temporal(TemporalType.DATE)
  @Column(name = "BIRTH_DATE")
  private LocalDate birthDate;

  @Column(name = "GENDER")
  private String gender;

  @Column(name = "OCCUPATION")
  private String occupation;

  @Column(name = "EDUCATION")
  private String education;

  @Column(name = "NATIONALITY")
  private String nationality;

  @Column(name = "SSN")
  private String ssn; // Social Security Number

  @Column(name = "TOKEN_RESET")
  private String tokenReset;

  @Lob
  @JsonRawValue
  @Column(name = "ATTRIBUTES", columnDefinition = "json", nullable = false)
  private String attributes;
}
