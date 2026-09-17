package io.github.giovannilamarmora.accesssphere;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class RedirectControllerTest {

  private final RedirectControllerImpl redirectController = new RedirectControllerImpl();

  @Test
  @DisplayName("swaggerUiRedirect returns redirect view to /swagger-ui.html")
  void testSwaggerUiRedirect() {
    String result = redirectController.swaggerUiRedirect();
    assertEquals("redirect:/swagger-ui.html", result);
  }

  @Test
  @DisplayName("static page routings return correct view names")
  void testStaticViews() {
    assertEquals("cookie-policy", redirectController.cookie_policy(new ExtendedModelMap()));
    assertEquals("privacy-policy", redirectController.privacy_policy(new ExtendedModelMap()));
    assertEquals("app/dashboard", redirectController.dashboard(new ExtendedModelMap()));
    assertEquals("app/login/index", redirectController.login(new ExtendedModelMap()));
    assertEquals("app/email/emails", redirectController.emails(new ExtendedModelMap()));
  }
}
