package io.github.giovannilamarmora.accesssphere.mfa.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiToken;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.strategy.MFAStrategyFactory;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.token.mfa.MFATokenService;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFAToken;
import io.github.giovannilamarmora.accesssphere.utilities.ExposedHeaders;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.RequestManager;
import io.github.giovannilamarmora.utils.web.ResponseManager;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@Logged
public class MFAAuthenticationService {

  @Value("${cookie-domain:}")
  private String cookieDomain;

  private static final Logger LOG = LoggerFilter.getLogger(MFAStrategyFactory.class);
  @Autowired private ClientService clientService;
  @Autowired private TokenService tokenService;
  @Autowired private MFATokenService mfaTokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static boolean isMfaEnabled(User user) {
    return !ObjectToolkit.isNullOrEmpty(user.getMfaSettings())
        && user.getMfaSettings().getEnabled()
        && !ObjectToolkit.isNullOrEmpty(user.getMfaSettings().getMfaMethods());
  }

  public Mono<OAuthTokenResponse> checkMFAAndMakeLogin(
      Map<String, Object> strapiToken,
      JWTData jwtData,
      User user,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    String device_token =
        RequestManager.getCookieOrHeaderData(ExposedHeaders.DEVICE_TOKEN, request);
    if (!ObjectToolkit.isNullOrEmpty(device_token)) {
      mfaTokenService.getByTempTokenOrDeviceToken(device_token);
      LOG.info("✅ The Device Token is Valid, skip MFA and continue with the login");
    } else if (MFAAuthenticationService.isMfaEnabled(user)) {
      strapiToken.put("expires_at", jwtData.getExp());
      AuthToken mfaToken = tokenService.generateMFAToken(user, clientCredential, strapiToken);
      return Mono.just(
          new OAuthTokenResponse(
              mfaToken, Utils.mapper().convertValue(strapiToken, JsonNode.class), jwtData, user));
    }
    return Mono.empty();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> verifyMfaAndGenerateToken(
      MfaVerificationRequest mfaRequest,
      MFAToken mfaToken,
      User user,
      ServerWebExchange exchange,
      UserDataService dataService) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(mfaToken.getClientId());
    return clientCredentialMono.flatMap(
        clientCredential -> {
          JWTData jwtData =
              JWTData.generateJWTData(user, clientCredential, SubjectType.CUSTOMER, request);
          StrapiToken strapiToken = Mapper.convertObject(mfaToken.getPayload(), StrapiToken.class);
          return dataService
              .getUserInfo(jwtData, strapiToken.getAccess_token())
              .map(
                  user1 -> {
                    String deviceToken = null;
                    if (!ObjectToolkit.isNullOrEmpty(mfaRequest.rememberDevice())
                        && mfaRequest.rememberDevice()) {
                      deviceToken = UUID.randomUUID().toString();
                      ResponseManager.setCookieAndHeaderData(
                          ExposedHeaders.DEVICE_TOKEN, deviceToken, cookieDomain, response);
                    }
                    mfaTokenService.completeLogin(
                        mfaToken, deviceToken, mfaRequest.mfaMethod().name());
                    jwtData.setRoles(user1.getRoles());
                    AuthToken token =
                        tokenService.generateToken(jwtData, clientCredential, strapiToken);
                    OAuthTokenResponse oAuthTokenResponse =
                        new OAuthTokenResponse(
                            token,
                            Utils.mapper().convertValue(strapiToken, JsonNode.class),
                            jwtData,
                            user1);
                    String message =
                        "Login Successfully! Welcome back "
                            + oAuthTokenResponse.getUser().getUsername()
                            + "!";

                    return ResponseEntity.status(HttpStatus.OK.value())
                        .body(
                            new Response(
                                HttpStatus.OK.value(),
                                message,
                                TraceUtils.getSpanID(),
                                new OAuthTokenResponse(
                                    oAuthTokenResponse.getToken(),
                                    clientCredential.getStrapiToken()
                                        ? oAuthTokenResponse.getStrapiToken()
                                        : null,
                                    includeUserInfo ? oAuthTokenResponse.getUserInfo() : null,
                                    includeUserData ? oAuthTokenResponse.getUser() : null)));
                  });
        });
  }
}
