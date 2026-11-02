# Learning notes

Short explanations of the concepts used in TicketTriage, written so I can explain them in an interview.
Where it helps, I compare them with Node/Express, which I already know.

---

## Milestone 1 — Backend foundations (Spring Boot 4.1, JPA, Flyway, PostgreSQL)

### Spring Boot in one paragraph
Spring Boot is an opinionated layer on top of the Spring Framework. You add **starters**
(e.g. `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`) and **auto-configuration** sets up
sensible beans for whatever is on the classpath: an embedded Tomcat server, Jackson for JSON, a Hikari
connection pool, Hibernate and so on. `@SpringBootApplication` turns on auto-configuration and
**component scanning** (it finds `@RestController`, `@Service` and similar classes in its package and sub-packages).
*Express analogy:* instead of wiring `app.use(express.json())`, a DB pool and an ORM by hand, Boot
wires them for you, and you override them only when you need something different.

### Dependency injection and constructor injection
Spring creates objects (**beans**) and passes their dependencies in for you. I use **constructor injection**:
`TicketService` asks for a `TicketRepository` in its constructor, and Spring supplies it.
- Dependencies can be `final`, so the object is always fully initialised.
- Easy to unit test: just `new TicketService(mockRepository)`, with no Spring needed.
- If a class has a single constructor, `@Autowired` is not needed.

### Layering: controller → service → repository
| Layer | Class | Responsibility |
|---|---|---|
| Controller | `TicketController` | HTTP only: routes, status codes, `@Valid`, converting to DTOs |
| Service | `TicketService` | Business logic and transaction boundaries |
| Repository | `TicketRepository` | Database access (Spring Data) |

**DTOs vs entities.** The `Ticket` entity mirrors the table. DTOs (`CreateTicketRequest`, `TicketResponse`, ...) are
Java **records** that define the API contract. Keeping them separate means:
- a DB change doesn't silently change the API;
- clients can't set fields they shouldn't (`id`, `createdAt`);
- we avoid lazy-loading and serialization surprises from exposing entities directly.

Mapping is a plain static method, `TicketResponse.from(ticket)`, with no mapping library.

### JPA / Hibernate entity
- `@Entity` + `@Table(name = "tickets")` maps the class to the table. Column names are derived automatically
  (`customerEmail` → `customer_email`).
- `@Id @GeneratedValue(strategy = IDENTITY)`: Postgres generates the id (`GENERATED ALWAYS AS IDENTITY`).
- `@Enumerated(EnumType.STRING)` stores `"BILLING"` rather than the ordinal `0`. Ordinals break as soon as someone
  reorders the enum.
- `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate) fill `created_at` and `updated_at`.
- JPA requires a no-argument constructor; I made it `protected` so application code uses the public
  constructor, which sets the defaults (`OTHER`, `MEDIUM`, `NEUTRAL`, `OPEN`).

### Spring Data JPA repositories
`TicketRepository extends JpaRepository<Ticket, Long>` is only an interface; Spring generates the
implementation at runtime (`save`, `findById`, `findAll`, paging, sorting...).
- **Custom JPQL** with `@Query`: the stats use `select t.category as category, count(t) as total ... group by t.category`.
  JPQL queries entity classes and fields, not tables. Hibernate translates it into SQL `GROUP BY`.
- **Interface projections** (`CategoryCount`): Spring Data maps each row onto the getters by alias, so the query
  only fetches the columns it needs.
- **Query by Example** for the optional filters: I build a "probe" `Ticket` with only the filters that were provided.
  Null fields are ignored, so `?category=BILLING` becomes `WHERE category = 'BILLING'` and no filters returns
  everything. That's simpler than writing one query per filter combination.

### Transactions
- `@Transactional(readOnly = true)` on the service class is the default for reads. Methods that write override it with
  `@Transactional`. If an exception escapes the method, the transaction rolls back.
- **Dirty checking:** inside a transaction, changing a managed entity (`ticket.setStatus(...)`) is enough for Hibernate
  to issue an `UPDATE` at commit. I call `saveAndFlush` in `update()` so the `UPDATE` happens *before* the response is built,
  which means the response contains the new `updatedAt`.
- `spring.jpa.open-in-view: false` closes the DB session when the service returns, instead of keeping it open while
  JSON is rendered. This avoids hidden lazy-loading queries in the web layer and is a common interview topic.

### Flyway migrations
- Versioned SQL files in `db/migration` (`V1__create_tickets.sql`) run once, in order, at startup. Flyway records them in the
  `flyway_schema_history` table. *Analogy:* knex or Prisma migrations.
- `spring.jpa.hibernate.ddl-auto: validate`: Hibernate never changes the schema; it only checks that entities match it.
  Flyway owns the schema, so every change is reviewed SQL in git.
- **Dev-only seed data:** `db/seed/V1_1__seed_sample_tickets.sql` is only on Flyway's path when the `dev` profile is active
  (`spring.flyway.locations` in `application-dev.yml`). Tests and production never load it.
- The DB enforces the enum values with `CHECK` constraints, as a safety net beyond the Java code.

### Configuration and profiles
- `application.yml` holds shared settings. `application-dev.yml` adds to or overrides them when `SPRING_PROFILES_ACTIVE=dev`.
- **Relaxed binding:** the env var `SPRING_DATASOURCE_URL` overrides `spring.datasource.url`. Production
  (Azure) sets the DB settings as env vars, so no credentials live in the code.
- Precedence, roughly: command-line args > env vars > profile yml > base yml.

### Validation
- Bean Validation annotations on the request record: `@NotBlank`, `@Email`, `@Size(max = ...)`.
- `@Valid @RequestBody` in the controller triggers validation *before* the method runs. On failure Spring throws
  `MethodArgumentNotValidException`. *Analogy:* a zod or Joi middleware.

### Global error handling and ProblemDetail
- `@RestControllerAdvice` is one place that turns exceptions into HTTP responses. *Analogy:* an Express error middleware.
- Errors use **ProblemDetail (RFC 9457)**, a standard JSON error shape: `type`, `title`, `status`, `detail`, `instance`.
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler`, which already maps Spring MVC errors
  (malformed JSON, bad enum in a query param, wrong HTTP method) to 4xx ProblemDetails. I added:
  - `TicketNotFoundException` → 404;
  - validation errors → 400 with an `errors` map (`field → message`) that the frontend can show next to inputs.

### REST details
- `POST` returns **201 Created** with a `Location` header pointing at the new resource.
- `PATCH` is a **partial update**: every field in `UpdateTicketRequest` is optional and only non-null fields are applied.
- Enum query params (`?priority=HIGH`) are converted automatically, and invalid values return 400.

### Actuator and OpenAPI
- **Actuator** `/actuator/health` (plus `/liveness` and `/readiness`) is used later for Docker healthchecks and Azure
  Container Apps probes. Only `health` is exposed over HTTP.
- **springdoc-openapi** generates an OpenAPI 3 spec from the controllers (`/v3/api-docs`) and serves Swagger UI
  (`/swagger-ui.html`).

### Testing
- **Unit tests** (`TicketServiceTest`) use JUnit 5, Mockito and AssertJ. The repository is mocked with `@Mock`, the service gets it
  via `@InjectMocks`, and there's no Spring context, so they run in milliseconds. `ArgumentCaptor` checks *what* the service passed to
  the repository (filters, sort order).
- **Integration test** (`TicketApiIntegrationTest`):
  - `@SpringBootTest` starts the full application context.
  - `@AutoConfigureMockMvc` sends HTTP requests through the real controller stack without opening a port.
  - **Testcontainers** starts a real `postgres:17-alpine` container, and `@ServiceConnection` wires the DataSource to it, so the
    actual Flyway migrations, CHECK constraints and `GROUP BY` are tested. H2 would only be an imitation of Postgres.
  - `@Transactional` on the test class rolls back after each test, so tests stay independent.

### Spring Boot 4 vs 3 (likely interview question)
- Boot 4 (Nov 2025) is built on Spring Framework 7 and Jakarta EE 11, and uses **Jackson 3** and Hibernate 7. The baseline is still Java 17.
- Auto-configuration was split into smaller modules, so starters are more specific: `spring-boot-starter-webmvc`
  (formerly `-web`), and Flyway now needs `spring-boot-starter-flyway`. Test support has per-module starters
  (`spring-boot-starter-webmvc-test`), and `@AutoConfigureMockMvc` moved package.
- Concepts (DI, starters, profiles, JPA, validation, error handling) are unchanged, so everything here applies to 3.x codebases too.
- Boot 3.5 open-source support ended June 2026, which is why this project uses 4.1.

### Quick interview answers
- *Why constructor injection?* Immutable dependencies, it's obvious what a class needs, and it's easy to test without Spring.
- *Why DTOs?* They decouple the API from the DB schema and prevent over-posting.
- *Why Flyway with `ddl-auto=validate`?* Reproducible, reviewable schema changes, and the app fails fast if the code and DB drift apart.
- *Why Testcontainers over H2?* It tests against the same database engine as production.
- *What does `@Transactional` do?* Spring wraps the method in a proxy that begins and commits the transaction (or rolls it back on a
  runtime exception). Note that a call from inside the same class bypasses the proxy.
