package io.github.giovannilamarmora.accesssphere.data.user.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.api.emailSender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientService;
import io.github.giovannilamarmora.accesssphere.data.tech.AdminUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.email.EmailTemplateService;
import io.github.giovannilamarmora.accesssphere.email.dto.EmailTemplateDTO;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private OAuth2ClientService oAuth2ClientService;
  @Mock private TokenService tokenService;
  @Mock private AdminUserService techUserService;
  @Mock private EmailTemplateService emailTemplateService;
  @Mock private EmailSenderService emailSenderService;
  @Mock private UserService userService;
  @Mock private OAuth2Authorization oAuth2Authorization;

  @InjectMocks private UserAccountService userAccountService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userAccountService, "oAuth2ClientService", oAuth2ClientService);
    ReflectionTestUtils.setField(userAccountService, "tokenService", tokenService);
    ReflectionTestUtils.setField(userAccountService, "techUserService", techUserService);
    ReflectionTestUtils.setField(userAccountService, "emailTemplateService", emailTemplateService);
    ReflectionTestUtils.setField(userAccountService, "emailSenderService", emailSenderService);
    ReflectionTestUtils.setField(userAccountService, "userService", userService);
    ReflectionTestUtils.setField(userAccountService, "oAuth2Authorization", oAuth2Authorization);
  }

  @Test
  @DisplayName("changePasswordRequest should throw error when email is invalid")
  void changePasswordRequest_invalidEmail_throws() {
    ChangePassword cp = new ChangePassword();
    cp.setEmail("invalid-email");

    assertThrows(
        OAuthException.class, () -> userAccountService.changePasswordRequest(cp, "it", false));
  }

  @Test
  @DisplayName("changePasswordRequest should return token when sendEmail is false")
  void changePasswordRequest_successWithoutEmail() {
    ChangePassword cp = new ChangePassword();
    cp.setEmail("test@example.com");

    User user = new User();
    user.setIdentifier("uuid-test");
    user.setUsername("testuser");
    user.setEmail("test@example.com");

    when(userService.getUserByEmail("test@example.com")).thenReturn(Mono.just(user));

    EmailTemplateDTO template = new EmailTemplateDTO();
    template.setSubject("Password Reset");
    when(emailTemplateService.getTemplateByCategoryLocaleAndClient(any(), any(), any()))
        .thenReturn(Mono.just(template));

    when(userService.updateUser(any(), any(User.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

    StepVerifier.create(userAccountService.changePasswordRequest(cp, "it", false))
        .assertNext(
            emailResponse -> {
              assertNotNull(emailResponse.getToken());
            })
        .verifyComplete();
  }
}
