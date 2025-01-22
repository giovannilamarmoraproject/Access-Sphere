package io.github.giovannilamarmora.accesssphere.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiUser;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@SpringBootTest
public class DataServiceTest {

  @InjectMocks private DataService dataService;
  @Mock private StrapiService strapiService;

  @Test
  public void test_retrieve_user_by_email_when_strapi_is_enabled() {
    dataService.setIsStrapiEnabled(true);
    String email = "test@example.com";
    UserEntity userEntity = new UserEntity();
    userEntity.setEmail(email);
    userEntity.setUsername("testuser");

    StrapiUser strapiUser = new StrapiUser();
    strapiUser.setEmail(email);
    when(strapiService.getUserByEmail(email)).thenReturn(Mono.just(strapiUser));

    // Act
    Mono<User> result = dataService.getUserByEmail(email);

    // Assert
    assertNotNull(result);
    assertEquals(email, result.block().getEmail());
  }
}
