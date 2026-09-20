package io.github.giovannilamarmora.accesssphere;

import io.github.giovannilamarmora.utils.interceptors.Logged;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Logged
@Controller
public class RedirectControllerImpl {

  @GetMapping("/cookie-policy")
  public String cookie_policy(Model model) {
    return "cookie-policy";
  }

  @GetMapping("/privacy-policy")
  public String privacy_policy(Model model) {
    return "privacy-policy";
  }

  @GetMapping({"/docs", "/docs/"})
  public String documentation() {
    return "redirect:/docs/it/overview";
  }

  @GetMapping({
    "/docs/it/overview",
    "/docs/it/architecture",
    "/docs/it/required",
    "/docs/it/install",
    "/docs/it/configuration",
    "/docs/it/deployment",
    "/docs/it/oauth",
    "/docs/it/iam",
    "/docs/it/mfa-settings",
    "/docs/it/integration",
    "/docs/it/api",
    "/docs/en/overview",
    "/docs/en/architecture",
    "/docs/en/required",
    "/docs/en/install",
    "/docs/en/configuration",
    "/docs/en/deployment",
    "/docs/en/oauth",
    "/docs/en/iam",
    "/docs/en/mfa-settings",
    "/docs/en/integration",
    "/docs/en/api"
  })
  @ResponseBody
  public ResponseEntity<Resource> documentationPage(ServerHttpRequest request) {
    String path = request.getPath().value();
    String documentationPath = path.substring("/docs/".length()) + ".html";
    return documentationPage(documentationPath);
  }

  private ResponseEntity<Resource> documentationPage(String page) {
    Resource resource = new ClassPathResource("static/docs/" + page);
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(resource);
  }

  @GetMapping("/app/login")
  public String login(Model model) {
    return "app/login/index";
  }

  @GetMapping("/app/login_old")
  public String loginOld(Model model) {
    return "app/login/index_old";
  }

  @GetMapping({"/app", "/app/dashboard", "/app/home", "/app/settings"})
  public String dashboard(Model model) {
    return "app/dashboard";
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

  @GetMapping("/app/users/roles/{identifier}")
  public String roles(Model model) {
    return "app/user/roles";
  }

  @GetMapping("/app/users/edit/{identifier}")
  public String editUser(Model model) {
    return "app/user/edit";
  }

  @GetMapping("/app/mfa/{identifier}")
  public String mfa(Model model) {
    return "app/user/mfa";
  }

  @GetMapping({"/app/clients", "/app/client", "/app/client/clients"})
  public String clients(Model model) {
    return "app/client/clients";
  }

  @GetMapping("/app/clients/register")
  public String registerClient(Model model) {
    return "app/client/register";
  }

  @GetMapping({"/app/clients/details", "/app/clients/details/{identifier}"})
  public String clientDetails(Model model) {
    return "app/client/client";
  }

  @GetMapping({"/app/clients/edit", "/app/clients/edit/{identifier}"})
  public String editClient(Model model) {
    return "app/client/edit";
  }

  /**
   * Routing per la pagina di gestione dei template email e invio test.
   * Restituisce la vista Thymeleaf statica "app/email/emails.html".
   */
  @GetMapping({"/app/emails", "/app/email"})
  public String emails(Model model) {
    return "app/email/emails";
  }

  /**
   * Reindirizzamento verso la console Swagger UI OpenAPI (/swagger-ui.html).
   * Consente l'accesso intuitivo sia da '/swagger-ui' sia con trailing slash '/swagger-ui/',
   * reindirizzando al path documentale configurato da Springdoc.
   *
   * @return Redirect view verso '/swagger-ui.html'
   */
  @GetMapping({"/swagger-ui", "/swagger-ui/"})
  public String swaggerUiRedirect() {
    return "redirect:/swagger-ui.html";
  }
}
