package io.github.giovannilamarmora.accesssphere.token.mfa.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.LoginStatus;
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
@Table(name = "MFA_TOKEN")
public class MFATokenEntity extends GenericEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "TEMP_TOKEN")
  private String tempToken;

  @Column(name = "DEVICE_TOKEN")
  private String deviceToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "LOGIN_STATUS")
  private LoginStatus loginStatus;

  @Column(name = "MFA_METHODS")
  private String mfaMethods;

  @Column(name = "SUBJECT")
  private String subject;

  @Column(name = "CLIENT_ID")
  private String clientId;

  @Column(name = "SESSION_ID")
  private String sessionId;

  @Column(name = "IDENTIFIER")
  private String identifier;

  @Column(name = "ISSUE_DATE")
  private Long issueDate;

  @Column(name = "EXPIRE_DATE")
  private Long expireDate;

  @Lob
  @JsonRawValue
  @Column(name = "PAYLOAD", columnDefinition = "json")
  private Object payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS")
  private TokenStatus status;
}
