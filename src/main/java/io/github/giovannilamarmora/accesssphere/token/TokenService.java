package io.github.giovannilamarmora.accesssphere.token;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.jsonwebtoken.*;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Logged
@Service
public class TokenService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AuthToken generateToken(
      User user, ClientCredential clientCredential, Map<String, Object> attributes) {
    switch (clientCredential.getTokenType()) {
      case BEARER_JWT -> {
        if (ObjectUtils.isEmpty(clientCredential.getJwtSecret())) {
          LOG.error("JWT Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, ExceptionMap.ERR_TOKEN_400.getMessage());
        }
        if (ObjectUtils.isEmpty(clientCredential.getJwtExpiration())) {
          LOG.error("JWT Expiration is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, ExceptionMap.ERR_TOKEN_400.getMessage());
        }
        return generateJWTToken(user, clientCredential, attributes);
      }
      case BEARER_JWE -> {
        if (ObjectUtils.isEmpty(clientCredential.getJweSecret())) {
          LOG.error("JWE Secret is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, ExceptionMap.ERR_TOKEN_400.getMessage());
        }
        if (ObjectUtils.isEmpty(clientCredential.getJweExpiration())) {
          LOG.error("JWE Expiration is not defined, please define them into the client");
          throw new TokenException(
              ExceptionMap.ERR_TOKEN_400, ExceptionMap.ERR_TOKEN_400.getMessage());
        }
        return generateJWEToken(user, clientCredential, attributes);
      }
      default -> {
        LOG.error("Token type is not defined, please define them into the client");
        throw new TokenException(
            ExceptionMap.ERR_TOKEN_400, ExceptionMap.ERR_TOKEN_400.getMessage());
      }
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private AuthToken generateJWTToken(
      User user, ClientCredential clientCredential, Map<String, Object> attributes) {
    ClaimsBuilder claims = Jwts.claims().subject(user.getUsername());
    claims.add(TokenClaims.ID.claim(), user.getId());
    claims.add(TokenClaims.ROLE.claim(), user.getRole());
    claims.add(TokenClaims.EMAIL.claim(), user.getEmail());
    if (!ObjectUtils.isEmpty(attributes)) claims.add(attributes);

    long dateExp = clientCredential.getJwtExpiration();
    Date exp = new Date(System.currentTimeMillis() + dateExp);

    SecretKey key = Keys.hmacShaKeyFor(clientCredential.getJwtSecret().getBytes());

    String token = Jwts.builder().claims(claims.build()).signWith(key).expiration(exp).compact();
    return new AuthToken(dateExp, "Bearer", token);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public User parseJWTToken(AuthToken token, String secret) throws UtilsException {
    Claims body = null;
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
    try {
      Jws<Claims> jwt =
          Jwts.parser().decryptWith(key).build().parseSignedClaims(token.getAccessToken());
      body = jwt.getPayload();
    } catch (JwtException e) {
      LOG.error("An error happen during parsing of JWT token, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    return new User(
        (@NotNull Long) body.get(TokenClaims.ID.claim()),
        body.getSubject(),
        (@NotNull String) body.get(TokenClaims.EMAIL.claim()),
        UserRole.valueOf((String) body.get(TokenClaims.ROLE.claim())));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private AuthToken generateJWEToken(
      User user, ClientCredential clientCredential, Map<String, Object> attributes) {
    // Crea un set di claims
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(user.getUsername())
            .claim(TokenClaims.ID.claim(), user.getId())
            .claim(TokenClaims.EMAIL.claim(), user.getEmail())
            .claim(TokenClaims.ROLE.claim(), user.getRole())
            .expirationTime(
                new Date(System.currentTimeMillis() + clientCredential.getJwtExpiration()))
            .build();
    if (!ObjectUtils.isEmpty(attributes)) claimsSet.getClaims().putAll(attributes);
    Payload payload = new Payload(claimsSet.toJSONObject());

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512);

    byte[] secretKey = clientCredential.getJwtSecret().getBytes();
    DirectEncrypter encrypted = null;
    JWEObject jweObject = new JWEObject(header, payload);
    try {
      encrypted = new DirectEncrypter(secretKey);
      jweObject.encrypt(encrypted);
    } catch (JOSEException e) {
      LOG.error("An error happen during generate JWE Token, message is {}", e.getMessage());
      throw new TokenException(e.getMessage());
    }
    String token = jweObject.serialize();

    return new AuthToken(claimsSet.getExpirationTime().getTime(), "Bearer", token);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public User parseJWEToken(AuthToken token, String secret) throws UtilsException {
    try {
      String tokenSplit = token.getAccessToken().split("Bearer ")[1];
      ConfigurableJWTProcessor<SimpleSecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      JWKSource<SimpleSecurityContext> jweKeySource = new ImmutableSecret<>(secret.getBytes());
      JWEKeySelector<SimpleSecurityContext> jweKeySelector =
          new JWEDecryptionKeySelector<>(
              JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512, jweKeySource);
      jwtProcessor.setJWEKeySelector(jweKeySelector);

      JWTClaimsSet claimsSet = jwtProcessor.process(tokenSplit, null);

      return new User(
          (Long) claimsSet.getClaim(TokenClaims.ID.claim()),
          claimsSet.getSubject(),
          (String) claimsSet.getClaim(TokenClaims.EMAIL.claim()),
          UserRole.valueOf((String) claimsSet.getClaim(TokenClaims.ROLE.claim())));
    } catch (JOSEException | ParseException | BadJOSEException e) {
      LOG.error("An error happen during parsing JWE Token, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, e.getMessage());
    }
  }
}
