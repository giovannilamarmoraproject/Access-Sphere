package io.github.giovannilamarmora.accesssphere.utilities;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AESKeyGenerator {
  private static final String SECRET_KEY = "+i7uHT86/RkZc9DYtM+OHMChgjVUFKAIJIpNxC+tIi4=";

  public static String generateBase64AESKey() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256); // Usa 128, 192 o 256 a seconda del tuo livello di sicurezza
    SecretKey secretKey = keyGen.generateKey();
    return secretKey.getEncoded().toString();
  }

  public static void main(String[] args) throws Exception {
    String base64Key = CryptoUtils.generateBase64Key();
    System.out.println("🔐 AES Secret Key (Base64): " + base64Key + "\n");
    // String encoded = CryptoUtils.encrypt("DFQ4OCVXHGDXKJQJ6V2IYAPG5W3JJWPZ");
    // System.out.println("🔐 AES Secret Key Encoded (Base64): " + encoded + "\n");
  }
}
