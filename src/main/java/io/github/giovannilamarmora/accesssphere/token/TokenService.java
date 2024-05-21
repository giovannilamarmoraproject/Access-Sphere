package io.github.giovannilamarmora.accesssphere.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Logged
@Service
public class TokenService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  @Autowired private AccessTokenService accessTokenService;

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
    claims.add(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes());

    long jwtExpiration = clientCredential.getJwtExpiration();
    Date now = new Date(System.currentTimeMillis());
    claims.add(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli());
    jwtData.setExp(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration());
    jwtData.setIat(now.toInstant().toEpochMilli());

    Date exp = new Date(now.toInstant().toEpochMilli() + jwtExpiration);

    SecretKey key = Keys.hmacShaKeyFor(clientCredential.getJwtSecret().getBytes());

    String accessToken =
        Jwts.builder().claims(claims.build()).expiration(exp).signWith(key).compact();

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
    idClaims.add(TokenClaims.ATTRIBUTES.claim(), jwtData.getAttributes());

    String idToken =
        Jwts.builder().claims(idClaims.build()).expiration(exp).signWith(key).compact();

    // Refresh Token
    String refreshToken =
        generateRefreshToken(UUID.randomUUID().toString(), jwtData.getIdentifier());

    accessTokenService.save(jwtData, refreshToken, now.toInstant().toEpochMilli(), payload);

    return new AuthToken(
        idToken,
        accessToken,
        refreshToken,
        now.toInstant().toEpochMilli() + jwtExpiration,
        jwtExpiration,
        "Bearer");
  }

  // @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  // public User parseJWTToken(AuthToken token, String secret) throws UtilsException {
  //   Claims body = null;
  //   SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
  //   try {
  //     Jws<Claims> jwt =
  //         Jwts.parser().decryptWith(key).build().parseSignedClaims(token.getAccessToken());
  //     body = jwt.getPayload();
  //   } catch (JwtException e) {
  //     LOG.error("An error happen during parsing of JWT token, message is {}", e.getMessage());
  //     throw new TokenException(ExceptionMap.ERR_TOKEN_401,
  // ExceptionMap.ERR_TOKEN_401.getMessage());
  //   }

  //   return new User(
  //       (@NotNull String) body.get(TokenClaims.IDENTIFIER.claim()),
  //       body.getSubject(),
  //       (@NotNull String) body.get(TokenClaims.EMAIL.claim()),
  //       UserRole.valueOf((String) body.get(TokenClaims.ROLE.claim())));
  // }

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
            .claim(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli())
            .expirationTime(
                new Date(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration()))
            .build();
    jwtData.setExp(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration());
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
            .claim(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli())
            .claim(TokenClaims.IAT.claim(), now.toInstant().toEpochMilli())
            .claim(TokenClaims.NAME.claim(), jwtData.getName())
            .claim(TokenClaims.PICTURE.claim(), jwtData.getPicture())
            .claim(TokenClaims.GIVEN_NAME.claim(), jwtData.getGiven_name())
            .claim(TokenClaims.FAMILY_NAME.claim(), jwtData.getFamily_name())
            .expirationTime(
                new Date(now.toInstant().toEpochMilli() + clientCredential.getJwtExpiration()))
            .build();

    Payload payloadId = new Payload(claimsSet.toJSONObject());

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

    accessTokenService.save(jwtData, refreshToken, now.toInstant().toEpochMilli(), payload);

    return new AuthToken(
        idToken,
        accessToken,
        refreshToken,
        claimsSet.getExpirationTime().getTime(),
        jweExpiration,
        "Bearer");
  }

  // @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  // public User parseJWEToken(AuthToken token, String secret) throws UtilsException {
  //  try {
  //    String tokenSplit = token.getAccessToken().split("Bearer ")[1];
  //    ConfigurableJWTProcessor<SimpleSecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
  //    JWKSource<SimpleSecurityContext> jweKeySource = new ImmutableSecret<>(secret.getBytes());
  //    JWEKeySelector<SimpleSecurityContext> jweKeySelector =
  //        new JWEDecryptionKeySelector<>(
  //            JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512, jweKeySource);
  //    jwtProcessor.setJWEKeySelector(jweKeySelector);

  //    JWTClaimsSet claimsSet = jwtProcessor.process(tokenSplit, null);

  //    return new User(
  //        (String) claimsSet.getClaim(TokenClaims.IDENTIFIER.claim()),
  //        claimsSet.getSubject(),
  //        (String) claimsSet.getClaim(TokenClaims.EMAIL.claim()),
  //        UserRole.valueOf((String) claimsSet.getClaim(TokenClaims.ROLE.claim())));
  //  } catch (JOSEException | ParseException | BadJOSEException e) {
  //    LOG.error("An error happen during parsing JWE Token, message is {}", e.getMessage());
  //    throw new TokenException(ExceptionMap.ERR_TOKEN_401, e.getMessage());
  //  }
  // }

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
}
