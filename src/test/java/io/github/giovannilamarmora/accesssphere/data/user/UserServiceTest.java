package io.github.giovannilamarmora.accesssphere.data.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.client.IOAuth2ClientDAO;
import io.github.giovannilamarmora.accesssphere.data.user.database.IUserDAO;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private IUserDAO userDAO;

  @Mock
  private io.github.giovannilamarmora.accesssphere.roles.database.IUserClientRoleDAO
      userClientRoleDAO;

  @Mock private IOAuth2ClientDAO clientDAO;

  @InjectMocks private UserService userService;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userService, "userDAO", userDAO);
    ReflectionTestUtils.setField(userService, "userClientRoleDAO", userClientRoleDAO);
    ReflectionTestUtils.setField(userService, "clientDAO", clientDAO);
  }

  @Test
  @DisplayName("getUserByIdentifier should return user when found")
  void getUserByIdentifier_success() {
    UserEntity entity = new UserEntity();
    entity.setId(1L);
    entity.setIdentifier("uuid-123");
    entity.setUsername("testuser");
    entity.setEmail("test@example.com");

    when(userDAO.findUserEntityByIdentifier("uuid-123")).thenReturn(entity);

    StepVerifier.create(userService.getUserByIdentifier("uuid-123"))
        .assertNext(
            user -> {
              assertEquals("uuid-123", user.getIdentifier());
              assertEquals("testuser", user.getUsername());
              assertEquals("test@example.com", user.getEmail());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getUserByIdentifier should throw OAuthException when not found")
  void getUserByIdentifier_notFound() {
    when(userDAO.findUserEntityByIdentifier("unknown")).thenReturn(null);

    assertThrows(OAuthException.class, () -> userService.getUserByIdentifier("unknown"));
  }

  @Test
  @DisplayName("getUserByEmail should return user when found")
  void getUserByEmail_success() {
    UserEntity entity = new UserEntity();
    entity.setId(1L);
    entity.setIdentifier("uuid-456");
    entity.setUsername("emailuser");
    entity.setEmail("user@example.com");

    when(userDAO.findUserEntityByEmail("user@example.com")).thenReturn(entity);

    StepVerifier.create(userService.getUserByEmail("user@example.com"))
        .assertNext(
            user -> {
              assertEquals("uuid-456", user.getIdentifier());
              assertEquals("emailuser", user.getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("authenticateUser should succeed with valid password")
  void authenticateUser_success() {
    UserEntity entity = new UserEntity();
    entity.setId(1L);
    entity.setUsername("authuser");
    entity.setEmail("auth@example.com");
    entity.setPassword(encoder.encode("SecretPass123!"));

    when(userDAO.findUserEntityByUsernameOrEmail("authuser", null)).thenReturn(entity);

    StepVerifier.create(userService.authenticateUser("authuser", null, "SecretPass123!"))
        .assertNext(
            user -> {
              assertEquals("authuser", user.getUsername());
              assertNull(user.getPassword(), "Password must be stripped from DTO");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("authenticateUser should throw OAuthException on invalid password")
  void authenticateUser_wrongPassword() {
    UserEntity entity = new UserEntity();
    entity.setId(1L);
    entity.setUsername("authuser");
    entity.setPassword(encoder.encode("CorrectPassword"));

    when(userDAO.findUserEntityByUsernameOrEmail("authuser", null)).thenReturn(entity);

    assertThrows(
        OAuthException.class,
        () -> userService.authenticateUser("authuser", null, "WrongPassword"));
  }

  @Test
  @DisplayName("authenticateUser should throw OAuthException when user not found")
  void authenticateUser_userNotFound() {
    when(userDAO.findUserEntityByUsernameOrEmail("ghost", null)).thenReturn(null);

    assertThrows(
        OAuthException.class, () -> userService.authenticateUser("ghost", null, "anyPassword"));
  }

  @Test
  @DisplayName("getUsersFromDatabase should return mapped DTO list")
  void getUsersFromDatabase_success() {
    UserEntity u1 = new UserEntity();
    u1.setId(1L);
    u1.setUsername("u1");

    UserEntity u2 = new UserEntity();
    u2.setId(2L);
    u2.setUsername("u2");

    when(userDAO.findAll()).thenReturn(List.of(u1, u2));

    StepVerifier.create(userService.getUsers())
        .assertNext(
            users -> {
              assertEquals(2, users.size());
              assertEquals("u1", users.get(0).getUsername());
              assertEquals("u2", users.get(1).getUsername());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "updateUser should update user and preserve existing mfaSettings, tokenReset and roles")
  void updateUser_success() {
    UserEntity existing = new UserEntity();
    existing.setId(99L);
    existing.setIdentifier("uuid-99");
    existing.setUsername("original");
    existing.setEmail("orig@example.com");
    existing.setPassword("encodedPass");
    existing.setRoles("ROLE_USER");
    existing.setMfaSettings("{\"enabled\":true}");
    existing.setTokenReset("reset-token-xyz");

    when(userDAO.findUserEntityByIdentifier("uuid-99")).thenReturn(existing);
    when(userDAO.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    User updateDto = new User();
    updateDto.setName("NewName");
    updateDto.setSurname("NewSurname");
    updateDto.setUsername("updatedUsername");
    updateDto.setEmail("orig@example.com");

    StepVerifier.create(userService.updateUser("uuid-99", updateDto))
        .assertNext(
            updated -> {
              assertEquals("uuid-99", updated.getIdentifier());
              assertEquals("NewName", updated.getName());
              assertEquals("NewSurname", updated.getSurname());
              assertNotNull(updated.getMfaSettings());
              assertTrue(updated.getMfaSettings().getEnabled());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateUser with explicit roles should successfully update roles")
  void updateUser_withExplicitRoles_shouldUpdateRoles() {
    UserEntity existing = new UserEntity();
    existing.setId(99L);
    existing.setIdentifier("uuid-99");
    existing.setUsername("original");
    existing.setEmail("orig@example.com");
    existing.setRoles("ROLE_USER");

    when(userDAO.findUserEntityByIdentifier("uuid-99")).thenReturn(existing);
    when(userDAO.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    User updateDto = new User();
    updateDto.setUsername("original");
    updateDto.setEmail("orig@example.com");
    updateDto.setRoles(List.of("ACCESS_SPHERE_ADMIN", "SUPER_ADMIN"));

    StepVerifier.create(userService.updateUser("uuid-99", updateDto))
        .assertNext(
            updated -> {
              assertEquals("uuid-99", updated.getIdentifier());
              assertNotNull(updated.getRoles());
              assertEquals(List.of("ACCESS_SPHERE_ADMIN", "SUPER_ADMIN"), updated.getRoles());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateUser should throw OAuthException when user to update does not exist")
  void updateUser_notFound_throws() {
    when(userDAO.findUserEntityByIdentifier("non-existent")).thenReturn(null);

    User updateDto = new User();
    updateDto.setUsername("ghost");

    assertThrows(OAuthException.class, () -> userService.updateUser("non-existent", updateDto));
  }
}
