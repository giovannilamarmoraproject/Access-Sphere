package io.github.giovannilamarmora.accesssphere.api.emailSender;

import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.api.emailSender.dto.EmailResponse;
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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
