package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.utils.generic.Response;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface MFAStrategy {
  Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest setupRequest);

  Mono<ResponseEntity<Response>> verifyCode(
      User user, MFAMethod mfaMethod, MFAConfirmationRequest confirmationRequest);
}
