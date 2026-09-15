# Access-Sphere: Contesto Architetturale e Memoria (GEMINI.md)

## 1. Panoramica del Progetto & Missione

**Access-Sphere** è un Identity and Access Management (IAM) e server di autorizzazione OAuth 2.0 enterprise-ready
realizzato con **Spring Boot 3 (Reattivo / WebFlux)**, **R2DBC / Reactive Data**, e una console di gestione web moderna
basata su **HTML5 / Tailwind CSS / Vanilla JS** con design system Google Material Design 3 (M3).

---

## 2. Stack Tecnologico

- **Backend**: Java 22, Spring Boot 3.4.0 (WebFlux, Reactive Security, R2DBC, Reactor Core).
- **Frontend**: Template Thymeleaf & HTML statico (`src/main/resources/static/app/`), Tailwind CSS, tipografia Google
  Sans Flex, FontAwesome 6, SweetAlert2.
- **Dati & Cache**: Driver reattivi SQL / H2 / MariaDB / PostgreSQL, secret dei client cifrati, MFA TOTP.
- **Stile**: Material 3 glassmorphism (`m3-dashboard.css`, `m3-themes.css`).

---

## 3. Moduli Architetturali Principali

1. **Engine OAuth 2.0** (`io.github.giovannilamarmora.accesssphere.oAuth`):
    - Authorization code, client credentials, generazione token, refresh token, PKCE.
2. **User IAM** (`io.github.giovannilamarmora.accesssphere.data.user`):
    - Registrazione utenti, gestione stato, modifica profilo, password policy, ruoli e permessi.
3. **Client IAM** (`io.github.giovannilamarmora.accesssphere.client`):
    - Registrazione client OAuth 2.0, rotazione secret, scope, URI di redirect.
4. **Engine MFA** (`io.github.giovannilamarmora.accesssphere.mfa`):
    - Generazione secret TOTP, generazione codici QR, codici di recupero, autenticazione step-up.
5. **Impostazioni & Personalizzazione App** (`io.github.giovannilamarmora.accesssphere.settings`):
    - Branding dinamico dell'applicazione (Nome, Tagline, Logo, Favicon).
    - Motore dei temi (preset: Cosmic Purple, Emerald Matrix, Ocean Blue, Cyberpunk Sunset, Midnight Slate, Forest
      Minimal).
    - Lingua predefinita per l'internazionalizzazione (`it`, `en`, `auto`).
    - Personalizzazione vista di login (Showcase vs. Minimal, immagine di sfondo e opacità).
    - Backup & Restore dell'applicazione (Export e Import completo snapshot JSON).

---

## 4. Regole UI/UX e Principi Invarianti di Design

> **REGOLA CRITICA**: Non alterare, riprogettare, distorcere o ricostruire la struttura della UI a meno che non sia
> esplicitamente richiesto dall'utente. Mantenere una fedeltà pixel-perfect rispetto al design originale.

1. **Layout Hero della Dashboard e delle Impostazioni**:
    - L'Hero di benvenuto ("Benvenuto nella Console di Amministrazione") e l'Hero della vista Impostazioni risiedono direttamente sullo sfondo della pagina
      (`<div class="mb-8">`), NON racchiusi in una `.m3-bento-card` o in un riquadro con bordo.
    - Il lato destro dell'Hero Dashboard ospita il pill con l'orologio di sistema e il badge di stato del servizio; l'Hero Impostazioni ospita i pulsanti Ripristina Predefiniti e Salva Impostazioni.
2. **Bento Grid dei KPI**:
    - Le schede metriche (Utenti IAM, Client Registrati, MFA Attivo, ID Sessione) utilizzano `.m3-bento-card`.
    - **Invariante Dati Dinamici**: Non associare mai attributi `data-i18n` a elementi DOM il cui contenuto viene
      iniettato dinamicamente via JavaScript (es. `#stat-recap-session`, `#stat-recap-users`, `#stat-recap-clients`).
      Questo previene che `translateDOM()` sovrascriva i dati reali con etichette di traduzione statiche.
3. **Controlli di Azione & Header**:
    - I pulsanti di azione circolari usano `.m3-icon-btn`.
    - Il pulsante di Logout DEVE usare `.m3-icon-btn.m3-icon-btn-danger` con l'accento rosso/salmone corallo
      (`color: #FFB4AB`, `background: rgba(255, 180, 171, 0.08)`, `border: 1px solid rgba(255, 180, 171, 0.22)`).
    - L'header di TUTTE le pagine (`dashboard.html`, `users.html`, `clients.html`, `user.html`, `register.html`,
      `edit.html`, `roles.html`, `mfa.html`, `client.html`) deve contenere i tab standard di navigazione:
        - **Panoramica** (`/app`)
        - **Utenti** (`/app/users`)
        - **Client OAuth2** (`/app/clients`)
        - **Impostazioni** (`/app/settings`)
        - **API Docs** (link esterno a GitHub)
4. **Gestione Storage & Sessione**:
    - La funzione `cleanStorageAndCookies()` non deve MAI cancellare le preferenze utente e il branding
      dell'applicazione.
    - Chiavi del `localStorage` da preservare al logout o all'inizializzazione del login:
        - `access_sphere_theme`
        - `access_sphere_settings`
        - `access_sphere_language`
        - `app_language`
        - `access_sphere_app_name`
5. **Sincronizzazione Configurazione Backend**:
    - Al caricamento pagina, `loadPublicSettings()` richiama `/v1/app/settings/public` e applica tema attivo e lingua
      predefinita.
    - Alla modifica e salvataggio impostazioni, `saveAdminSettings()` aggiorna il backend e persiste le preferenze in
      `localStorage`.

---

## 5. Metodologia DOE (Direttiva, Orchestrazione, Esecuzione)

Per garantire precisione operativa e zero sprechi di token, ogni attività deve seguire rigorosamente il framework DOE:

1. **Direttiva (Directive)**:
    - Analizzare la richiesta dell'utente definendo chiaramente l'obiettivo, i vincoli e l'ambito di intervento.
    - Nessuna azione o generazione di codice prima di aver isolato il perimetro esatto.
2. **Orchestrazione (Orchestration)**:
    - Pianificare i passaggi sequenziali minimi necessari per raggiungere l'obiettivo.
    - Identificare i file specifici coinvolti evitando letture o modifiche a file non correlati.
3. **Esecuzione (Execution)**:
    - Implementare le modifiche in modo mirato e compatto.
    - Rispettare rigorosamente gli standard di codice, commenti e build.

---

## 6. Linee Guida di Sviluppo, Commenti & Risparmio Token

- **Commenti Dettagliati e Interconnessioni Obbligatori**:
    - Ogni classe, metodo, blocco logico, funzione JS o porzione complessa di codice introdotta o modificata DEVE
      includere commenti chiari, costanti e descrittivi (a livello di riga e di metodo/funzione).
    - I commenti devono spiegare non solo lo scopo del blocco, ma anche come funzioni e metodi sono **interconnessi**
      tra loro, evidenziando il flusso dei dati e le dipendenze logiche tra componenti.
    - Mantenere la codebase sempre documentata, auto-esplicativa e tracciabile.
- **Sincronizzazione della Documentazione (`README.md`)**:
    - Qualora un intervento introduca nuove feature, modifichi endpoint, aggiorni configurazioni, cambi la struttura dei
      moduli o alteri il comportamento architetturale, l'agente DEVE aggiornare contestualmente il file `README.md` (o i
      file di documentazione correlati) per riflettere lo stato attuale del sistema.
- **Regola di Build Condizionale**:
    - Eseguire la build (`mvn test`, `mvn clean compile`, ecc.) **SOLO ED ESCLUSIVAMENTE** se sono stati modificati,
      aggiunti o eliminati file **Java** (`.java`).
    - Se le modifiche riguardano solo file statici, HTML, CSS, JS, documentazione o risorse di configurazione frontend,
      **NON** avviare la build Maven.
- **Efficienza dei Token**:
    - Essere diretti e concisi nelle spiegazioni testuali: niente preamboli, convenevoli o ripetizioni inutili.
    - Evitare di stampare per intero file lunghi se le modifiche sono circoscritte, limitandosi ai soli blocchi e
      contesti rilevanti, **a meno che l'utente non richieda esplicitamente il file completo**.
- **Integrità & Scope**:
    - Non reintrodurre mai chiamate Stripe rimosse o dipendenze legacy fittizie.
    - Agire strettamente entro il perimetro richiesto dall'utente.