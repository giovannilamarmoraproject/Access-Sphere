package io.github.giovannilamarmora.accesssphere.utilities;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptoUtils {

  @Value("${rest.client.strapi.aes-key}")
  private String aesKey;

  private static String staticAesKey;

  private static final String ALGORITHM = "AES";

  @PostConstruct
  public void init() {
    staticAesKey = aesKey;
  }

  // Generazione sicura della chiave
  public static String generateBase64Key() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
    keyGen.init(256); // 128, 192, 256
    SecretKey secretKey = keyGen.generateKey();
    return Base64.getEncoder().encodeToString(secretKey.getEncoded());
  }

  // Uso per criptare
  public static String encrypt(String data) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(staticAesKey);
      SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (Exception e) {
      throw new RuntimeException("Errore durante la cifratura", e);
    }
  }

  public static String decrypt(String encryptedData) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(staticAesKey);
      SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
      byte[] decryptedBytes = cipher.doFinal(decodedBytes);
      return new String(decryptedBytes);
    } catch (Exception e) {
      throw new RuntimeException("Errore durante la decifratura", e);
    }
  }
}
