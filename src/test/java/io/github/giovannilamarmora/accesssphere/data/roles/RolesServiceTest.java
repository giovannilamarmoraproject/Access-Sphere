package io.github.giovannilamarmora.accesssphere.data.roles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.RolesService;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserRoles;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RolesServiceTest {

  @Mock private ClientService clientService;
  @Mock private UserService userService;
  @Mock private TechUserService techUserService;

  @InjectMocks private RolesService rolesService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(rolesService, "clientService", clientService);
    ReflectionTestUtils.setField(rolesService, "userService", userService);
    ReflectionTestUtils.setField(rolesService, "techUserService", techUserService);
  }

  @Test
  @DisplayName("changeRoles should throw 403 Forbidden when caller is not a tech user")
  void changeRoles_notTechUser_throwsForbidden() {
    when(techUserService.isTechUser()).thenReturn(false);

    UserRoles userRoles = new UserRoles(List.of("ROLE_ADMIN"));
    assertThrows(OAuthException.class, () -> rolesService.changeRoles("user-123", userRoles));
  }

  @Test
  @DisplayName(
      "changeRoles should accept client roles, deduplicate them, and accept system roles like SUPER_ADMIN")
  void changeRoles_success_deduplicatesAndIncludesSystemRoles() {
    when(techUserService.isTechUser()).thenReturn(true);

    ClientCredential client1 = new ClientCredential();
    client1.setClientId("linkatutto");
    client1.setAppRoles(List.of(new AppRole("LINKATUTTO_ADMIN", "DEFAULT")));

    ClientCredential client2 = new ClientCredential();
    client2.setClientId("access_sphere");
    client2.setAppRoles(List.of(new AppRole("ACCESS_SPHERE_ADMIN", "ADMIN")));

    when(clientService.getClientCredentials()).thenReturn(Mono.just(List.of(client1, client2)));

    User user = new User();
    user.setIdentifier("user-123");
    user.setUsername("giovannilamarmora");
    user.setRoles(List.of("LINKATUTTO_ADMIN"));
    when(userService.getUserByIdentifier("user-123")).thenReturn(Mono.just(user));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    when(userService.updateUser(eq("user-123"), userCaptor.capture()))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    UserRoles payload =
        new UserRoles(
            List.of(
                "LINKATUTTO_ADMIN",
                "LINKATUTTO_ADMIN", // duplicato intenzionale
                "ACCESS_SPHERE_ADMIN",
                "SUPER_ADMIN", // ruolo di sistema
                "UNKNOWN_RANDOM_ROLE" // non valido, deve essere scartato
                ));

    StepVerifier.create(rolesService.changeRoles("user-123", payload))
        .assertNext(
            (ResponseEntity<Response> response) -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());

              User updated = (User) response.getBody().getData();
              assertNotNull(updated);

              List<String> assignedRoles = updated.getRoles();
              assertNotNull(assignedRoles);
              // Deve contenere LINKATUTTO_ADMIN (una sola volta), ACCESS_SPHERE_ADMIN, SUPER_ADMIN
              assertEquals(3, assignedRoles.size());
              assertTrue(assignedRoles.contains("LINKATUTTO_ADMIN"));
              assertTrue(assignedRoles.contains("ACCESS_SPHERE_ADMIN"));
              assertTrue(assignedRoles.contains("SUPER_ADMIN"));
              assertFalse(assignedRoles.contains("UNKNOWN_RANDOM_ROLE"));
            })
        .verifyComplete();
  }
}
