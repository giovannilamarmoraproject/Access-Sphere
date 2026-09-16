package io.github.giovannilamarmora.accesssphere.email.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.settings.entity.JsonMapConverter;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entita JPA per la persistenza dei Template Email a Database.
 * Memorizza identificatore, nome, oggetto, corpo HTML, parametri dinamici e lingua/locale.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "EMAIL_TEMPLATES")
public class EmailTemplateEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "IDENTIFIER", nullable = false)
  private String identifier;

  /**
   * Categoria applicativa del template (es. RESET_PASSWORD, WELCOME_USER, ecc.).
   * Se non specificata, assume di default RESET_PASSWORD.
   */
  @Column(name = "CATEGORY", nullable = false)
  private String category = "RESET_PASSWORD";

  /**
   * Identificativo dell'applicazione/client OAuth2 a cui appartiene il template.
   * Se nullo o vuoto, il template e globale di default per tutta la piattaforma Access Sphere.
   */
  @Column(name = "CLIENT_ID")
  private String clientId;

  @Column(name = "NAME", nullable = false)
  private String name;

  @Column(name = "SUBJECT", nullable = false)
  private String subject;

  @Lob
  @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
  private String description;

  @Lob
  @Column(name = "TEMPLATE", columnDefinition = "LONGTEXT", nullable = false)
  private String template;

  /**
   * Mappa dei parametri predefiniti associati al template (es. NAME, RESET_CODE, SITE_URL).
   * Salvata come JSON nel database tramite JsonMapConverter.
   */
  @Convert(converter = JsonMapConverter.class)
  @Lob
  @Column(name = "PARAMS", columnDefinition = "TEXT")
  private Map<String, Object> params = new LinkedHashMap<>();

  /**
   * Lingua/Locale del template (es. "en-GB", "it", "default" o null per tutte le lingue).
   */
  @Column(name = "LOCALE")
  private String locale;
}
