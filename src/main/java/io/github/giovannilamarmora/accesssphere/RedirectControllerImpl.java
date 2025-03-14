package io.github.giovannilamarmora.accesssphere;

import io.github.giovannilamarmora.accesssphere.utilities.ExposedHeaders;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Logged
@Controller
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*",
    exposedHeaders = {
      ExposedHeaders.LOCATION,
      ExposedHeaders.SESSION_ID,
      ExposedHeaders.AUTHORIZATION,
      ExposedHeaders.TRACE_ID,
      ExposedHeaders.SPAN_ID,
      ExposedHeaders.PARENT_ID,
      ExposedHeaders.REDIRECT_URI,
      ExposedHeaders.REGISTRATION_TOKEN
    })
public class RedirectControllerImpl {

  @GetMapping("/cookie-policy")
  public String cookie_policy(Model model) {
    return "cookie-policy";
  }

  @GetMapping("/privacy-policy")
  public String privacy_policy(Model model) {
    return "privacy-policy";
  }

  @GetMapping("/app/login")
  public String login(Model model) {
    return "app/login/index";
  }

  @GetMapping("/app/users")
  public String users(Model model) {
    return "app/user/users";
  }

  @GetMapping("/app/users/details/{identifier}")
  public String usersDetails(Model model) {
    return "app/user/user";
  }

  @GetMapping("/app/users/register")
  public String register(Model model) {
    return "app/user/register";
  }
}
