package io.github.giovannilamarmora.accesssphere.data.user.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
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
@Table(name = "USERS")
public class UserEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "STRAPI_ID")
  private Long strapiId;

  @Column(name = "IDENTIFIER", nullable = false)
  private String identifier;

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

  @Column(name = "ROLES", nullable = false)
  private String roles;

  @Lob
  @Column(name = "PROFILE_PHOTO", columnDefinition = "LONGTEXT")
  private String profilePhoto;

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
  @Column(name = "ATTRIBUTES", columnDefinition = "TEXT")
  private String attributes;

  @Lob
  @Column(name = "MFA_SETTINGS", columnDefinition = "TEXT")
  private String mfaSettings;
}
