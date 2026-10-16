# Access-Sphere: Regole Principali e Architettura (GEMINI.md)

## 1. Panoramica del Progetto e Stack

**Access-Sphere** è un Identity and Access Management (IAM) e server di autorizzazione OAuth 2.1 pragmatico e ad alte
prestazioni.

- **Backend**: Java 22, Spring Boot 3.4 (WebFlux, Reactive Security, R2DBC, Project Reactor).
- **Frontend**: Thymeleaf & HTML statico (`src/main/resources/static/app/`), Tailwind CSS, Vanilla JS, Material Design 3
  (`m3-dashboard.css`, `m3-themes.css`), FontAwesome 6, SweetAlert2.
- **Package Principali**: `oAuth`, `data.user`, `client`, `mfa`, `settings`.

## 2. Regole Backend Anti-Overengineering e WebFlux (CRITICO)

- **KISS e YAGNI Rigorosi:** Scrivi il codice più semplice e lineare possibile. NON creare coppie `Interface` + `Impl`
  per singole implementazioni. Inietta direttamente le classi `@Service` concrete.
- **ZERO Pattern Ripetitivi (Boilerplate):** È severamente vietato introdurre Factory, Strategy, Builder o classi
  generiche di tipo Helper/Manager/Util a meno che non sia esplicitamente richiesto.
- **Catene Reattive Pulite (WebFlux/Reactor):**
    - Mantieni le pipeline `Mono`/`Flux` piatte e lineari. NON annidare mai un `.flatMap()` dentro un altro `.flatMap()`
      (estrai invece un metodo privato nella stessa classe).
    - NON chiamare MAI `.block()`, `.toFuture().get()`, né usare cicli imperativi sui flussi reattivi.
    - Preferisci `Mono.zip()`, `.switchIfEmpty(Mono.error(...))` e `.filter()` rispetto a blocchi `if/else` verbosi
      all'interno delle lambda.
- **Idiomi di Java 22:** Usa i `record` per i DTO/payload interni, il pattern matching e `var` dove i tipi sono
  evidenti.

## 3. Efficienza dei Token, Logging e Regole di Build

- **Logging Significativo:** Assicurati che i servizi (in particolare i moduli Client e Auth) includano log SLF4J chiari
  e strutturati nei punti chiave del ciclo di vita (successo, fallimento della validazione, mappatura degli errori)
  anziché flussi silenziosi o stampe di debug rumorose.
- **Solo Javadoc Concise:** Aggiungi brevi Javadoc ESCLUSIVAMENTE su classi o metodi pubblici, spiegando il *perché* e
  il *flusso dei dati*. NON aggiungere commenti inline rumorosi su righe di codice ovvie.
- **Build Silenziosa Condizionale:** Esegui Maven (`mvn test-compile -q`) SOLO se sono stati modificati file `.java` o
  il `pom.xml`. Non eseguire MAI Maven per modifiche a HTML, CSS, JS o documentazione.
- **Sincronizzazione del README:** Aggiorna il file `README.md` SOLO quando cambiano endpoint pubblici, variabili
  d'ambiente o schemi del database.

## 4. Convenzione Messaggi di Commit Git (Solo Suggerimento) in Inglese

- L'agente **NON deve MAI eseguire autonomamente il commit Git** (`git commit`).
- Al termine di ogni implementazione o modifica, l'agente deve **stampare nella risposta** il messaggio di commit pronto
  per essere copiato dall'utente, formattato in un blocco di codice:
  ```text
  <tipo>(<ambito>): <emoji> <titolo breve>

  <descrizione completa e dettagliata>
  ```
- **Regole di formattazione**:
    - `<tipo>`: `feat`, `fix`, `refactor`, `docs`, `style`, `perf`, `test`, `chore`.
    - `<ambito>`: il modulo o componente toccato (es. `oauth`, `user-iam`, `mfa`, `settings`, `ui`).
    - `<emoji>`: emoji contestuale (es. ✨ `feat`, 🐛 `fix`, 📝 `docs`, ♻️ `refactor`, 🔒 `security`).
    - `<titolo breve>`: sintesi chiara dell'azione.
    - **Body**: separato da una riga vuota, con la spiegazione esaustiva del *cosa*, del *perché* e degli impatti logici
      delle modifiche apportate.

## 5. Instradamento Obbligatorio delle Skill

- Quando tocchi **HTML, CSS, JS, i18n o template UI**, carica SEMPRE `.agent/skills/access-sphere-ui/SKILL.md`.
- Quando tocchi **OAuth 2.1, Sicurezza, Ruoli, Permessi o Scoping Multi-Tenant**, carica SEMPRE
  `.agent/skills/access-sphere-iam-oauth/SKILL.md`.