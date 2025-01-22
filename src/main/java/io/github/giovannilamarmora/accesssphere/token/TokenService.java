package io.github.giovannilamarmora.accesssphere.token;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWEDecryptionKeySelector;
import com.nimbusds.jose.proc.JWEKeySelector;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.auth.TokenUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Logged
@Service
@RequiredArgsConstructor
public class TokenService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private final SessionID sessionID;
  @Autowired private AccessTokenService accessTokenService;

  private final String testSecret = "flhfEg6QbtVU4f9WgIUVbkTebFBX7O7lQ43ly+uKDg4=";

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AuthToken generateToken(
      JWTData jwtData, ClientCredential clientCredential, Object payload) {
    switch (clientCredential.getTokenType()) {
      case BEARER_JWT -> {
        if (ObjectUtils.isEmpty(clientCredential.getJwtSecret())) {
          LOG.error("JWT Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWT Secret on client configuration");
        }
        if (ObjectUtils.isEmpty(clientCredential.getJwtExpiration())) {
          LOG.error("JWT Expiration is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWT Expiration on client configuration");
        }
        return generateJWTToken(jwtData, clientCredential, payload);
      }
      case BEARER_JWE -> {
        if (ObjectUtils.isEmpty(clientCredential.getJweSecret())) {
          LOG.error("JWE Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWE Secret on client configuration");
        }
        if (ObjectUtils.isEmpty(clientCredential.getJweExpiration())) {
          LOG.error("JWE Expiration is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWE Expiration on client configuration");
        }
        return generateJWEToken(jwtData, clientCredential, payload);
      }
      default -> {
        LOG.error("Token type is not defined, please define them into the client");
        throw new TokenException(
            ExceptionMap.ERR_TOKEN_400, "Invalid token_type in client configuration");
      }
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public JWTData parseToken(String token, ClientCredential clientCredential) {
    switch (clientCredential.getTokenType()) {
      case BEARER_JWT -> {
        if (ObjectUtils.isEmpty(clientCredential.getJwtSecret())) {
          LOG.error("JWT Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWT Secret on client configuration");
        }
        return parseJWTToken(token, clientCredential);
      }
      case BEARER_JWE -> {
        if (ObjectUtils.isEmpty(clientCredential.getJweSecret())) {
          LOG.error("JWE Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, "Invalid JWE Secret on client configuration");
        }
        return parseJWEToken(token, clientCredential);
      }
      default -> {
        LOG.error("Token type is not defined, please define them into the client");
        throw new TokenException(
            ExceptionMap.ERR_TOKEN_400, "Invalid token_type in client configuration");
      }
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AuthToken exchangeToken(
      User user,
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    // Log per il processo di exchange
    LOG.info("🔄 Performing token exchange for client: {}", clientCredential.getClientId());

    JWTData exchangeToken = JWTData.generateJWTData(user, clientCredential, request);
    JsonNode strapi_token = OAuthMapper.getStrapiToken(accessTokenData.getPayload());

    // Esegui la logica per lo scambio del token
    AuthToken newToken =
        generateToken(
            exchangeToken,
            clientCredential,
            Utilities.isNullOrEmpty(strapi_token) ? accessTokenData.getPayload() : strapi_token);

    // Log il risultato dell'exchange
    LOG.info(
        "🔄 Token exchange completed successfully for client: {}", clientCredential.getClientId());

    return newToken;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private AuthToken generateJWTToken(
      JWTData jwtData, ClientCredential clientCredential, Object payload) {
    // Access Token
    ClaimsBuilder claims = Jwts.claims().subject(jwtData.getSub());
    claims.add(TokenClaims.IDENTIFIER.claim(), jwtData.getIdentifier());
    claims.add(TokenClaims.ISS.claim(), jwtData.getIss());
    claims.add(TokenClaims.AUD.claim(), jwtData.getAud());
    claims.add(TokenClaims.EMAIL.claim(), jwtData.getEmail());
    claims.add(TokenClaims.AZP.claim(), jwtData.getAzp());
    claims.add(TokenClaims.AT_HASH.claim(), jwtData.getAt_hash());
    claims.add(TokenClaims.ROLE.claim(), jwtData.getRoles());
    claims.add(TokenClaims.AUTH_TYPE.claim(), jwtData.getType());
    claims.add(TokenClaims.CLIENT_ID.claim(), jwtData.getClient_id());
    claims.add(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes());

    long jwtExpiration = clientCredential.getJwtExpiration();
    Date now = new Date(System.currentTimeMillis());
    claims.add(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli());
    jwtData.setExp(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration());
    jwtData.setIat(now.toInstant().toEpochMilli());

    Date exp = new Date(now.toInstant().toEpochMilli() + jwtExpiration);

    SecretKey key = Keys.hmacShaKeyFor(clientCredential.getJwtSecret().getBytes());

    String accessToken =
        Jwts.builder().claims(claims.build()).issuedAt(now).expiration(exp).signWith(key).compact();

    // ID Token
    ClaimsBuilder idClaims = Jwts.claims().subject(jwtData.getSub());
    idClaims.add(TokenClaims.IDENTIFIER.claim(), jwtData.getIdentifier());
    idClaims.add(TokenClaims.ISS.claim(), jwtData.getIss());
    idClaims.add(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli());
    idClaims.add(TokenClaims.AUD.claim(), jwtData.getAud());
    idClaims.add(TokenClaims.EMAIL.claim(), jwtData.getEmail());
    idClaims.add(TokenClaims.AZP.claim(), jwtData.getAzp());
    idClaims.add(TokenClaims.NAME.claim(), jwtData.getName());
    idClaims.add(TokenClaims.PICTURE.claim(), jwtData.getPicture());
    idClaims.add(TokenClaims.GIVEN_NAME.claim(), jwtData.getGiven_name());
    idClaims.add(TokenClaims.FAMILY_NAME.claim(), jwtData.getFamily_name());
    idClaims.add(TokenClaims.AT_HASH.claim(), jwtData.getAt_hash());
    idClaims.add(TokenClaims.ROLE.claim(), jwtData.getRoles());
    idClaims.add(TokenClaims.AUTH_TYPE.claim(), jwtData.getType());
    idClaims.add(TokenClaims.CLIENT_ID.claim(), jwtData.getClient_id());
    idClaims.add(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes());

    String idToken =
        Jwts.builder().claims(idClaims.build()).expiration(exp).signWith(key).compact();

    // Refresh Token
    String refreshToken =
        generateRefreshToken(UUID.randomUUID().toString(), jwtData.getIdentifier());

    accessTokenService.save(
        jwtData,
        refreshToken,
        TokenUtils.hashingToken(accessToken),
        TokenUtils.hashingToken(idToken),
        clientCredential.getClientId(),
        sessionID.getSessionID(),
        jwtData.getRoles(),
        payload);

    return new AuthToken(
        clientCredential.getIdToken() ? idToken : null,
        clientCredential.getAccessToken() ? accessToken : null,
        refreshToken,
        now.toInstant().toEpochMilli() + jwtExpiration,
        jwtExpiration,
        "Bearer");
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private JWTData parseJWTToken(String token, ClientCredential clientCredential) {
    Claims body = null;
    SecretKey key = Keys.hmacShaKeyFor(clientCredential.getJwtSecret().getBytes());
    try {
      String tokenSplit = token.contains("Bearer") ? token.split("Bearer ")[1] : token;
      Jws<Claims> jwt = Jwts.parser().verifyWith(key).build().parseSignedClaims(tokenSplit);
      body = jwt.getPayload();
    } catch (JwtException e) {
      LOG.error("An error happen during parsing of JWT token, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    return new JWTData(
        (@NotNull String) body.get(TokenClaims.IDENTIFIER.claim()),
        String.join(", ", (Set) (body.get(TokenClaims.AUD.claim()))),
        (String) body.get(TokenClaims.AZP.claim()),
        (Long) body.get(TokenClaims.EXP.claim()),
        (Long) body.get(TokenClaims.IAT.claim()),
        (@NotNull String) body.get(TokenClaims.ISS.claim()),
        body.getSubject(),
        ObjectUtils.isEmpty(body.get(TokenClaims.NAME.claim()))
            ? null
            : (String) body.get(TokenClaims.NAME.claim()),
        (@NotNull String) body.get(TokenClaims.EMAIL.claim()),
        ObjectUtils.isEmpty(body.get(TokenClaims.PICTURE.claim()))
            ? null
            : (String) body.get(TokenClaims.PICTURE.claim()),
        ObjectUtils.isEmpty(body.get(TokenClaims.GIVEN_NAME.claim()))
            ? null
            : (String) body.get(TokenClaims.GIVEN_NAME.claim()),
        ObjectUtils.isEmpty(body.get(TokenClaims.FAMILY_NAME.claim()))
            ? null
            : (String) body.get(TokenClaims.FAMILY_NAME.claim()),
        (@NotNull String) body.get(TokenClaims.AT_HASH.claim()),
        true,
        ObjectUtils.isEmpty(body.get(TokenClaims.ROLE.claim()))
            ? null
            : Utils.mapper()
                .convertValue(body.get(TokenClaims.ROLE.claim()), new TypeReference<>() {}),
        ObjectUtils.isEmpty(body.get(TokenClaims.AUTH_TYPE.claim()))
            ? null
            : OAuthType.valueOf((String) body.get(TokenClaims.AUTH_TYPE.claim())),
        ObjectUtils.isEmpty(body.get(TokenClaims.CLIENT_ID.claim()))
            ? clientCredential.getClientId()
            : (String) body.get(TokenClaims.CLIENT_ID.claim()),
        ObjectUtils.isEmpty(body.get(TokenClaims.ATTRIBUTES.claim()))
            ? null
            : Utils.mapper()
                .convertValue(body.get(TokenClaims.ATTRIBUTES.claim()), new TypeReference<>() {}));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private AuthToken generateJWEToken(
      JWTData jwtData, ClientCredential clientCredential, Object payload) {
    long jweExpiration = clientCredential.getJweExpiration();
    Date now = new Date(System.currentTimeMillis());
    // Access Token
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(jwtData.getSub())
            .claim(TokenClaims.IDENTIFIER.claim(), jwtData.getIdentifier())
            .claim(TokenClaims.ISS.claim(), jwtData.getIss())
            .claim(TokenClaims.AUD.claim(), jwtData.getAud())
            .claim(TokenClaims.EMAIL.claim(), jwtData.getEmail())
            .claim(TokenClaims.AZP.claim(), jwtData.getAzp())
            .claim(TokenClaims.AT_HASH.claim(), jwtData.getAt_hash())
            .claim(TokenClaims.ROLE.claim(), jwtData.getRoles())
            .claim(TokenClaims.AUTH_TYPE.claim(), jwtData.getType())
            .claim(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes())
            .claim(TokenClaims.CLIENT_ID.claim(), jwtData.getClient_id())
            .claim(TokenClaims.IAT.claim(), new Date(now.toInstant().toEpochMilli()))
            .expirationTime(
                new Date(now.toInstant().toEpochMilli() + clientCredential.getJweExpiration()))
            .build();
    jwtData.setExp(now.toInstant().toEpochMilli() + clientCredential.getJweExpiration());
    jwtData.setIat(now.toInstant().toEpochMilli());
    Payload payloadAccess = new Payload(claimsSet.toJSONObject());

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512);

    byte[] secretKey = clientCredential.getJweSecret().getBytes();
    DirectEncrypter encrypted = null;
    JWEObject jweObject = new JWEObject(header, payloadAccess);
    try {
      encrypted = new DirectEncrypter(secretKey);
      jweObject.encrypt(encrypted);
    } catch (JOSEException e) {
      LOG.error("An error happen during generate JWE Token, message is {}", e.getMessage());
      throw new TokenException(e.getMessage());
    }
    String accessToken = jweObject.serialize();

    // Access Token
    JWTClaimsSet idClaimsSet =
        new JWTClaimsSet.Builder()
            .subject(jwtData.getSub())
            .claim(TokenClaims.IDENTIFIER.claim(), jwtData.getIdentifier())
            .claim(TokenClaims.ISS.claim(), jwtData.getIss())
            .claim(TokenClaims.AUD.claim(), jwtData.getAud())
            .claim(TokenClaims.EMAIL.claim(), jwtData.getEmail())
            .claim(TokenClaims.AZP.claim(), jwtData.getAzp())
            .claim(TokenClaims.AT_HASH.claim(), jwtData.getAt_hash())
            .claim(TokenClaims.ROLE.claim(), jwtData.getRoles())
            .claim(TokenClaims.AUTH_TYPE.claim(), jwtData.getType())
            .claim(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes())
            .claim(TokenClaims.IAT.claim(), new Date(now.toInstant().toEpochMilli()))
            .claim(TokenClaims.NAME.claim(), jwtData.getName())
            .claim(TokenClaims.PICTURE.claim(), jwtData.getPicture())
            .claim(TokenClaims.GIVEN_NAME.claim(), jwtData.getGiven_name())
            .claim(TokenClaims.FAMILY_NAME.claim(), jwtData.getFamily_name())
            .claim(TokenClaims.CLIENT_ID.claim(), jwtData.getClient_id())
            .expirationTime(
                new Date(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration()))
            .build();

    Payload payloadId = new Payload(idClaimsSet.toJSONObject());

    DirectEncrypter encryptedIds = null;
    JWEObject jweObjectId = new JWEObject(header, payloadId);
    try {
      encryptedIds = new DirectEncrypter(secretKey);
      jweObjectId.encrypt(encryptedIds);
    } catch (JOSEException e) {
      LOG.error("An error happen during generate JWE Token, message is {}", e.getMessage());
      throw new TokenException(e.getMessage());
    }
    String idToken = jweObject.serialize();

    String refreshToken =
        generateRefreshToken(UUID.randomUUID().toString(), jwtData.getIdentifier());

    accessTokenService.save(
        jwtData,
        refreshToken,
        TokenUtils.hashingToken(accessToken),
        TokenUtils.hashingToken(idToken),
        clientCredential.getClientId(),
        sessionID.getSessionID(),
        jwtData.getRoles(),
        payload);

    return new AuthToken(
        clientCredential.getIdToken() ? idToken : null,
        clientCredential.getAccessToken() ? accessToken : null,
        refreshToken,
        claimsSet.getExpirationTime().getTime(),
        jweExpiration,
        "Bearer");
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private JWTData parseJWEToken(String token, ClientCredential clientCredential) {
    try {
      String tokenSplit = token.split("Bearer ")[1];
      ConfigurableJWTProcessor<SimpleSecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      JWKSource<SimpleSecurityContext> jweKeySource =
          new ImmutableSecret<>(clientCredential.getJweSecret().getBytes());
      JWEKeySelector<SimpleSecurityContext> jweKeySelector =
          new JWEDecryptionKeySelector<>(
              JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512, jweKeySource);
      jwtProcessor.setJWEKeySelector(jweKeySelector);

      JWTClaimsSet claimsSet = jwtProcessor.process(tokenSplit, null);

      return new JWTData(
          (@NotNull String) claimsSet.getClaim(TokenClaims.IDENTIFIER.claim()),
          String.join(", ", (List) claimsSet.getClaim(TokenClaims.AUD.claim())),
          (String) claimsSet.getClaim(TokenClaims.AZP.claim()),
          ((Date) claimsSet.getClaim(TokenClaims.EXP.claim())).toInstant().toEpochMilli(),
          ((Date) claimsSet.getClaim(TokenClaims.IAT.claim())).toInstant().toEpochMilli(),
          (@NotNull String) claimsSet.getClaim(TokenClaims.ISS.claim()),
          claimsSet.getSubject(),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.NAME.claim()))
              ? null
              : (String) claimsSet.getClaim(TokenClaims.NAME.claim()),
          (@NotNull String) claimsSet.getClaim(TokenClaims.EMAIL.claim()),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.PICTURE.claim()))
              ? null
              : (String) claimsSet.getClaim(TokenClaims.PICTURE.claim()),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.GIVEN_NAME.claim()))
              ? null
              : (String) claimsSet.getClaim(TokenClaims.GIVEN_NAME.claim()),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.FAMILY_NAME.claim()))
              ? null
              : (String) claimsSet.getClaim(TokenClaims.FAMILY_NAME.claim()),
          (@NotNull String) claimsSet.getClaim(TokenClaims.AT_HASH.claim()),
          true,
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.ROLE.claim()))
              ? null
              : Utils.mapper()
                  .convertValue(
                      claimsSet.getClaim(TokenClaims.ROLE.claim()), new TypeReference<>() {}),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.AUTH_TYPE.claim()))
              ? null
              : OAuthType.valueOf((String) claimsSet.getClaim(TokenClaims.AUTH_TYPE.claim())),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.CLIENT_ID.claim()))
              ? clientCredential.getClientId()
              : (String) claimsSet.getClaim(TokenClaims.CLIENT_ID.claim()),
          ObjectUtils.isEmpty(claimsSet.getClaim(TokenClaims.ATTRIBUTES.claim()))
              ? null
              : Utils.mapper()
                  .convertValue(
                      claimsSet.getClaim(TokenClaims.ATTRIBUTES.claim()),
                      new TypeReference<>() {}));
    } catch (JOSEException | ParseException | BadJOSEException e) {
      LOG.error("An error happen during parsing JWE Token, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }
  }

  private String generateRefreshToken(String plain, String identifier) {
    String _message = identifier + "_" + plain + "_Access_Sphere";
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      LOG.error(
          "An error happen during the generation of the refresh token, message is {}",
          e.getMessage());
      throw new TokenException(e.getMessage());
    }
    messageDigest.update(_message.getBytes());
    return Base64.encodeBase64URLSafeString(messageDigest.digest());
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(testSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
