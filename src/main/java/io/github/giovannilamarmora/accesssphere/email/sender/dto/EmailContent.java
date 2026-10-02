package io.github.giovannilamarmora.accesssphere.email.sender.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello DTO che incapsula i parametri completi di un messaggio email destinato all'invio.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailContent {

  public static final String RESET_TEMPLATE = "EMAIL_RESET.html";
  public static final String CONTACT_TEMPLATE = "CONTACT_EMAIL.html";

  private String bbc;
  private String cc;
  private String from;
  private String replyTo;
  private Date sentDate;

  @NotBlank(message = "The field 'subject' is required and it cannot be null or blank")
  private String subject;

  @NotBlank(message = "The field 'text' is required and it cannot be null or blank")
  private String text;

  @NotBlank(message = "The field 'to' is required and it cannot be null or blank")
  private String to;
}
