# Walkthrough: Email Template Management, Live Preview & Strapi Replacement

Abbiamo completato l'implementazione della nuova sezione **Email** con gestione completa dei template su database, live preview reattivo split-screen, estrazione automatica dei parametri segnaposto, collaudo diretto con invio email di test e la completa sostituzione della chiamata legacy a Strapi per il flusso di reimpostazione della password.

---

## 🚀 Modifiche Principali

### 1. Backend: Gestione Template Email & Sostituzione Strapi
- **Entità & DAO (`EMAIL_TEMPLATES`)**:
  - Creata [`EmailTemplateEntity.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/email/entity/EmailTemplateEntity.java) con campi `identifier`, `name`, `subject`, `description`, `template` (LONGTEXT), `params` (JSON Map con converter Jackson) e `locale`.
  - Creato [`IEmailTemplateDAO.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/email/IEmailTemplateDAO.java).
- **Service & Smart Multilingual Fallback**:
  - Implementato in [`EmailTemplateService.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/email/EmailTemplateService.java) un algoritmo di fallback a 4 livelli:
    1. Corrispondenza esatta per locale (es. `it-IT` o `it`).
    2. Corrispondenza per prefisso lingua (es. `it` per `it-IT`).
    3. Template globale/predefinito (`locale == null`).
    4. Primo template disponibile per quell'identificatore (garantisce supporto anche se l'amministratore configura il template in una sola lingua).
- **Controller REST (`/v1/email-templates`)**:
  - `GET /v1/email-templates`: elenco ordinato dei template.
  - `GET /v1/email-templates/{id}`: dettaglio template.
  - `GET /v1/email-templates/identifier/{identifier}`: ricerca con locale fallback.
  - `POST /v1/email-templates`: creazione template.
  - `PUT /v1/email-templates`: aggiornamento template.
  - `DELETE /v1/email-templates/{id}`: eliminazione template.
  - `POST /v1/email-templates/send-test`: esecuzione invio email di test tramite `EmailSenderClient`.
- **Inizializzazione & Seeding Automatico**:
  - In [`ProjectInitializerService.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/config/ProjectInitializerService.java), censito all'avvio a DB vuoto il template `ACCESS_SPHERE_CODE_RESET_PASSWORD` con il codice HTML responsive e i relativi parametri (`SITE_URL`, `NAME`, `RESET_CODE`, `SITE_SHORT`).
- **Sostituzione Chiamata Strapi in `UserService`**:
  - In [`UserService.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/data/user/UserService.java) nel metodo `changePasswordRequest`, la precedente chiamata `strapiService.getTemplateById` è stata sostituita da `emailTemplateService.getTemplateByIdentifierAndLocale(templateIdentifier, finalLocale)`.

---

### 2. Frontend: Nuova UI Email & Live Preview Editor
- **Nuova Pagina (`/app/emails`)**:
  - Creata [`emails.html`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/resources/static/app/email/emails.html) con design system Material Expressive 3 glassmorphism, hero header (`<div class="mb-8">`), Bento Grid con 4 KPI cards (`stat-total-templates`, `stat-total-locales`, `stat-total-params`, `stat-reset-pwd-status`), e toolbar di ricerca/filtro per lingua.
- **Editor Split-View con Live Preview Iframe**:
  - Colonna sinistra: form metadati, gestore dinamico di parametri segnaposto (`KEY` -> `valore d'esempio`), pulsante *"Rileva da HTML"* con regex scanning, e textarea codice HTML monospace.
  - Colonna destra: rendering in tempo reale dentro un iframe isolato con toggle desktop/mobile frame e sostituzione opzionale dei parametri {{KEY}}.
- **Modale Invio Email di Test**:
  - Modale interattiva per l'invio istantaneo di prova: genera dinamicamente gli input per ogni parametro configurato nel template e chiama `POST /v1/email-templates/send-test`.
- **Script Controller**:
  - Creato [`emails.js`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/resources/static/app/email/js/emails.js) per tutta la logica asincrona, interfacciamento REST, debounce preview e feedback SweetAlert2.
- **Routing & Navigazione**:
  - Aggiunto mapping `/app/emails` in [`RedirectControllerImpl.java`](file:///c:/Users/giola/Documents/Personal%20Project/Access-Sphere/src/main/java/io/github/giovannilamarmora/accesssphere/RedirectControllerImpl.java).
  - Aggiunto il tab "Template Email" negli header di tutte le viste (`dashboard.html`, `users.html`, `clients.html`, `client.html`, `edit.html`, `register.html`, `roles.html`, `mfa.html`, `user.html`) e nella M3 Bento Sidebar in `settings.js`.
- **Internazionalizzazione (i18n)**:
  - Sincronizzate le chiavi di traduzione in `it.json`, `en.json`, `fr.json`, `es.json` e `de.json`.

---

## 🧪 Validazione & Test

- Esecuzione `mvn test` con Amazon Corretto JDK 22:
  - **44 test eseguiti**, **0 fallimenti**, **0 errori**.
  - Test unitari per `EmailTemplateService` superati con verifica di tutti i flussi di fallback e invio test.
  - Test di `ProjectInitializerService` superati con verifica del seeding DB.
  - Test di `UserService` superati con verifica del flusso `changePasswordRequest`.
