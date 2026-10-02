package io.github.giovannilamarmora.accesssphere.email.sender.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Modello DTO di risposta restituito a seguito dell'avvenuta operazione di invio email.
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailResponse {

  private LocalDateTime timestamp;
  private String message;
  private String token;

  public EmailResponse() {
    this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
  }
}
