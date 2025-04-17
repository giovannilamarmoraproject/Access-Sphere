package io.github.giovannilamarmora.accesssphere.mfa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MFASetupResponse(String secret, String otpAuth, String base64QrCodeImage) {

  public MFASetupResponse addQRCode(String qrCode) {
    return new MFASetupResponse(secret, otpAuth, qrCode);
  }
}
