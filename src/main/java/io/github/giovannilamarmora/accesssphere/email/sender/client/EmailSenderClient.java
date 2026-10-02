package io.github.giovannilamarmora.accesssphere.email.sender.client;

import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.webClient.UtilsUriBuilder;
import io.github.giovannilamarmora.utils.webClient.WebClientRest;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client HTTP reattivo per l'integrazione remota con il microservizio esterno email-sender.
 * Incapsulato all'interno del sottosistema di invio email.
 */
@Component
@Logged
public class EmailSenderClient {

  private final WebClientRest webClientRest = new WebClientRest();

  @Value(value = "${rest.client.email-sender.baseUrl}")
  private String emailSenderUrl;

  @Value(value = "${rest.client.email-sender.path.sendEmailUrl}")
  private String sendEmailUrl;

  @Autowired private WebClient.Builder builder;

  @PostConstruct
  void init() {
    webClientRest.setBaseUrl(emailSenderUrl);
    webClientRest.init(builder);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.EXTERNAL)
  public Mono<ResponseEntity<EmailResponse>> sendEmail(EmailContent emailContent) {
    Map<String, Object> params = new HashMap<>();
    params.put("htmlText", true);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return webClientRest.perform(
        HttpMethod.POST,
        UtilsUriBuilder.buildUri(sendEmailUrl, params),
        emailContent,
        headers,
        EmailResponse.class);
  }
}
