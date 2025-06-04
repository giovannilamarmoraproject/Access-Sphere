package io.github.giovannilamarmora.accesssphere.utilities;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class QRCodeUtils {
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static byte[] generateQRCodeImage(String text, int width, int height) {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
      ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
      return pngOutputStream.toByteArray();
    } catch (WriterException | java.io.IOException e) {
      throw new MFAException(ExceptionMap.ERR_MFA_400, "❌ Error during generation of QRCode!");
    }
  }

  public static byte[] generateCustomQRCode(String data, int width, int height) {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.MARGIN, 1); // riduci il margine
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

      BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

      // Personalizza i colori
      int foregroundColor = 0xFF1F1F1F; // dark gray
      int backgroundColor = 0xFFFFFFFF; // white

      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          boolean bit = bitMatrix.get(x, y);
          image.setRGB(x, y, bit ? foregroundColor : backgroundColor);
        }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("❌ QR Code generation failed", e);
    }
  }

  public static byte[] generateQRCodeWithLogo(
      String qrContent, int width, int height, InputStream logoInputStream) {
    try {
      // Genera il QR code base
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.MARGIN, 1);
      hints.put(
          EncodeHintType.ERROR_CORRECTION,
          ErrorCorrectionLevel.H); // livello alto per tolleranza logo

      BitMatrix bitMatrix =
          qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height, hints);
      BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      int qrColor = 0xFF1F1F1F; // scuro
      int bgColor = 0xFFFFFFFF; // bianco

      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          qrImage.setRGB(x, y, bitMatrix.get(x, y) ? qrColor : bgColor);
        }
      }

      // Carica il logo
      BufferedImage logo = ImageIO.read(logoInputStream);

      // Calcola dimensione e posizione del logo
      int logoWidth = width / 5;
      int logoHeight = height / 5;
      int logoX = (width - logoWidth) / 2;
      int logoY = (height - logoHeight) / 2;

      // Ridimensiona il logo
      Image scaledLogo = logo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

      // Sovrapponi il logo
      Graphics2D g = qrImage.createGraphics();
      g.drawImage(scaledLogo, logoX, logoY, null);
      g.dispose();

      // Converti in byte[]
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(qrImage, "png", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("❌ QR Code with logo generation failed", e);
    }
  }

  public static byte[] generateQRCodeWithLogoFromUrl(
      String qrContent, int width, int height, String logoUrl) {
    try {
      // Genera il QR code base
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.MARGIN, 2);
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

      BitMatrix bitMatrix =
          qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height, hints);
      BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      int qrColor = 0xFF1F1F1F;
      int bgColor = 0xFFFFFFFF;

      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          qrImage.setRGB(x, y, bitMatrix.get(x, y) ? qrColor : bgColor);
        }
      }

      // Carica il logo da URL
      BufferedImage logo = ImageIO.read(URI.create(logoUrl).toURL());

      // Calcola dimensione e posizione del logo
      // Modifica del calcolo della dimensione del logo per ridurlo ulteriormente
      int logoWidth = width / 4; // Riduci da 5 a 10 per un logo più piccolo
      int logoHeight = height / 4; // Riduci da 5 a 10 per un logo più piccolo

      int logoX = (width - logoWidth) / 2;
      int logoY = (height - logoHeight) / 2;

      // Ridimensiona il logo
      Image scaledLogo = logo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

      // Sovrapponi il logo
      Graphics2D g = qrImage.createGraphics();
      g.drawImage(scaledLogo, logoX, logoY, null);
      g.dispose();

      // Converte in byte[]
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(qrImage, "png", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("❌ QR Code with logo from URL generation failed", e);
    }
  }
}
