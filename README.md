# Mini Ticket Tracker

A small support-ticket tracker: a REST API in Java with Spring Boot, MySQL for storage and a
single-page Angular UI.

## Stack

| Layer    | Choice                                                              |
|----------|---------------------------------------------------------------------|
| API      | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Bean Validation)       |
| Database | MySQL 8.4, Flyway migrations                                        |
| UI       | Angular 22 (standalone components, signals, reactive forms)         |
| Tests    | JUnit 5 + MockMvc against MySQL in Testcontainers, plus unit tests  |

## Running it

Prerequisites: JDK 21+, Node 20+, and Docker (for MySQL and the tests).

### 1. Start MySQL

The bundled compose file runs MySQL only and maps it to port `53306` to avoid clashing with a
local install:

```bash
docker compose up -d db
```

To use your own MySQL 8 instead, set `DATABASE_URL` (JDBC URL), `DATABASE_USER` and
`DATABASE_PASSWORD`.

### 2. Start the API

```bash
cd api
./mvnw spring-boot:run        # Flyway migrates on boot, listens on :8080
```

Alternatively, skip step 1: `./mvnw spring-boot:test-run` starts the app with a throwaway MySQL
container via Testcontainers (see `TestTicketTrackerApplication`).

### 3. Start the UI

```bash
cd web
npm install
npm start                     # http://localhost:4200
```

The UI calls `/api/*`, and the Angular dev server proxies that to the API with the prefix
stripped (`proxy.conf.mjs`). If the API is not on `localhost:8080`, set `API_URL`, e.g.
`API_URL=http://localhost:9000 npm start`.

### Running the tests

```bash
cd api
./mvnw test
```

Docker must be running. Testcontainers starts a `mysql:8.4` container for the integration tests,
Flyway applies the real migrations to it, and each test starts from an empty `tickets` table.

- `TicketApiIntegrationTest`: create (defaults, trimming, persistence, validation), list (status
  filter, pagination, ordering, bad filter), update (partial, 404, non-updatable field) and stats.
- `TicketQueriesTest`: unit tests for query-string and PATCH-body parsing.

## API

All responses are JSON. Errors share one shape:

```json
{ "error": { "code": "validation_error", "message": "Request validation failed",
             "fields": { "title": "is required" } } }
```

| Method  | Path             | Notes |
|---------|------------------|-------|
| `POST`  | `/tickets`       | Body: `title` (required, 1–200 characters after trimming), `description`, `priority` (default `medium`), `status` (default `open`). Returns `201`. |
| `GET`   | `/tickets`       | Query: `status`, `page` (default 1), `limit` (default 20, max 100). Returns `{ data, pagination: { page, limit, total, total_pages } }`, newest first. |
| `PATCH` | `/tickets/{id}`  | Body: `status` and/or `priority`. Other fields are rejected with `400`; unknown id is `404`. |
| `GET`   | `/tickets/stats` | `{ "open": n, "in_progress": n, "closed": n }`, always all three keys. |

```bash
curl -X POST localhost:8080/tickets -H 'Content-Type: application/json' \
  -d '{"title": "Printer on fire", "priority": "high"}'
curl 'localhost:8080/tickets?status=open&page=1&limit=10'
curl -X PATCH localhost:8080/tickets/1 -H 'Content-Type: application/json' -d '{"status": "closed"}'
curl localhost:8080/tickets/stats
```

## Project layout

```
api/src/main/java/com/lodwar/tickets/
  ticket/
    Ticket.java                 JPA entity
    Status.java, Priority.java  enums with their lowercase wire/database values
    EnumConverters.java         JPA converters so columns hold "in_progress", not ordinals
    TicketDtos.java             request/response records (Bean Validation on the create body)
    TicketQueries.java          query-string and PATCH-body parsing
    TicketRepository.java       Spring Data repository + GROUP BY projection
    TicketService.java          transactions and business rules
    TicketController.java       HTTP layer
  web/ApiExceptionHandler.java  every error rendered in the shared JSON shape
api/src/main/resources/
  application.yml               env-driven config, snake_case JSON
  db/migration/                 Flyway migrations
web/src/app/
  app.ts                        page state (signals): filter, page, stats, reloads
  tickets/                      API service, form, table, stats bar
```

## Decisions & Tradeoffs

I went with the conventional Spring stack: Data JPA for persistence, Flyway owning the schema with
Hibernate set to `validate`, and Bean Validation on the create request record. Status and priority
are Java enums persisted through `AttributeConverter`s as their lowercase API values, so the table,
its `CHECK` constraints and the JSON all agree. A JPA ordinal or constant name would leak an
internal detail into the database. The `PATCH` body is read as a map and checked by hand, because
Spring Boot's Jackson setup ignores unknown properties, and I wanted an attempt to edit the title
rejected rather than silently dropped. JSON uses a global `SNAKE_CASE` naming strategy, so the
records stay idiomatic Java while the wire format matches the contract (`created_at`,
`total_pages`). The integration tests use Testcontainers with the same MySQL version as compose,
which costs some startup time but tests the real Flyway migrations and SQL rather than an H2
approximation. Pagination is offset-based on `created_at DESC, id DESC` with matching indexes,
which is fine at this size but would become keyset pagination for large tables. Shortcuts I took:
no authentication, no frontend tests, Jackson's default leniency for scalars (e.g. a numeric
`title` is coerced to a string), and the UI re-fetches after each change rather than caching.
With more time I'd add Spring Security, springdoc-openapi, Actuator health and metrics, Angular
component tests, CI running `./mvnw verify`, and container images for the API and UI.

### Assumptions

- `id` is an auto-incrementing integer; UUIDs weren't needed for a single-database app.
- A new ticket may be created with any status, but defaults to `open` / `medium`.
- A blank description is stored as `NULL`.
- `limit` above 100 is rejected rather than silently clamped.
- Stats count all tickets and ignore the list's filter and pagination.
