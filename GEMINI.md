# Access-Sphere: Core Rules & Architecture (GEMINI.md)

## 1. Project & Stack

**Access-Sphere** is a pragmatic, high-performance IAM and OAuth 2.1 Authorization Server.

- **Backend**: Java 22, Spring Boot 3.4 (WebFlux, Reactive Security, R2DBC, Project Reactor).
- **Frontend**: Thymeleaf & Static HTML (`src/main/resources/static/app/`), Tailwind CSS, Vanilla JS, Material Design 3
  (`m3-dashboard.css`, `m3-themes.css`), FontAwesome 6, SweetAlert2.
- **Core Packages**: `oAuth`, `data.user`, `client`, `mfa`, `settings`.

## 2. Backend Anti-Overengineering & WebFlux Rules (CRITICAL)

- **Strict KISS & YAGNI:** Write the simplest, flattest code possible. Do NOT create `Interface` + `Impl` pairs for
  single implementations. Inject concrete `@Service` classes directly.
- **NO Boilerplate Patterns:** Strictly forbidden to introduce Factory, Strategy, Builder, or generic
  Helper/Manager/Util classes unless explicitly requested.
- **Clean Reactive Chains (WebFlux/Reactor):**
    - Keep `Mono`/`Flux` pipelines flat and linear. NEVER nest `.flatMap()` inside another `.flatMap()` (extract a
      private method on the same class instead).
    - NEVER call `.block()`, `.toFuture().get()`, or use imperative loops over reactive streams.
    - Prefer `Mono.zip()`, `.switchIfEmpty(Mono.error(...))`, and `.filter()` over verbose `if/else` blocks inside
      lambdas.
- **Java 22 Idioms:** Use `record` for DTOs/internal payloads, pattern matching, and `var` where types are obvious.

## 3. Token Efficiency, Logging & Build Rules

- **Meaningful Logging:** Ensure services (especially Client and Auth modules) include clear, structured SLF4J logs at
  key lifecycle points (success, validation failure, error mapping) rather than silent flows or noisy debug prints.
- **Concise Javadoc Only:** Add brief Javadoc ONLY on public methods/classes explaining *why* and *data flow*. Do NOT
  add noisy inline comments on obvious lines of code.
- **Conditional Quiet Build:** Run Maven (`mvn test-compile -q`) ONLY if `.java` or `pom.xml` files were modified. NEVER
  run Maven for HTML/CSS/JS/docs changes.
- **README Sync:** Update `README.md` ONLY when public endpoints, env vars, or database schemas change.
- **Integrity & Commits:** Never run `git commit` autonomously; always output the conventional commit block
  (`<type>(<scope>): <emoji> <title>`) at the end of the response.

## 4. Mandatory Skill Routing

- When touching **HTML, CSS, JS, i18n, or UI templates**, ALWAYS load `.agent/skills/access-sphere-ui/SKILL.md`.
- When touching **OAuth 2.1, Security, Roles, Permissions, or Multi-Tenant scoping**, ALWAYS load
  `.agent/skills/access-sphere-iam-oauth/SKILL.md`.