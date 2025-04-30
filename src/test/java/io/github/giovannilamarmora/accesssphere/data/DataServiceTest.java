package io.github.giovannilamarmora.accesssphere.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class DataServiceTest {

  @InjectMocks private UserDataServiceImpl dataService;

  @Mock private UserLogicService logicService; // <-- aggiunto

  @Test
  public void test_retrieve_user_by_email_when_strapi_is_enabled() {
    dataService.setStrapiEnabled(true);

    String email = "test@example.com";
    User expectedUser = new User();
    expectedUser.setEmail(email);

    // Mock del metodo nel service logic
    when(logicService.getUserByEmail(email, false)).thenReturn(Mono.just(expectedUser));

    // Act
    Mono<User> result = dataService.getUserByEmail(email);

    // Assert
    assertNotNull(result);
    assertEquals(email, result.block().getEmail());
  }
}
