package io.github.giovannilamarmora.accesssphere.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientServiceTest {

  @Mock private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Mock private PermissionRoleManager permissionRoleManager;
  @Mock private AdminUserService techUserService;
  @Mock private OAuth2Authorization oAuth2Authorization;
  @Mock private IUserClientRoleDAO userClientRoleDAO;

  @InjectMocks private OAuth2ClientService oAuth2ClientService;

  private OAuth2Client techClient;
  private OAuth2Client linkatuttoClient;
  private OAuth2Client otherClient;

  @BeforeEach
  void setUp() {
    lenient().when(techUserService.getAdminClientId()).thenReturn("ACCESS-SPHERE-AUTH");

    techClient = new OAuth2Client();
    techClient.setClientId("ACCESS-SPHERE-AUTH");

    linkatuttoClient = new OAuth2Client();
    linkatuttoClient.setClientId("LINKATUTTO-ADMIN");

    otherClient = new OAuth2Client();
    otherClient.setClientId("OTHER-CLIENT");
  }

  @Test
  @DisplayName("getClients for Super Admin returns all clients")
  void testGetClientsForSuperAdmin() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);
    when(permissionRoleManager.isSuperAdmin(oAuth2Authorization)).thenReturn(true);
    when(oAuth2ClientDomainService.getOAuth2Clients())
        .thenReturn(Mono.just(List.of(techClient, linkatuttoClient, otherClient)));

    Mono<List<OAuth2Client>> result = oAuth2ClientService.getOAuth2Clients();

    StepVerifier.create(result)
        .assertNext(
            clients -> {
              assertNotNull(clients);
              assertEquals(3, clients.size());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getClients for delegated user scopes to only managed clients with clients:read")
  void testGetClientsForDelegatedUser() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);
    when(permissionRoleManager.isSuperAdmin(oAuth2Authorization)).thenReturn(false);
    when(permissionRoleManager.getManagedClientIds(oAuth2Authorization))
        .thenReturn(List.of("LINKATUTTO-ADMIN"));
    when(permissionRoleManager.hasPermission(
            oAuth2Authorization, "LINKATUTTO-ADMIN", "clients:read"))
        .thenReturn(true);
    when(oAuth2ClientDomainService.getOAuth2Clients())
        .thenReturn(Mono.just(List.of(techClient, linkatuttoClient, otherClient)));

    Mono<List<OAuth2Client>> result = oAuth2ClientService.getOAuth2Clients();

    StepVerifier.create(result)
        .assertNext(
            clients -> {
              assertNotNull(clients);
              assertEquals(1, clients.size());
              assertEquals("LINKATUTTO-ADMIN", clients.get(0).getClientId());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getClientById returns client when user is authorized")
  void testGetClientByIdAuthorized() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);
    doNothing()
        .when(permissionRoleManager)
        .assertPermission(oAuth2Authorization, "LINKATUTTO-ADMIN", "clients:read");
    when(oAuth2ClientDomainService.getOAuth2ClientByClientID("LINKATUTTO-ADMIN"))
        .thenReturn(Mono.just(linkatuttoClient));

    Mono<OAuth2Client> result = oAuth2ClientService.getOAuth2ClientByClientID("LINKATUTTO-ADMIN");

    StepVerifier.create(result)
        .assertNext(
            client -> {
              assertNotNull(client);
              assertEquals(linkatuttoClient, client);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("addClientToDatabase creates client and auto-assigns ROLE_ADMIN to non-tech creator")
  void testAddClientDelegatedUser() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);
    when(permissionRoleManager.isSuperAdmin(oAuth2Authorization)).thenReturn(false);
    when(oAuth2Authorization.getIdentifier()).thenReturn("user-uuid-1");
    doNothing()
        .when(permissionRoleManager)
        .assertPermission(oAuth2Authorization, "LINKATUTTO-ADMIN", "clients:write");
    doNothing().when(oAuth2ClientDomainService).saveOAuth2Client(linkatuttoClient);

    oAuth2ClientService.saveOAuth2Client(linkatuttoClient);

    verify(oAuth2ClientDomainService, times(1)).saveOAuth2Client(linkatuttoClient);
    verify(userClientRoleDAO, times(1))
        .save(
            argThat(
                entity ->
                    entity.getUserIdentifier().equals("user-uuid-1")
                        && entity.getClientId().equals("LINKATUTTO-ADMIN")));
  }

  @Test
  @DisplayName("updateClientInDatabase prevents non-tech user from modifying technical client")
  void testUpdateTechnicalClientDeniedForNonTech() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);
    when(permissionRoleManager.isSuperAdmin(oAuth2Authorization)).thenReturn(false);

    assertThrows(
        OAuthException.class, () -> oAuth2ClientService.updateOAuth2ClientByClientId(techClient));
    verify(oAuth2ClientDomainService, never()).updateOAuth2ClientByClientId(any());
  }

  @Test
  @DisplayName("deleteClientFromDatabase prevents deleting technical client")
  void testDeleteTechnicalClientDenied() {
    doNothing().when(permissionRoleManager).assertHasConsoleAccess(oAuth2Authorization);

    assertThrows(
        OAuthException.class, () -> oAuth2ClientService.deleteOAuth2ClientByClientId(techClient));
    verify(oAuth2ClientDomainService, never()).deleteOAuth2ClientByClientId(any());
  }
}
