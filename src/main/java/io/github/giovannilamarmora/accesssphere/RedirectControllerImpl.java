package io.github.giovannilamarmora.accesssphere;

import io.github.giovannilamarmora.utils.interceptors.Logged;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Logged
@Controller
public class RedirectControllerImpl {

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("client_id", "TEST");
    return "login/index";
  }
}
