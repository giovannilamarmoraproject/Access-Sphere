package io.github.giovannilamarmora.accesssphere.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientControllerTest {

  @Mock private OAuth2ClientService oAuth2ClientService;

  @InjectMocks private OAuth2ClientControllerImpl oAuth2ClientController;

  private OAuth2Client sampleClient;

  @BeforeEach
  void setUp() {
    sampleClient = new OAuth2Client();
    sampleClient.setClientId("TEST-CLIENT-001");
    sampleClient.setClientSecret("sample-secret");
  }

  @Test
  @DisplayName("getClientCredentials returns 200 OK with list of clients")
  void testGetClientCredentials() {
    Response mockResponse = new Response(HttpStatus.OK.value(), "Client credential list", "span-1", List.of(sampleClient));
    when(oAuth2ClientService.getClients()).thenReturn(Mono.just(ResponseEntity.ok(mockResponse)));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.getClientCredentials("Bearer sample-token", MockServerHttpRequest.get("/v1/clients").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals(200, responseEntity.getBody().getStatus());
          assertEquals(List.of(sampleClient), responseEntity.getBody().getData());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).getClients();
  }

  @Test
  @DisplayName("getClientById returns 200 OK with single client")
  void testGetClientById() {
    Response mockResponse = new Response(HttpStatus.OK.value(), "Client details", "span-2", sampleClient);
    when(oAuth2ClientService.getClientById("TEST-CLIENT-001")).thenReturn(Mono.just(ResponseEntity.ok(mockResponse)));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.getClientById("Bearer sample-token", "TEST-CLIENT-001", MockServerHttpRequest.get("/v1/clients/TEST-CLIENT-001").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals(sampleClient, responseEntity.getBody().getData());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).getClientById("TEST-CLIENT-001");
  }

  @Test
  @DisplayName("createClient calls service and returns 201 CREATED")
  void testCreateClient() {
    doNothing().when(oAuth2ClientService).addClientToDatabase(any(OAuth2Client.class));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.createClient("Bearer sample-token", sampleClient, MockServerHttpRequest.post("/v1/clients").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals(HttpStatus.CREATED.value(), responseEntity.getBody().getStatus());
          assertEquals(sampleClient, responseEntity.getBody().getData());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).addClientToDatabase(sampleClient);
  }

  @Test
  @DisplayName("updateClientById sets clientId and returns 200 OK")
  void testUpdateClientById() {
    OAuth2Client clientWithoutId = new OAuth2Client();
    clientWithoutId.setClientSecret("updated-secret");
    doNothing().when(oAuth2ClientService).updateClientInDatabase(any(OAuth2Client.class));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.updateClientById("PATH-CLIENT-ID", "Bearer sample-token", clientWithoutId, MockServerHttpRequest.put("/v1/clients/PATH-CLIENT-ID").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals("PATH-CLIENT-ID", clientWithoutId.getClientId());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).updateClientInDatabase(clientWithoutId);
  }

  @Test
  @DisplayName("updateClient calls service and returns 200 OK")
  void testUpdateClient() {
    doNothing().when(oAuth2ClientService).updateClientInDatabase(any(OAuth2Client.class));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.updateClient("Bearer sample-token", sampleClient, MockServerHttpRequest.put("/v1/clients").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals(sampleClient, responseEntity.getBody().getData());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).updateClientInDatabase(sampleClient);
  }

  @Test
  @DisplayName("deleteClient calls service and returns 200 OK with null data")
  void testDeleteClient() {
    doNothing().when(oAuth2ClientService).deleteClientFromDatabase(any(OAuth2Client.class));

    Mono<ResponseEntity<Response>> result =
        oAuth2ClientController.deleteClient("Bearer sample-token", "DELETE-CLIENT-ID", MockServerHttpRequest.delete("/v1/clients/DELETE-CLIENT-ID").build());

    StepVerifier.create(result)
        .assertNext(responseEntity -> {
          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
          assertNotNull(responseEntity.getBody());
          assertEquals("Client deleted", responseEntity.getBody().getMessage());
          assertNull(responseEntity.getBody().getData());
        })
        .verifyComplete();

    verify(oAuth2ClientService, times(1)).deleteClientFromDatabase(any(OAuth2Client.class));
  }
}
