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
    - L'Hero di benvenuto ("Benvenuto nella Console di Amministrazione") e l'Hero della vista Impostazioni risiedono
      direttamente sullo sfondo della pagina (`<div class="mb-8">`), NON racchiusi in una `.m3-bento-card` o in un
      riquadro con bordo.
    - Il lato destro dell'Hero Dashboard ospita il pill con l'orologio di sistema e il badge di stato del servizio;
      l'Hero Impostazioni ospita i pulsanti Ripristina Predefiniti e Salva Impostazioni.
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
      `edit.html`, `roles.html`, `mfa.html`, `client.html`, `emails.html`, `role.html`) deve contenere la barra di navigazione unificata a macro-aree con dropdown M3:
        - **Panoramica** (`/app`)
        - **Identità & Accessi** (dropdown): Utenti (`/app/users`), Nuovo Utente (`/app/users/register`), Ruoli (`/app/role`)
        - **Applicazioni** (dropdown): Client OAuth2 (`/app/clients`), Nuovo Client (`/app/clients/register`)
        - **Sistema** (dropdown): Template Email (`/app/emails`), Impostazioni (`/app/settings`), API Docs (`/webjars/swagger-ui/index.html`)
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
6. **Conformità Cromatica e Rispetto Rigoroso della Palette**:
    - Ogni singolo elemento, componente, testo, bordo, badge, modale o icona aggiunto o modificato nella UI DEVE
      rispettare rigorosamente la palette colori del design system Material Design 3 e i token definiti nei file di
      stile (`m3-dashboard.css`, `m3-themes.css`).
    - È fatto divieto assoluto di introdurre colori hex, rgb o classi arbitrarie hardcoded che rompano l'armonia
      cromatica o che non si adattino al tema dinamico attivo (es. Cosmic Purple, Emerald Matrix, Ocean Blue, ecc.).
    - Utilizzare esclusivamente le variabili CSS del tema (es. `--md-sys-color-primary`, `--md-sys-color-surface`,
      `--md-sys-color-outline`, ecc.) o le classi di utility coerenti con lo schema grafico esistente.
7. **Internazionalizzazione Obbligatoria (i18n)**:
    - **Nessun testo statico hardcoded**: Ogni singolo elemento testuale introdotto o modificato nella UI (titoli,
      etichette, pulsanti, placeholder, messaggi di tooltip, toast, modali SweetAlert2) DEVE essere predisposto per
      l'internazionalizzazione.
    - Nel template HTML, associare sempre il relativo attributo di traduzione (es. `data-i18n="chiave_traduzione"` o
      `data-i18n-placeholder="chiave_placeholder"`).
    - Contestualmente a ogni modifica frontend, l'agente DEVE aggiornare i dizionari/file di lingua (es. `it`, `en`)
      aggiungendo le nuove chiavi con i relativi testi tradotti, mantenendo la coerenza terminologica del sistema IAM.

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
- **Convenzione Messaggi di Commit Git (Solo Suggerimento) in Inglese**:
    - L'agente **NON deve MAI eseguire autonomamente il commit Git** (`git commit`).
    - Al termine di ogni implementazione o modifica, l'agente deve **stampare nella risposta** il messaggio di commit
      pronto per essere copiato dall'utente, formattato in un blocco di codice:
      ```text
      <tipo>(<ambito>): <emoji> <titolo breve>

      <descrizione completa e dettagliata>
      ```
    - **Regole di formattazione**:
        - `<tipo>`: `feat`, `fix`, `refactor`, `docs`, `style`, `perf`, `test`, `chore`.
        - `<ambito>`: il modulo o componente toccato (es. `oauth`, `user-iam`, `mfa`, `settings`, `ui`).
        - `<emoji>`: emoji contestuale (es. ✨ `feat`, 🐛 `fix`, 📝 `docs`, ♻️ `refactor`, 🔒 `security`).
        - `<titolo breve>`: sintesi chiara dell'azione.
        - **Body**: separato da una riga vuota, con la spiegazione esaustiva del *cosa*, *perché* e degli impatti logici
          delle modifiche apportate.
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

---

## 7. Visione Architetturale: Ruoli, Permessi & Amministrazione Delegata (Multi-Tenant Scoping)

Appunti e linee guida di progettazione concordati per l'evoluzione del controllo accessi e l'integrazione con servizi esterni:

### A. Separazione dei Mondi: Console Access-Sphere vs. Servizi Frontend Esterni
1. **Console Access-Sphere (IAM Backoffice)**:
    - Riservata agli amministratori della piattaforma o ai manager delegati.
    - Consente la gestione sistemistica: Client OAuth2, Utenti, Ruoli, Impostazioni globali e snapshot di backup.
2. **Servizi FE Esterni (Applicazioni Consumer)**:
    - Applicazioni client esterne (es. CRM, E-Commerce, Portali) che autenticano gli utenti tramite OAuth 2.0 / OIDC.
    - **Perimetro limitato**: non gestiscono client o altri utenti.
    - **Self-Service**: operano solo su se stessi tramite endpoint dedicati (`/userinfo`, `/me`, modifica anagrafica e cambio password con verifica `token.sub == targetIdentifier`).
    - **Business Logic**: consumano `role`, `type` e `permissions` unicamente per regolare la visibilità delle proprie pagine e funzionalità applicative.

### B. Contratto Dati Token & Retrocompatibilità FE
- I FE legacy basati esclusivamente su `Nome` (`roles: ["ROLE_..."]`) e `Type` (`type: "ADMIN" | "USER" | "DEFAULT"`) mantengono il 100% della retrocompatibilità.
- Per abilitare controlli granulari (es. `can('users:delete')`), il JWT e l'endpoint `/v1/oauth2/userinfo` verranno estesi con la claim `permissions: [...]` generata dall'aggregazione delle abilitazioni configurate nei ruoli del client.

### C. Amministrazione Delegata (Multi-Tenant Scoping)
Per consentire a utenti con ruoli elevati (es. `Manager 1`) di accedere alla console di Access-Sphere operando esclusivamente sulle proprie risorse di competenza:
1. **Associazione Utente ↔ Client**: mappatura esplicita tra utente e `client_id` (tramite tabella relazionale o attributi di appartenenza).
2. **Data Scoping a Livello Backend**:
    - `GET /v1/clients`: Super Admin visualizza tutti i client; Manager visualizza solo i client abilitati (`managed_clients`).
    - `GET /v1/users`: Super Admin visualizza tutti gli utenti; Manager visualizza solo gli utenti registrati o associati ai propri `managed_clients`.
    - **Enforcement di Scrittura**: tentativi di modifica/eliminazione su record esterni al perimetro autorizzato vengono respinti con HTTP `403 Forbidden`.
3. **Comportamento Console FE**:
    - La console web consuma le API filtrate mostrando solo le righe e le opzioni consentite.
    - Disabilitazione/occultamento automatico delle sezioni globali (es. Backup & Restore, Branding di sistema) riservate ai soli Super Admin.