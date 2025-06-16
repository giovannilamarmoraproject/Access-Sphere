package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface MFAStrategy {
  Mono<ResponseEntity<Response>> generateSecret(User user, MFASetupRequest setupRequest);

  void verifyCode(List<MFAMethod> mfaMethods, String otp, String identifier);

  Mono<ResponseEntity<Response>> mfaChallenge(User user, MFASetupRequest setupRequest);
}
