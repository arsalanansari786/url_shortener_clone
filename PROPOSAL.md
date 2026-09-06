# URL Shortener Website — Proposal

**Project:** usp (`url_shortener_clone`)
**Repo:** https://github.com/arsalanansari786/url_shortener_clone
**Date:** 2026-08-09

---

## 1. Objective

Turn the existing Spring Boot URL-shortener backend (shorten, redirect, expiry, click count) into a complete, usable **website**: a browser-based UI on top of the current API, with the backend hardened enough to be demo/portfolio-ready. End state: a person can open the site, paste a long URL, get a short link, copy it, click it, and see it redirect — with expired/invalid links handled gracefully.

## 2. Proposed Solution

Keep the current architecture (Controller → Service → Repository → H2) and add a thin static frontend served directly by Spring Boot (`src/main/resources/static/`), so one `mvnw spring-boot:run` serves both API and UI — no separate frontend server or build tooling.

Backend hardening alongside the UI work:
- Move the hardcoded `127.0.0.1:8991` base URL into `application.properties` (`app.base-url`) so it isn't baked into responses.
- Replace the current Base64-slice short-code generator with a Base62 generator (URL-safe, no `+`/`/`), keeping the existing collision-retry loop.
- Centralize error handling with a `@ControllerAdvice` instead of the current swallowed `catch (Exception e) {}` blocks, so failures return a clean JSON error instead of a silent `null` → generic 500/404.
- Validate incoming URLs (`java.net.URI` parse + scheme check) before accepting them.
- Add unit + integration tests (currently there are none beyond the empty Spring Boot context test).
- Add a `README.md` with setup/run instructions.

## 3. Implementation Details

**Stack (unchanged):** Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, H2 (in-memory), Lombok. **Frontend:** plain HTML/CSS/vanilla JS (no framework/build step), served as static resources.

**New/changed files:**

| File | Change |
|---|---|
| `src/main/resources/static/index.html` | Home page — form to submit a long URL, result card with the short link and copy button |
| `src/main/resources/static/style.css` | Minimal responsive styling |
| `src/main/resources/static/app.js` | Calls `POST /short`, renders result, loads `/view` table |
| `src/main/resources/static/expired.html` | Friendly page for expired links |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | `@ControllerAdvice`, converts exceptions to clean JSON errors |
| `src/main/java/.../service/UrlService.java` | Base62 generator, config-driven base URL/TTL, input validation |
| `src/main/java/.../controller/UrlController.java` | Return full short URL using injected `app.base-url`; consistent response bodies |
| `src/main/resources/application.properties` | New `app.*` config keys (below) |
| `src/test/java/...` | `UrlServiceTest` (unit), `UrlControllerTest` (MockMvc integration) |
| `README.md` | Setup, run, and usage instructions |

**API contract (unchanged endpoints, tightened responses):**

| Method | Path | Request | Success | Failure |
|---|---|---|---|---|
| `POST` | `/short` | raw long URL (text body) | `200` + short URL string | `400` invalid URL, `500` generation failure |
| `GET` | `/{shortUrl}` | — | `302` redirect | `404` unknown, `410` expired (currently `403`, see note), `414` malformed code |
| `GET` | `/view` | — | `200` + JSON map of all links | — |
| `GET` | `/expired` | — | rendered `expired.html` for browser hits | — |

Note: I'll switch the expired-link status from `403 Forbidden` to `410 Gone`, which is the semantically correct HTTP code for "used to exist, doesn't anymore." Flag if you'd rather keep `403`.

## 4. Scope In

- Single-page web UI: shorten form, copy-to-clipboard result, table of existing links with click counts and expiry time.
- Redirect flow via `GET /{shortUrl}`, including expired-link and not-found pages.
- Server-side URL validation and centralized error handling.
- Config-driven base URL, short-code length, and TTL (no more hardcoded values).
- Base62 short-code generation (URL-safe).
- Unit tests (service layer) and integration tests (controller layer via MockMvc).
- `README.md` with run instructions.
- Git commit history + push to the existing GitHub remote.

## 5. Scope Out (this round)

- User accounts / authentication / per-user link ownership.
- Custom aliases (user-chosen short codes) — straightforward follow-up if wanted.
- Rate limiting / abuse protection.
- Production database (Postgres/MySQL) — staying on H2 in-memory; data resets on restart, this is a known/accepted limitation for now.
- Distributed cache (Redis) for multi-instance deployments — there's already a `// TODO` for this in the code; not tackled here.
- Cloud deployment / hosting (Render, Railway, AWS, etc.) — code will be push-ready but not deployed.
- QR code generation, link analytics beyond click count, bulk shortening.

## 6. Configuration

New keys added to `application.properties` (all overridable via env vars, e.g. `APP_BASE_URL`):

```
app.base-url=http://localhost:8991
app.short-code-length=6
app.short-url.ttl-seconds=300
server.port=8991
```

`spring.h2.console.enabled=true` stays on for local debugging (`/h2Console`), disabled implicitly outside `dev` if we later add a profile — not needed for this scope.

## 7. UI/UX Impact

- **Home (`/`)**: single input field + "Shorten" button. On success, shows the short URL in a read-only field with a "Copy" button and the expiry time. Below it, a table of previously created links (short code, original URL, click count, expires-at) fetched from `/view`.
- **Redirect**: no visible page for the happy path — browser is sent straight to the destination via `302`.
- **Expired link**: dedicated page ("This link has expired") instead of a bare JSON/text response.
- **Not found / malformed code**: simple "Link not found" page instead of a bare status code.
- Plain CSS, mobile-responsive, no external UI framework — keeps the app self-contained with zero frontend build step.

## 8. Test Criteria

**Unit (UrlService):**
- Short code is 6 characters, URL-safe (no `+`, `/`, whitespace).
- Same long URL submitted twice produces two different valid short codes (or optionally the same one, depending on your call — currently always new).
- Code collision triggers a retry and still returns a unique code.
- Expired entries are not returned as valid (checked against both in-memory map and DB fallback).

**Integration (UrlController via MockMvc):**
- `POST /short` with a valid URL → `200` + non-empty short URL body.
- `POST /short` with empty/blank/malformed body → `400`.
- `GET /{validShortCode}` → `302` with correct `Location` header.
- `GET /{unknownCode}` → `404`.
- `GET /{expiredCode}` → `410` (or `403` if we keep current behavior) and lands on the expired page.
- `GET /view` → `200` + JSON body reflecting created links.

**Manual (browser):**
- Load `/`, submit a real URL, confirm the short link renders and copy button works.
- Click the short link in a new tab, confirm redirect to the original URL.
- Wait past the 5-minute TTL, click again, confirm the expired page shows.
- Submit an invalid string (e.g. `"not a url"`), confirm a clear error message in the UI (no raw stack trace/blank screen).

## 9. Acceptance Criteria

- Given a valid long URL, when submitted through the website, then a working short link is returned and displayed with a copy option.
- Given a valid short link within its TTL, when visited, then the browser is redirected to the original long URL.
- Given a short link past its TTL, when visited, then the user sees a clear "expired" page, not a raw error.
- Given an unknown short code, when visited, then the user sees a clear "not found" page.
- Given an invalid/empty URL submission, when submitted, then the UI shows a readable validation error, not a blank/broken state.
- `./mvnw test` passes with the new unit + integration tests.
- `./mvnw spring-boot:run` serves the working website at `http://localhost:8991` with no manual setup beyond starting the app.
- All changes are committed with clear messages and pushed to `origin/main` (or a branch you specify) on the existing GitHub repo.

## 10. Steps to Use (once built)

1. `cd usp && ./mvnw spring-boot:run`
2. Open `http://localhost:8991` in a browser.
3. Paste a long URL into the input, click **Shorten**.
4. Copy the returned short link.
5. Open the short link in a new tab to confirm it redirects correctly.
6. Visit `http://localhost:8991/view` any time to see all active links and click counts.
7. Wait 5+ minutes and revisit a short link to confirm the expired page appears.

---

### Open decisions before I start coding

- Keep expired-link status as `403` or switch to `410 Gone`?
- Any interest in custom aliases now, or keep that as a later phase (currently scoped out)?
- OK to push directly to `main`, or do you want a feature branch + you merge?
