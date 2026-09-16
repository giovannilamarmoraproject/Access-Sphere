package io.github.giovannilamarmora.accesssphere.email.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Categorie applicative strutturate per i Template Email di Access Sphere.
 * Collega ciascun template a un preciso scopo del flusso di autenticazione e sicurezza,
 * fornendo i parametri segnaposto standard e indicando se il flusso e attualmente attivo nel codice.
 */
@Getter
public enum EmailTemplateCategory {
  RESET_PASSWORD(
      "RESET_PASSWORD",
      "ACCESS_SPHERE_CODE_RESET_PASSWORD",
      "Reimpostazione Password",
      "Invio del codice di sicurezza o link per il ripristino della password dimenticata",
      true,
      createResetPasswordParams()),

  WELCOME_USER(
      "WELCOME_USER",
      "ACCESS_SPHERE_WELCOME_USER",
      "Benvenuto Utente",
      "Email di benvenuto e conferma attivazione nuovo account utente",
      false,
      createWelcomeParams()),

  EMAIL_VERIFICATION(
      "EMAIL_VERIFICATION",
      "ACCESS_SPHERE_EMAIL_VERIFICATION",
      "Verifica Indirizzo Email",
      "Invio del codice di verifica per confermare l'indirizzo email",
      false,
      createVerificationParams()),

  MFA_OTP(
      "MFA_OTP",
      "ACCESS_SPHERE_MFA_OTP",
      "Codice OTP MFA",
      "Invio del codice monouso per l'autenticazione a due fattori via email",
      false,
      createMfaOtpParams()),

  ACCOUNT_LOCKED(
      "ACCOUNT_LOCKED",
      "ACCESS_SPHERE_ACCOUNT_LOCKED",
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
          || cat.getDefaultIdentifier().equalsIgnoreCase(code.trim())) {
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
