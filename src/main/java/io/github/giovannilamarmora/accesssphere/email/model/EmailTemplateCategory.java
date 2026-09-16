package io.github.giovannilamarmora.accesssphere.email.model;

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
      "Reimpostazione Password",
      "Invio del codice di sicurezza o link per il ripristino della password dimenticata",
      true,
      createResetPasswordParams()),

  WELCOME_USER(
      "WELCOME_USER",
      "e4589c6a-9ccc-4c65-a000-302da1ebcfac",
      "Benvenuto Utente",
      "Email di benvenuto e conferma attivazione nuovo account utente",
      false,
      createWelcomeParams()),

  EMAIL_VERIFICATION(
      "EMAIL_VERIFICATION",
      "9cfc8a62-9989-4776-aa5d-b7d3b3765534",
      "Verifica Indirizzo Email",
      "Invio del codice di verifica per confermare l'indirizzo email",
      false,
      createVerificationParams()),

  MFA_OTP(
      "MFA_OTP",
      "f5b7de80-7057-4a3c-8e63-c81a8ce04eaa",
      "Codice OTP MFA",
      "Invio del codice monouso per l'autenticazione a due fattori via email",
      false,
      createMfaOtpParams()),

  ACCOUNT_LOCKED(
      "ACCOUNT_LOCKED",
      "b26d7eee-efd4-49d9-8eab-d25c5d2f894a",
      "Avviso Account Bloccato",
      "Notifica all'utente in seguito a troppi tentativi falliti di autenticazione",
      false,
      createAccountLockedParams());

  private final String code;
  private final String defaultIdentifier;
  private final String defaultTitle;
  private final String defaultDescription;
  private final boolean activeInCode;
  private final Map<String, Object> defaultParams;

  EmailTemplateCategory(
      String code,
      String defaultIdentifier,
      String defaultTitle,
      String defaultDescription,
      boolean activeInCode,
      Map<String, Object> defaultParams) {
    this.code = code;
    this.defaultIdentifier = defaultIdentifier;
    this.defaultTitle = defaultTitle;
    this.defaultDescription = defaultDescription;
    this.activeInCode = activeInCode;
    this.defaultParams = defaultParams;
  }

  public static EmailTemplateCategory fromCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      return RESET_PASSWORD;
    }
    for (EmailTemplateCategory cat : values()) {
      if (cat.getCode().equalsIgnoreCase(code.trim())
          || cat.name().equalsIgnoreCase(code.trim())
          || cat.getDefaultIdentifier().equalsIgnoreCase(code.trim())
          || (cat == RESET_PASSWORD
              && "ACCESS_SPHERE_CODE_RESET_PASSWORD".equalsIgnoreCase(code.trim()))
          || (cat == WELCOME_USER
              && "ACCESS_SPHERE_WELCOME_USER".equalsIgnoreCase(code.trim()))
          || (cat == EMAIL_VERIFICATION
              && "ACCESS_SPHERE_EMAIL_VERIFICATION".equalsIgnoreCase(code.trim()))
          || (cat == MFA_OTP
              && "ACCESS_SPHERE_MFA_OTP".equalsIgnoreCase(code.trim()))
          || (cat == ACCOUNT_LOCKED
              && "ACCESS_SPHERE_ACCOUNT_LOCKED".equalsIgnoreCase(code.trim()))) {
        return cat;
      }
    }
    return RESET_PASSWORD;
  }

  private static Map<String, Object> createResetPasswordParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "https://accesssphere.io");
    params.put("NAME", "Mario Rossi");
    params.put("RESET_CODE", "123456");
    params.put("SITE_SHORT", "Access Sphere");
    return params;
  }

  private static Map<String, Object> createWelcomeParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "https://accesssphere.io");
    params.put("NAME", "Mario Rossi");
    params.put("USERNAME", "mario.rossi");
    params.put("SITE_SHORT", "Access Sphere");
    return params;
  }

  private static Map<String, Object> createVerificationParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("SITE_URL", "https://accesssphere.io");
    params.put("NAME", "Mario Rossi");
    params.put("VERIFICATION_CODE", "987654");
    params.put("SITE_SHORT", "Access Sphere");
    return params;
  }

  private static Map<String, Object> createMfaOtpParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("NAME", "Mario Rossi");
    params.put("OTP_CODE", "654321");
    params.put("EXPIRATION_MINUTES", "10");
    params.put("SITE_SHORT", "Access Sphere");
    return params;
  }

  private static Map<String, Object> createAccountLockedParams() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("NAME", "Mario Rossi");
    params.put("SUPPORT_URL", "https://accesssphere.io/support");
    params.put("LOCK_REASON", "Troppi tentativi errati");
    params.put("SITE_SHORT", "Access Sphere");
    return params;
  }
}
