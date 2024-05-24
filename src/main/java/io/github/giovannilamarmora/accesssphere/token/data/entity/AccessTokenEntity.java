package io.github.giovannilamarmora.accesssphere.token.data.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
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
@Table(name = "ACCESS_TOKEN_DATA")
public class AccessTokenEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Lob
  @Column(name = "REFRESH_TOKEN_HASH", columnDefinition = "TEXT")
  private String refreshTokenHash;

  @Lob
  @Column(name = "ACCESS_TOKEN_HASH", columnDefinition = "TEXT")
  private String accessTokenHash;

  @Lob
  @Column(name = "ID_TOKEN_HASH", columnDefinition = "TEXT")
  private String idTokenHash;

  @Column(name = "CLIENT_ID")
  private String clientId;

  @Column(name = "SUBJECT")
  private String subject;

  @Column(name = "EMAIL")
  private String email;

  @Column(name = "IDENTIFIER")
  private String identifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "TYPE")
  private OAuthType type;

  @Column(name = "ISSUE_DATE")
  private Long issueDate;

  @Column(name = "EXPIRE_DATE")
  private Long expireDate;

  @Lob
  @JsonRawValue
  @Column(name = "PAYLOAD", columnDefinition = "json")
  private Object payload;
}
