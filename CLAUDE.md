# Notary Public Web Application

## General Working Rules

Read and follow these at the start of every session.

1. Keep responses, clarifications, and thought-process descriptions short and
   clear — one sentence when that's meaningful enough.
2. Working code only. Plausibility is not correctness; verify before
   reporting done. After each logical/completed change, run the existing
   test suite. A task is done only when the desired outcome is achieved and
   all tests pass.
3. Say when a premise appears wrong before implementing around it, or ask
   for clarification when several implementation options are available.
4. Touch only what the task requires. Avoid drive-by refactors, formatting,
   or cleanup.
5. Keep communication direct and concise. Skip flattery, filler, ceremonial
   openings, and emoji.
6. Read complete errors, logs, and stack traces before fixing them.
7. State the plan or success criteria before editing. For non-trivial work
   (more than a one-line fix), include the verification you expect to run.
8. Read the files you will touch and the nearby callers, consumers, or docs
   that define their behavior.
9. Resolve ambiguity by reading code or running commands when practical;
   surface assumptions out loud when they affect the result.
10. Project maintenance instructions (how to keep this project healthy, e.g.
    doc-update policy) belong in this file, not scattered elsewhere.
11. Always update this file's Progress section after new functionality is
    introduced or a defined milestone is reached.
12. Use the minimum code or documentation change that solves the stated
    problem.
13. Do not add speculative features, abstractions, configurability, or
    hooks.
14. Clean up orphans created by your own change (unused imports, obsolete
    helpers) — this is the one exception to rule 4.
15. Do not delete pre-existing dead code unless asked; mention it in the
    summary if it matters.
16. Keep this rules list short enough to actually follow. Add a rule only
    when it prevents a real repeat mistake or documents durable project
    behavior.
17. When the user corrects an approach, tighten the relevant rule instead of
    appending a vague new one.
18. Git commits: keep the default "Co-Authored-By: Claude" trailer; do not
    change the commit author field.
19. When a new feature is implemented, always add test cases that verify its
    correct functionality.
20. `mvn test` never runs SpotBugs/find-security-bugs — that plugin binds to
    the `verify` phase, one phase past `test`, so passing tests says nothing
    about the security check. Before committing anything touching redirects,
    request-derived values, raw SQL, or similar security-sensitive patterns,
    also run `mvn clean compile spotbugs:check` (the actual CI step) — see
    Build & Run. Reason: shipped a commit with an `UNVALIDATED_REDIRECT` that
    `mvn test` couldn't have caught no matter how thorough.

## Project Overview

A Spring Boot web application for a notary public office. It has a public-facing
multilingual website and a protected admin panel for managing appointments and
documents.

Two audiences, two areas of the app:

1. **Public site** — visited by clients. Server-rendered, available in 3 languages.
2. **Admin panel** — used only by the notary/staff. Login-protected, manages
   appointments and documents.

## Tech Stack

- Java 25 (LTS), Spring Boot 4.1.x (latest
  stable, requires Spring Framework 7.0.8+), Maven (single module)
- Thymeleaf for server-side rendering (both public site and admin panel)
- Bootstrap (latest 5.x) for styling, loaded via CDN in the base layout
  template — use its grid, forms, nav, and card components rather than
  writing custom CSS from scratch; only add custom CSS for things Bootstrap
  doesn't cover (e.g. the map container sizing)
- Spring Data JPA + PostgreSQL (use H2 file-based DB for local dev, PostgreSQL
  for prod — externalize via `application.yml` profiles)
- Spring Security for admin authentication (form login, session-based — no need
  for OAuth/JWT for a single-admin internal tool)
- Spring's built-in i18n (`MessageSource`) — 3 locales:
  - `messages_ro.properties` (Romanian)
  - `messages_hu.properties` (Hungarian)
  - `messages_en.properties` (English, also the fallback/default)
- Leaflet.js + OpenStreetMap tiles for the contact page map pin (no API key
  needed — do not use Google Maps JS API)
- Bean Validation (`jakarta.validation`) for form input
- Flyway for DB schema migrations (start with `V1__init.sql`)

Note: Spring Boot 4 moved/changed some packages and config compared to 3.x
(Spring Framework 7 API changes). Check current Spring Boot 4.1 docs/migration
notes when scaffolding rather than relying on 3.x-era tutorials or examples.

**Always search current web documentation before writing code against any of
these dependencies**, especially the newest ones (Spring Boot 4.1, Spring
Framework 7, Spring Security 7, Hibernate 7). Training data likely predates
or only partially covers these releases — don't guess at APIs, config
property names, or migration steps from memory. When in doubt, look it up.

## Domain Model

- **Service** — id, translated name/description (one row per locale or a
  `ServiceTranslation` child table), duration in minutes, active flag
- **Appointment** — id, client name, email, phone, service (FK), requested
  date/time, status (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`), notes,
  created timestamp
- **Document** — id, title, category, stored filename, original filename,
  content type, uploaded timestamp, optional FK to Appointment
- **AdminUser** — id, username, hashed password, role (start with a single
  `ADMIN` role — no need for granular roles yet)

Store uploaded files on the filesystem under a configurable base directory
(e.g. `app.storage.documents-dir` in `application.yml`), not as DB blobs. The
DB only stores metadata and the relative path.

## Pages

### Public site (all under locale-aware routing, e.g. `/en/...`, `/ro/...`)
- **Home** — brief intro, links to other pages
- **Services** — list of services offered, translated description per service
- **Contact** — address, phone, email, embedded Leaflet map with a pin at the
  office location
- **Book Appointment** — form: name, email, phone, service dropdown, preferred
  date/time, notes. On submit, creates an `Appointment` with status `PENDING`
  and shows a confirmation page. Send a confirmation email if `spring-boot-starter-mail`
  is configured (stub this out with a `TODO` if SMTP isn't set up yet).

### Admin panel (all under `/admin/**`, protected by Spring Security)
- **Login**
- **Appointment dashboard** — list/filter by status and date, view details,
  change status, add internal notes
- **Document manager** — upload, list, download, delete documents; associate
  with an appointment optionally
- **Service editor** — CRUD on services and their translations
- **Statistics** — summary view of appointments grouped by service/type: counts
  per service, breakdown by status (pending/confirmed/cancelled/completed), and
  a date-range filter (e.g. this month, last 30 days, custom range). Simple
  table + a chart (Chart.js via CDN is fine) is enough — no need for a
  reporting framework.

## Conventions

- Package structure: `com.<company>.notary.{config,controller,domain,dto,migration,repository,service}`
  — ask me for the actual base package/company name before scaffolding
- Default locale is Romanian (`ro`); English (`en`) and Hungarian (`hu`) are
  the other two supported locales
- Controllers thin, business logic in `@Service` classes, DB access via Spring
  Data JPA repositories
- Use DTOs for form binding in the public booking form (never bind Thymeleaf
  forms directly to JPA entities)
- Validate all public-facing form input server-side even though we'll add
  client-side validation too
- Write integration tests for the appointment booking flow and the admin CRUD
  endpoints using `@SpringBootTest` + Testcontainers (PostgreSQL) once the
  schema stabilizes; unit tests for services as you go

## Build & Run

- `mvn spring-boot:run` for local dev (H2 profile)
- `mvn clean package` produces a single executable jar
- `mvn test` does NOT run the SpotBugs/find-security-bugs check used by the
  `pr-security-tests.yml` CI workflow (that plugin binds to `verify`, a phase
  `test` never reaches). Run `mvn clean compile spotbugs:check` separately
  before committing anything security-sensitive — see rule 20 above
- Flyway migrations run automatically on startup — these define table structure
  only (`db/migration/*.sql`); no seed data lives in SQL anymore
- Reference/seed data lives in YAML files under `src/main/resources/`
  (`services.yml`, `admin-users.yml`), loaded by `CommandLineRunner`s
  (`ServiceSeeder`, `AdminUserSeeder`) after Flyway completes. `ServiceSeeder`
  creates missing services only (services.yml seeds a service just once, on
  first creation); `AdminUserSeeder` only ever creates missing accounts by
  username — it will never touch one that already exists, since the Admin
  Users screen is a live, independent source of truth for those

## Dockerization

- Multi-stage `Dockerfile`: build stage uses a Maven+Java 25 image to run
  `mvn clean package`, runtime stage copies the jar into a slim Java 25 JRE
  base image (e.g. `eclipse-temurin:25-jre` — verify current recommended tag
  via web search, don't assume)
- `docker-compose.yml` with two services: the app and a `postgres` container
  (use the `prod`/`docker` Spring profile to point at the Postgres service by
  container name, not `localhost`)
- Mount the documents storage directory as a named volume so uploaded files
  survive container restarts/rebuilds
- Externalize DB credentials and storage path via environment variables in
  `application-docker.yml`, not hardcoded
- `.dockerignore` should exclude `target/`, `.git/`, and local IDE files
- Goal: `docker compose up` should be enough to get the full stack (app + DB)
  running for local testing or deployment

## Suggested build order (use this as your task list)

1. Scaffold project via Spring Initializr deps (Web, Thymeleaf, Data JPA, H2,
   PostgreSQL driver, Security, Validation, Flyway)
2. Domain entities + Flyway `V1__init.sql`
3. i18n setup: locale resolver + interceptor, message property files, language
   switcher in the layout template
4. Public pages: Home, Services (read-only from DB)
5. Contact page with Leaflet map
6. Appointment booking form + confirmation flow
7. Spring Security config for `/admin/**` + login page + seed one AdminUser
8. Admin appointment dashboard (list, filter, status change)
9. Document upload/download/delete + filesystem storage service
10. Admin service editor (CRUD + translations)
11. Admin statistics page (aggregation queries + chart)
12. Dockerize: Dockerfile, docker-compose.yml (app + Postgres), verify
    `docker compose up` runs the full stack end to end
13. Polish: error pages, basic responsive CSS, tests for critical flows

Work through this list incrementally — confirm each step compiles and runs
before moving to the next. Don't jump ahead to later steps until earlier ones
are working.

## Progress (as of 2026-08-18)

Steps 1–11 done and verified (full automated suite, not just manual curl
checks). Base package: `com.brinza.notary`. Everything under "beyond spec"
below was added on top of the original numbered plan.

- [x] **1. Scaffold** — Spring Boot 4.1.0, Java 25, Maven. `application.yml`
      with `h2` (default) and `docker` profiles.
- [x] **2. Domain entities** — `Service`/`ServiceTranslation`, `Appointment`,
      `Document`, `AdminUser`, `SystemSetting`, `InternalNote` + Flyway `V1`–`V13`
      (plus Java-based `V11__AddAppointmentEndedAt.java`). Gotcha: `Service`
      business-logic classes must fully-qualify
      `@org.springframework.stereotype.Service` to avoid a same-name import
      clash — see `ServiceCatalogService`.
- [x] **3. i18n** — `PathLocaleResolver` resolves locale from the URL prefix
      (`/en`, `/ro`, `/hu`), not a cookie/session. Gotcha: a root
      `messages.properties` (unsuffixed) must exist alongside the
      `messages_xx.properties` files or Boot's message-source auto-config
      silently no-ops.
- [x] **4. Public Home + Services pages** — `services.yml` seeds 5 sample
      services on first startup only (see step 10).
- [x] **5. Contact page + Leaflet map** — `app.contact.*` in
      `application.yml`; coordinates are real
      (46.72982564167728, 23.48734642212008).
- [x] **6. Booking form + confirmation flow** — DTO-backed, validated, i18n'd
      error messages via `{key}` Bean Validation templates. Confirmation
      email is implemented (not a stub): `AppointmentEmailService` +
      `AsyncEmailSender`, sent asynchronously so SMTP latency doesn't block
      the booking/admin status-change request.
- [x] **7. Spring Security for `/admin/**`** — custom login page, seeded
      `AdminUser` (**default `admin`/`admin` — change before real
      deployment**, via `admin-users.yml`/`AdminUserSeeder`, create-missing-only).
      Extended well beyond original scope:
      - `AdminRole`; admin user management screen (`AdminUserAdminController`,
        CRUD + change-password, `AdminUserManagementService`); session
        tracking (`AdminSessionListener`/`AdminSessionRegistry`); last-login
        tracking (`AdminLoginTracker`); correlation-ID filters
        (`CorrelationIdFilter`, `AdminSessionCorrelationFilter`).
      - Brute-force protection: `LoginAttemptService` locks a username after
        N failed logins for M minutes, both runtime-editable by TECHNICIAN
        users (Configurare > Sistem, `SystemSettings`, defaults 5/15, same
        pattern as the mail-enabled toggle). A tripped lock persists on the
        `AdminUser` row (`is_locked`/`lock_until`, `V15` migration), surviving
        restarts, and self-clears once `lockUntil` passes
        (`AdminUserDetailsService`). Only a *tripped* lock persists — the
        in-progress failure count is in-memory only and resets on restart
        (thresholds themselves live in `system_settings` and don't reset).
      - Locked accounts get a dedicated login-page message
        (`/admin/login?locked`, `SecurityConfig`'s `failureHandler` branches
        on `LockedException`), shown on every attempt while locked including
        a correct password. Deliberately reversed from the original generic
        `?error`-for-both design on explicit request — accepted
        account-enumeration tradeoff for this single-admin internal tool.
      - `AdminUser.fullName` is editable via the edit form but only ever
        shown on a read-only detail page (`GET /admin/users/{id}`); the list
        page no longer has an Editează action, only Detalii → edit link.
        Schema change was drop-and-recreate (`V14`), not `ALTER TABLE` (no
        real prod data yet). Detail page shows Activ/Blocat + `lockUntil`;
        TECHNICIAN can force-unlock (`POST /admin/users/{id}/unlock`) which
        clears both the persisted lock and the in-memory attempt counter,
        allowing immediate login.
      - **Gotcha (regression-tested):** `LoginAttemptService
        .onAuthenticationFailure` must carry `@Transactional` itself, not
        just `recordFailure` — a same-class call bypasses the Spring AOP
        proxy, so `recordFailure`'s own `@Transactional` silently never
        applies, and the lock is lost. Every automated test passed anyway
        because workflow tests are class-level `@Transactional` (one
        Hibernate session masks the bug); caught only via manual browser
        testing + DB check. `LoginAttemptServicePersistenceTest` is
        deliberately non-`@Transactional` to catch this again. **Lesson:**
        put `@Transactional` on the externally-invoked entry point, not a
        helper it calls internally.
- [x] **8. Admin appointment dashboard** — list/filter by status + date
      range, detail view, status change, internal notes with history (`V8`).
      `internal_notes` is deliberately separate from the client's own
      booking `notes` (`V4`) so staff edits never clobber the client's
      message. Also gained a calendar view (`CalendarAdminController`), not
      in the original spec.
      - Navbar "Programări" badge (yellow `!`) shows whenever ≥1 `PENDING`
        appointment exists (`AppointmentRepository.existsByStatus`,
        `AppointmentManagementService.hasPendingAppointments()`), exposed via
        `AdminGlobalModelAttributes` (`@ControllerAdvice`). Gotcha:
        `@WebMvcTest` instantiates every `@ControllerAdvice` regardless of
        `basePackages`, so a direct constructor dependency broke unrelated
        slices — fixed via `ObjectProvider`, deferring the lookup and
        resolving to "no notification" when absent (same pattern needed
        again below for `StructuredDataService`/`contactSettings`).
        Regression test: `AdminNavbarPendingNotificationWorkflowTest`.
      - Overlap handling: booking itself is unrestricted, but a `PENDING`
        appointment overlapping an already-`CONFIRMED` one is flagged (red
        `!` in the pending table + warning box on detail page), via
        `AppointmentRepository.existsOverlapping` →
        `AppointmentManagementService.overlapsConfirmed()`. `updateStatus`
        rejects `PENDING`→`CONFIRMED` while an overlap exists;
        `updateSchedule` applies the same check whenever the appointment
        being rescheduled is (or would become) `CONFIRMED` and overlaps —
        together these two guards make "CONFIRMED never overlaps" an actual
        invariant, not just a display warning.
      - Reschedule form greys out start/end options that would land inside a
        `CONFIRMED` span (`findBusyTimeSlots` → `BusyTimeSlots(startTimes,
        endTimes)`). Gotcha (real bug, caught only by manual UI testing):
        start and end need *different* boundary rules, not one shared
        busy-set — starting exactly when another appointment starts is
        already an overlap (`otherStart <= t < otherEnd`), but ending
        exactly when one *starts* is fine (back-to-back), while ending
        exactly when one *ends* is still an overlap
        (`otherStart < t <= otherEnd`). Greying is a UI hint only —
        `updateSchedule`'s server-side check is the real guarantee.
        Regression tests: `AppointmentOverlapWorkflowTest`,
        `AppointmentManagementServiceTest`.
      - Admins can create appointments directly ("+ Adaugă programare" on the
        list page → `admin/appointments/new.html`), reusing
        `AppointmentBookingService`/`BookingRequest`. Deliberate difference:
        `bookAsAdmin()` skips the "booking received" email (admin acting on
        the client's behalf); `sendConfirmedEmail` on status change is
        unaffected. The new-appointment form also lets the admin pick an
        explicit end time (not just derived from service duration) and shows
        the day-timeline visual; `bookAsAdmin(BookingRequest, LocalTime)`
        requires and validates that end time, while public `book()` still
        derives it from duration. Changing the date fetches busy-times and
        day-schedule endpoints; the latter returns a Thymeleaf fragment view
        directly — its parameter must be passed by name in that case
        (`"admin/fragments :: appointmentsTimeline(appointments=...)"`) or
        Thymeleaf rejects it as a "synthetic"/positional parameter.
      - List page's status filter is a multi-select checkbox dropdown (not a
        single `<select>`); "Toate" is mutually exclusive with individual
        statuses via plain JS and isn't itself submitted — no `status`
        params means no filter. `showList`'s `status` param is
        `Set<AppointmentStatus>`; repository JPQL uses `IN :statuses`, with
        an empty incoming set normalized to `null` before the query (empty
        JPQL `IN ()` isn't safe to assume). Needed the Bootstrap JS bundle
        added to this page only — other admin pages still pull Bootstrap CSS
        only.
- [x] **9. Document upload/download/delete** — filesystem storage
      (`DocumentStorageService`, `DocumentManagementService`). Deviation: no
      standalone document-manager page — done from the appointment detail
      page's `/admin/appointments/{id}/documents/**` endpoints, since
      documents are always tied to an appointment in practice.
- [x] **10. Admin service editor** — `ServiceAdminController` under
      `/admin/settings/services` (list/create/edit/delete), TECHNICIAN-only
      like the rest of `/admin/settings/**`. Configuration page has two tabs:
      Sistem + Servicii. Deleting a service in use is blocked
      (`existsByServiceId`) — deactivate instead. Consequence: `ServiceSeeder`
      became create-missing-only (mirrors `AdminUserSeeder`) since the admin
      Services screen is now the live source of truth.
- [x] **11. Admin statistics** — `StatisticsAdminController` under
      `/admin/statistics`: monthly counts by service/status with a Chart.js
      chart, plus (beyond spec) traffic stats, an admin activity log
      (`AdminActivityLogger` + `activity.html`), and a raw log viewer
      (`LogViewerService` + `logs.html`).
      - Traffic tab (`traffic.html`): per-client (by IP) breakdown of time
        spent on each public page, plus a total. Deliberately **not**
        persisted — `TrafficStatsService` is an in-memory, runtime-only
        `@Service` (resets on restart, unbounded for the process lifetime;
        traffic on a single-notary site was judged too low for that to
        matter). Time-on-page can't be observed directly over HTTP, so it's
        inferred from the gap between consecutive requests from the same IP:
        a gap under 10 minutes is attributed to the page that preceded it; a
        gap of 10 minutes or more means the visitor went idle, so that pause
        is discarded (not counted as reading time) and the next request
        starts a fresh session. Sessions for the same IP keep accumulating
        into the same total for as long as the app runs — only the idle gap
        itself is excluded. The page currently open in an ongoing session has
        no counted duration yet; it's only added once another request
        arrives (or dropped if none ever does within the timeout).
        `PublicTrafficTrackingFilter` (`config.filters`) feeds it from real
        traffic: only GETs to a known public page count as a "view" (locale
        stripped, so `/ro/services` and `/en/services` are the same
        `PublicPage.SERVICES` entry) — the booking form's POST isn't a page
        view, and `/admin/**`, static assets, `/sitemap.xml`, etc. are
        untouched. Gotcha: like `AdminSessionCorrelationFilter`,
        `@WebMvcTest` slices pull in every `Filter`-type `@Component`
        regardless of `basePackages`/`controllers()`, so every existing
        `@WebMvcTest` needed `TrafficStatsService.class` added to its
        `@Import` alongside `AdminSessionRegistry.class`.
      - Traffic table also shows a session counter (`sessionCount`, one of
        those idle-timeout transitions per client) and an approximate
        (city-level) `location`, resolved via `GeoLocationService` — the
        app's only outbound HTTP dependency (`http://ip-api.com`, free tier,
        no API key; see README.md's Outbound Network Access note for the
        prod firewall implication). Deliberately not a blocking lookup:
        `TrafficStatsService.recordPageView` detects a brand-new IP
        atomically via `computeIfAbsent`'s mapping-function guarantee and
        fires `geoLocationService.resolveAsync(ip, session::setLocation)`
        exactly once for it — an `@Async` fire-and-forget call (same pattern
        as `AsyncEmailSender`), so the request thread never waits on it.
        `location` stays `null` (shown as `—`) until/unless the callback
        arrives; on failure or timeout (3s connect + read) it's simply never
        called again — no retry, no persistence. Private/loopback IPs
        (typical of local dev) short-circuit to `"Local"` without an outbound
        call at all. `GeoLocationService`'s `RestClient` is hand-built with
        `JdkClientHttpRequestFactory` (plain `spring-web`, already on the
        classpath) rather than injecting Boot's autoconfigured
        `RestClient.Builder` — that autoconfiguration lives in
        `spring-boot-http-client`/a RestClient starter this project doesn't
        depend on (`spring-boot-starter-webmvc` alone doesn't pull it in).

Beyond spec: runtime-editable app log level (`SystemSettings.logLevel`, same
cache/persist pattern as `mailEnabled`) — dropdown posts to
`/admin/settings/log-level`, calls `LoggingSystem.setLogLevel` against
`com.brinza.notary` only, never `ROOT` (framework logging unaffected).
`LoggingSystem` isn't a Spring bean here (no Actuator dep), obtained via
`LoggingSystem.get(classLoader)`. `load()` re-applies the persisted level on
every startup since `logback-spring.xml`'s static `root level="INFO"` is only
the initial default.

Beyond spec: public-facing maintenance/notice banner, configured from a new
**Notificare** tab on the Configurare page (`/admin/settings/notification`,
own `admin/settings/notification.html`) — unlike **Sistem**/**Servicii**,
this one tab is open to ADMIN as well as TECHNICIAN (`SecurityConfig`,
`settingsNav` fragment gates each tab's `<li>` individually via
`sec:authorize`). Same `SystemSettings` cache/persist pattern as
`mailEnabled`, extended with `notification.enabled`/`notification.message`/
`notification.vacation-start`/`notification.vacation-end` keys (still the
generic `system_settings` table, no new migration).
- Turning the toggle on requires either a typed message or a selected
  vacation date range (native `<input type="date">` pair, no JS calendar
  library) — `SystemSettings.setNotification` rejects a partial range, a
  start after end, and (if there's no vacation range either) an empty
  message. Turning the toggle off always clears both the message and the
  vacation range, so a stale one can never resurface just by flipping the
  toggle back on undoing nothing else. The toggle is the sole persistence
  trigger (`onchange="this.form.submit()"`) — deliberately not also wired to
  the message input, since both auto-submitting independently raced each
  other (whichever field's browser event fired first submitted a stale
  snapshot of the other field's DOM value).
- Public rendering (`GlobalModelAttributes`, `layout/fragments.html`'s
  `notificationBanner` fragment, included site-wide right under the navbar):
  a typed message always wins over the vacation range; with no typed
  message, an active vacation range renders a translated "office is closed"
  template (`notification.vacation.message.*` keys, `messages_en/ro/hu.
  properties`) with the date(s) in `<strong>` — built from `prefix`/
  `between`/`suffix` message pieces rather than one `{0}`/`{1}` template, so
  the dates can be individually bolded via plain `th:text` spans without
  needing `th:utext`/unescaped output (blocked by the CI Thymeleaf guard). A
  single-day range (start equals end) uses a dedicated `singleDay.prefix`/
  `singleDay.suffix` pair ("closed **on** `<date>`") instead of the
  between/and phrasing.
- The booking page's flatpickr date picker (`public/book.html`) greys out
  the announced vacation range (`notificationVacationStartIso`/`...EndIso`
  model attributes) so a visitor can't request an appointment during a
  closure the banner is actively telling them about — gated on the same
  "is the vacation template actually the one showing" flag, not merely on
  whether a range is stored, so a typed message overriding the vacation
  banner doesn't grey out dates the visitor is no longer being told about.

Beyond spec: booking is blocked on Saturdays/Sundays —
`BookingRequest.isRequestedAtOnWeekday()` (same `@AssertTrue` pattern as the
existing half-hour-slot check), so this applies to both the public booking
form and the admin's own "+ Adaugă programare" form (`bookAsAdmin`), since
both share the same `BookingRequest` DTO. `public/book.html`'s flatpickr
also greys out Saturday/Sunday client-side, same array alongside the
vacation-range disable rule.

Not started:

- [ ] 12. Dockerize — no `Dockerfile`, `docker-compose.yml`, or
      `.dockerignore` yet.
- [ ] 13. Polish — a generic `error.html` exists but hasn't had a dedicated
      pass; no responsive CSS pass yet.

**SEO track** (separate from the numbered plan; technical foundations +
structured data done, content/local-SEO items deferred):
- Structured data (schema.org JSON-LD) via `StructuredDataService`, embedded
  with Thymeleaf's `/*[( )]*/` script-inlining idiom (never `th:utext`,
  blocked by the `pr-security-tests.yml` guard):
  - `LegalService` node on the Contact page only (where the data lives).
  - `Service` catalog (one node per active service, `provider` → `LegalService`)
    on the Services page as a single `@graph` block.
  - `BreadcrumbList`, computed generically from `pathAfterLocale`
    (`GlobalModelAttributes`) and rendered site-wide — `null` on home, 2
    levels on Services/Contact/Book, 3 on booking confirmation.
  - `app.contact.*` restructured from two free-text fields into
    `ContactSettings` (`@ConfigurationProperties(prefix = "app.contact")`
    record: `street`/`city`/`postal-code`/`country-code`/`opening-time`/
    `closing-time`/`days-of-week` + `phone`/`email`/`latitude`/`longitude`).
    Contact page and the booking-confirmation email derive display strings
    from `ContactSettings.displayAddress()`/`displayHours()`. `daysOfWeek`
    holds schema.org's canonical English tokens and is **not** localized —
    must stay in sync by hand with the translated `contact.schedule.days`
    label in `messages_*.properties`.
  - Needed `spring-boot-starter-json` (not pulled in by
    `spring-boot-starter-webmvc` alone on Boot 4.1's split modules) and
    Jackson 3's `tools.jackson.databind.json.JsonMapper` (legacy
    `com.fasterxml.jackson` `ObjectMapper` is deprecated on Boot 4.1).
  - Same `@ControllerAdvice`/`@WebMvcTest` gotcha as step 8's navbar badge —
    fixed the same way, via `ObjectProvider<StructuredDataService>`.
- Per-page, per-locale `<title>`/meta description, self-canonical, and
  `hreflang` alternates (ro/en/hu + x-default) on all 5 public pages via
  `layout/fragments.html`'s `head(...)` fragment, built from `app.base-url`
  (currently a `https://example.com` placeholder — **must be set before
  deploy**) + `currentLocale`/`pathAfterLocale`.
- `SeoController` serves `/sitemap.xml` (4 indexable pages × 3 locales,
  XML-escaped) and `/robots.txt` (disallows `/admin/`, `/h2-console/`), both
  generated from `app.base-url` + the page-path list so they can't drift.
  `POTENTIAL_XML_INJECTION` is suppressed in `spotbugs-exclude.xml` for this
  class — find-security-bugs doesn't recognize the custom `escapeXml()`
  sanitizer, and inputs aren't user-controlled (same false-positive class as
  `CRLF_INJECTION_LOGS`).
- `noindex,nofollow` on the booking confirmation page, every `/admin/**`
  page + login, and `error.html`.
- Root `/` → `/ro` redirect (`WebConfig`) is now an explicit 301.
- Content pass: `services.html` card titles fixed from `<h5>` to
  `<h2 class="h5 card-title">` (correct heading order, same visual size via
  Bootstrap's `.h5` utility). NAP block added to the shared public footer,
  sourced from `ContactSettings` (same `ObjectProvider` pattern). Phone/email
  on the footer + Contact page are real `tel:`/`mailto:` links (raw config
  value, no E.164 reformatting — RFC 3966 allows the space-separated format
  already in `app.contact.phone`).
  - Reciprocal in-content links (Home/Services/Contact cross-linking to
    Book) were tried and reverted on request — with the navbar already
    linking every page, a second link to the same URL in the body read as
    accidental. Kept only the explicitly-requested "Book Appointment" CTA on
    the Services page.

**Security & CI track:**
- GitHub Actions: `pr-tests.yml` (full Maven suite on every PR),
  `pr-security-tests.yml` (SpotBugs + find-security-bugs via
  `spotbugs-exclude.xml`, plus grep-based guards against `th:utext`/bare
  `[(...)]` unescaped output and raw `action=` attributes that would bypass
  CSRF token injection).
- Dependabot configured for Maven and GitHub Actions.

**Testing:** full automated suite under `src/test/java` — unit tests per
service/config/repository/controller, plus higher-level workflow tests in
`com.brinza.notary.workflow` (`BookingWorkflowTest`,
`AdminAppointmentStatusWorkflowTest`, `AdminUserCrudWorkflowTest`,
`SecurityAccessWorkflowTest`). Uses H2, not Testcontainers — see
[[project_np_webapp_test_suite_structure]] memory for why and for test-slice
gotchas with Boot 4/Security 7.

**Known shortcuts/deviations still standing:**
- Admin panel has no i18n (English-only templates, some hardcoded Romanian
  strings e.g. "Document șters.") — intentional, matches "used only by the
  notary/staff", not spec'd as multilingual.
- Document management lives inside the appointment detail page rather than
  as its own admin section (see step 9).
