package io.github.giovannilamarmora.accesssphere.email.template.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Categorie applicative strutturate per i Template Email di Access Sphere. Collega ciascun template
 * a un preciso scopo del flusso di autenticazione e sicurezza, fornendo i parametri segnaposto
 * standard e indicando se il flusso e attualmente attivo nel codice.
 */
@Getter
public enum EmailTemplateCategory {
  RESET_PASSWORD(
      "RESET_PASSWORD",
      "c7b2a5d1-9f3e-4b68-8a21-7e8c3f4a1d95",
      "ACCESS_SPHERE_CODE_RESET_PASSWORD",
      "Reimpostazione Password",
      "Reset Password",
      "Reset Password",
      "Invio del codice di sicurezza o link per il ripristino della password dimenticata",
      "Template for sending password reset code",
      "reset-password.html",
      true,
      createResetPasswordParams()),

  WELCOME_USER(
      "WELCOME_USER",
      "e4589c6a-9ccc-4c65-a000-302da1ebcfac",
      "ACCESS_SPHERE_WELCOME_USER",
      "Benvenuto Utente",
      "Welcome User",
      "Welcome to Access Sphere",
      "Email di benvenuto e conferma attivazione nuovo account utente",
      "Template for welcoming new users and confirming account registration",
      "welcome-user.html",
      false,
      createWelcomeParams()),

  EMAIL_VERIFICATION(
      "EMAIL_VERIFICATION",
      "9cfc8a62-9989-4776-aa5d-b7d3b3765534",
      "ACCESS_SPHERE_EMAIL_VERIFICATION",
      "Verifica Indirizzo Email",
      "Email Verification",
      "Verify your email address",
      "Invio del link di conferma per verificare l'indirizzo email e attivare l'account",
      "Template for sending email verification link to confirm user account",
      "email-verification.html",
      false,
      createVerificationParams()),

  MFA_OTP(
      "MFA_OTP",
      "f5b7de80-7057-4a3c-8e63-c81a8ce04eaa",
      "ACCESS_SPHERE_MFA_OTP",
      "Codice OTP MFA",
      "MFA OTP Code",
      "Your MFA security code",
      "Invio del codice monouso per l'autenticazione a due fattori via email",
      "Template for sending one-time MFA authentication code",
      "mfa-otp.html",
      false,
      createMfaOtpParams()),

  ACCOUNT_LOCKED(
      "ACCOUNT_LOCKED",
      "b26d7eee-efd4-49d9-8eab-d25c5d2f894a",
      "ACCESS_SPHERE_ACCOUNT_LOCKED",
      "Avviso Account Bloccato",
      "Account Locked Alert",
      "Security alert: Account temporarily locked",
      "Notifica all'utente in seguito a troppi tentativi falliti di autenticazione",
      "Notification to user following excessive failed authentication attempts",
      "account-locked.html",
      false,
      createAccountLockedParams());

  private final String code;
  private final String defaultIdentifier;
  private final String legacyIdentifier;
  private final String defaultTitle;
  private final String englishName;
  private final String defaultSubject;
  private final String defaultDescription;
  private final String englishDescription;
  private final String templateFileName;
  private final boolean activeInCode;
  private final Map<String, Object> defaultParams;

  EmailTemplateCategory(
      String code,
      String defaultIdentifier,
      String legacyIdentifier,
      String defaultTitle,
      String englishName,
      String defaultSubject,
      String defaultDescription,
      String englishDescription,
      String templateFileName,
      boolean activeInCode,
      Map<String, Object> defaultParams) {
    this.code = code;
    this.defaultIdentifier = defaultIdentifier;
    this.legacyIdentifier = legacyIdentifier;
    this.defaultTitle = defaultTitle;
    this.englishName = englishName;
    this.defaultSubject = defaultSubject;
    this.defaultDescription = defaultDescription;
    this.englishDescription = englishDescription;
    this.templateFileName = templateFileName;
    this.activeInCode = activeInCode;
    this.defaultParams = defaultParams;
  }

  public static EmailTemplateCategory fromCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      return RESET_PASSWORD;
    }
    String trimmed = code.trim();
    for (EmailTemplateCategory cat : values()) {
      if (cat.getCode().equalsIgnoreCase(trimmed)
          || cat.name().equalsIgnoreCase(trimmed)
          || cat.getDefaultIdentifier().equalsIgnoreCase(trimmed)
          || (cat.getLegacyIdentifier() != null
              && cat.getLegacyIdentifier().equalsIgnoreCase(trimmed))) {
        return cat;
      }
    }
    return RESET_PASSWORD;
  }

  /**
   * Converte la mappa dei parametri predefiniti in una Map&lt;String, String&gt; idonea per la
   * persistenza nell'entita del template email.
   *
   * @return Mappa chiave-valore di stringhe
   */
  public Map<String, String> getDefaultParamsAsStringMap() {
    Map<String, String> stringMap = new LinkedHashMap<>();
    if (this.defaultParams != null) {
      this.defaultParams.forEach((k, v) -> stringMap.put(k, v != null ? v.toString() : ""));
    }
    return stringMap;
  }

  private static Map<String, Object> createResetPasswordParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "APP.URL");
    params.put("NAME", "USER.NAME");
    params.put("RESET_CODE", "AUTH.RESET_CODE");
    params.put("SITE_SHORT", "APP.NAME");
    return params;
  }

  private static Map<String, Object> createWelcomeParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "APP.URL");
    params.put("NAME", "USER.NAME");
    params.put("USERNAME", "USER.USERNAME");
    params.put("SITE_SHORT", "APP.NAME");
    return params;
  }

  private static Map<String, Object> createVerificationParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "APP.URL");
    params.put("NAME", "USER.NAME");
    params.put("VERIFY_URL", "AUTH.VERIFY_URL");
    params.put("SITE_SHORT", "APP.NAME");
    return params;
  }

  private static Map<String, Object> createMfaOtpParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "APP.URL");
    params.put("NAME", "USER.NAME");
    params.put("OTP_CODE", "AUTH.OTP_CODE");
    params.put("EXPIRATION_MINUTES", "AUTH.EXPIRE_MINUTES");
    params.put("SITE_SHORT", "APP.NAME");
    return params;
  }

  private static Map<String, Object> createAccountLockedParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "APP.URL");
    params.put("NAME", "USER.NAME");
    params.put("LOCK_REASON", "AUTH.LOCK_REASON");
    params.put("SUPPORT_URL", "APP.SUPPORT_URL");
    params.put("SITE_SHORT", "APP.NAME");
    return params;
  }
}
