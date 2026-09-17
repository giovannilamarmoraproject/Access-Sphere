package io.github.giovannilamarmora.accesssphere.oAuth.oidc;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserValidator;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Servizio applicativo dedicato all'endpoint OpenID Connect (OIDC) UserInfo, conforme alle
 * specifiche OpenID Connect Core 1.0 (Section 5.3).
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Risolve la configurazione e i permessi del client chiamante tramite {@link ClientService}.
 *   <li>Convalida la conformita' del tipo di autorizzazione del token tramite {@link
 *       UserValidator#validateAuthType}.
 *   <li>Decodifica e verifica la firma del token Bearer tramite {@link TokenService#parseToken}.
 *   <li>Per token locali di tipo BEARER, interroga {@link UserService#getUserByEmail} per
 *       recuperare i claim anagrafici dal database IAM.
 *   <li>Per token federati GOOGLE, interroga {@link GrpcService#userInfo} per verificare i claim di
 *       Google, convalida i claim tramite {@link UserValidator#validateGoogleUserInfo} e collega
 *       l'anagrafica locale.
 *   <li>Restituisce l'oggetto {@link OAuthTokenResponse} contenente i claim OIDC e, se richiesto
 *       dal parametro {@code include_user_data}, l'intero profilo utente.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class OIDCService {

  private static final Logger LOG = LoggerFilter.getLogger(OIDCService.class);

  @Autowired private ClientService clientService;
  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private UserService userService;

  /**
   * Recupera i claim di identita' OIDC per l'utente autenticato a partire dal Bearer token.
   *
   * @param bearer Token di accesso Bearer fornito dal client nell'header HTTP Authorization.
   * @param request Richiesta HTTP server per l'estrazione di query parameters opzionali.
   * @return Mono contenente la risposta OIDC {@link OAuthTokenResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> userInfo(String bearer, ServerHttpRequest request) {
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    LOG.info(
        "👤 [OIDC USER INFO] Starting UserInfo retrieval (includeUserData: {})", includeUserData);

    return clientService
        .getClientCredentialByClientID(accessTokenData.getClientId())
        .flatMap(
            clientCredential -> {
              UserValidator.validateAuthType(clientCredential, accessTokenData);
              JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
              switch (decryptToken.getType()) {
                case BEARER -> {
                  return userService
                      .getUserByEmail(decryptToken.getEmail())
                      .map(
                          user ->
                              includeUserData
                                  ? new OAuthTokenResponse(decryptToken, user)
                                  : new OAuthTokenResponse(decryptToken));
                }
                case GOOGLE -> {
                  GoogleModel userInfo =
                      GrpcService.userInfo(
                          accessTokenData.getPayload().get("id_token").textValue(),
                          clientCredential);
                  UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
                  if (includeUserData) {
                    return userService
                        .getUserByEmail(decryptToken.getEmail())
                        .map(user -> new OAuthTokenResponse(decryptToken, user));
                  }
                  return Mono.just(new OAuthTokenResponse(decryptToken));
                }
                default -> {
                  return UserValidator.defaultErrorOnType();
                }
              }
            })
        .doOnSuccess(
            res ->
                LOG.info(
                    "✅ [OIDC USER INFO] UserInfo retrieval successfully completed (includeUserData: {})",
                    includeUserData));
  }
}
