package io.github.giovannilamarmora.accesssphere.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QRCodeUtilsTest {

  @Test
  @DisplayName("Test generateQRCodeWithDefaultLogo generates valid PNG with embedded logo")
  void testGenerateQRCodeWithDefaultLogo() throws Exception {
    String testOtpAuth = "otpauth://totp/Access-Sphere:test@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Access-Sphere";
    byte[] qrBytes = QRCodeUtils.generateQRCodeWithDefaultLogo(testOtpAuth, 300, 300);

    assertNotNull(qrBytes);
    assertTrue(qrBytes.length > 0);

    // Verify it is a valid PNG image
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(qrBytes));
    assertNotNull(img);
    assertEquals(300, img.getWidth());
    assertEquals(300, img.getHeight());
  }

  @Test
  @DisplayName("Test generateQRCodeImage generates valid basic QR code")
  void testGenerateQRCodeImage() throws Exception {
    String testContent = "https://example.com";
    byte[] qrBytes = QRCodeUtils.generateQRCodeImage(testContent, 200, 200);

    assertNotNull(qrBytes);
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(qrBytes));
    assertNotNull(img);
    assertEquals(200, img.getWidth());
    assertEquals(200, img.getHeight());
  }

  @Test
  @DisplayName("Test generateCustomQRCode generates valid custom QR code")
  void testGenerateCustomQRCode() throws Exception {
    String testContent = "test-custom-data";
    byte[] qrBytes = QRCodeUtils.generateCustomQRCode(testContent, 250, 250);

    assertNotNull(qrBytes);
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(qrBytes));
    assertNotNull(img);
    assertEquals(250, img.getWidth());
    assertEquals(250, img.getHeight());
  }
}
