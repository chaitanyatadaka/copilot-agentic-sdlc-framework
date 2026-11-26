# QueueLess Architecture

**Status:** Approved architecture; owner review decisions incorporated  
**Requirements baseline:** `docs/requirements/backlog-proposal.md` (REQ-001–REQ-058)  
**Scope:** One service centre, multiple queues, local-demo MVP

## 1. Architecture overview

QueueLess will use a **simple modular monolith**: one React/Vite browser application and one Spring Boot REST application, backed by a file-persisted H2 database.

The frontend is divided into customer and administrator experiences. The backend exposes REST APIs and owns all validation, lifecycle transitions, numbering, position calculations, and persistence. This keeps queue rules authoritative and prevents browser-only enforcement.

```text
Desktop/mobile browser
  ├─ Customer UI ─────┐
  └─ Administrator UI ─┼─ JSON/REST ─> Spring MVC controllers
                       │                 ├─ queue and token application services
                       │                 ├─ lifecycle / FIFO / numbering rules
                       │                 └─ Spring Data JPA repositories
                       └─ polling      H2 file database
```

No external notification, payment, physical-centre, or multi-tenancy service is included.

## 2. Requirements-aligned decisions

| Decision | Rationale | Trade-off |
|---|---|---|
| React + Vite for the frontend | Matches the proposed stack and provides a simple SPA suitable for desktop/mobile browsers. | Initial SPA load and additional browser state management; no server-side rendering. |
| Java + Spring Boot/Spring Web/Spring Data JPA | Matches the proposed backend stack and provides clear REST, validation, transaction, and persistence support. | More setup than a minimal framework, but appropriate maintainability for lifecycle concurrency rules. |
| H2 file database | Matches the proposed stack and permits persistence across normal restarts without external infrastructure. | Demo scale only; not a production-grade high-concurrency or HA database. |
| Modular monolith | QueueLess is one small service centre domain. A modular monolith avoids premature microservices. | All modules deploy together. |
| REST + JSON | Required by REQ-020 and works for both frontend experiences. | Polling is used instead of push updates. |
| Polling | Required MVP default: short 3–5 second intervals may be chosen in implementation; interval is not an acceptance threshold. | Update lag is accepted; no WebSocket/SSE dependency. |
| Pessimistic queue-row locking | Serializes joins and queue operations for strict FIFO and one-CALLED invariants. | Brief lock contention at the queue level; other queues remain independent. |
| Local-demo admin access | No production authentication is required; use a clearly marked demo boundary rather than claiming security. | Not suitable for untrusted or public deployment. |
| Single backend process | MVP runs exactly one Spring Boot backend process; queue invariants rely on transaction-scoped H2 row locks in that process. | Horizontal/multi-instance deployment is explicitly unsupported for this MVP. |
| Browser baseline | Support latest two stable versions of Chrome, Edge, Firefox, and Safari. | Browser automation and manual smoke checks must cover the declared baseline as practical in the environment. |

The proposed technologies in REQ-028–REQ-031 remain an implementation starting point. This architecture adopts them because they are proportionate to the requirements.

## 3. Architectural decisions and rationale

### AD-01: Queue and token aggregation
The `Queue` aggregate owns open/closed state, configuration, current local calendar day, last allocated sequence, and token invariants. Token operations execute through `QueueService`/`TokenService` transaction boundaries. Locking `Queue` serializes operations for that queue.

### AD-02: Strict FIFO
Waiting tokens are ordered by the queue's allocation sequence. Join increments the per-queue daily sequence and stores it on the token. Call selects `status = WAITING ORDER BY sequenceNumber ASC, id ASC`. The sequence is the oldest waiting position rather than a browser-supplied rank. A waiting token's position is the count of older WAITING tokens plus 1.

### AD-03: Daily numbering
Each Queue stores `numberingDay` and `lastSequenceNumber`. A join transaction compares `numberingDay` with the application's configured local date (`ASSUMP-001`) and resets both fields to today's date and 1 for the new allocation. The H2 unique constraint is `(queue_id, numbering_day, sequence_number)`. Historical display values are never rewritten.

If no join occurs overnight, no state mutation is required. The next join performs the lazy reset. This is per queue and has no cross-queue effect.

### AD-04: Duplicate joins
Inside the queue lock, a join rejects an existing WAITING token that matches the duplicate definition: same queue, same guest name, and optional phone. If both provide a phone, phone must match; if the submitted phone is absent, name alone is compared. If a submitted phone is present and no existing active record has that phone/name combination, it is not a duplicate. Names and phones are normalized for storage/comparison only as trim plus exact name matching and submitted phone normalization. This implements `ASSUMP-003` without inventing an identity system.

### AD-05: One CALLED invariant
Within a queue-locked transaction, call rejects if any CALLED token exists; completion or skip clears that state only after validation and within the same transaction. A pessimistic Queue lock prevents concurrent calls/completions for the queue. A database unique partial index for `(queue_id) WHERE status='CALLED'` is preferred if the selected H2/JPA version supports it; the queue lock remains the portable application invariant. No partial-index assumption is allowed to replace the transactional check.

### AD-06: H2 persistence mode
Use Spring profile `demo` with H2 file mode and JPA DDL configuration (for example `./data/queueless.mv.db`). Do not use in-memory H2 for the persistence-compliant default. Tests may use isolated H2 instances.

## 4. Components

### 4.1 Frontend modules

| Module | Responsibility | Requirement trace |
|---|---|---|
| `AppShell`/routing | Separate `/customer` and `/admin` experiences; shared API client/error handling. | REQ-001, REQ-021, REQ-055, REQ-056 |
| Customer queue directory | List open queues and allow selection. | REQ-002, REQ-037, REQ-052 |
| Join form | Name required; optional phone; client validation plus server-authoritative validation. | REQ-002, REQ-038, REQ-039, REQ-024 |
| Customer token view | Poll token status and position; display prefix/number, estimated wait, cancel control. | REQ-003–REQ-006, REQ-017, REQ-023, REQ-055 |
| Admin queue list | Create/configure and open/close queues. | REQ-011, REQ-037, REQ-050–REQ-052 |
| Admin dashboard | Poll selected queue totals and active/called tokens; call/complete/skip actions. | REQ-007–REQ-012, REQ-053, REQ-054, REQ-055 |
| Shared API/error components | Typed payload parsing, loading/error states, conflict messages, polling lifecycle. | REQ-024, REQ-049, REQ-058 |

### 4.2 Backend packages

| Package/component | Responsibility | Requirement trace |
|---|---|---|
| `config` | Clock, H2/JPA configuration, CORS for local frontend origin, demo profile. | REQ-015, REQ-019, REQ-057 |
| `api` | REST controllers, DTO validation, HTTP status/error mapping. | REQ-020, REQ-024, REQ-058 |
| `queue` | Queue CRUD/configuration/open-close and queue-level totals. | REQ-011, REQ-037, REQ-050–REQ-053 |
| `token` | Join, display, position/estimate, cancellation and lifecycle transitions. | REQ-002–REQ-010, REQ-013, REQ-017, REQ-018 |
| `domain` | Enums and domain invariants for lifecycle/FIFO/daily numbering. | REQ-006, REQ-008–REQ-018, REQ-047–REQ-049 |
| `persistence` | JPA entities, repositories and constraints. | REQ-019, REQ-022, REQ-023 |
| `polling` support | Snapshot endpoints with persisted current state. | REQ-055 |

## 5. Data design

### Queue

| Column | Constraint |
|---|---|
| `id` | BIGINT/UUID primary key |
| `name` | NOT NULL and nonblank after trimming |
| `prefix` | NOT NULL and nonblank after trimming; assumed unique |
| `average_service_duration_minutes` | NOT NULL and positive integer |
| `open` | NOT NULL boolean |
| `numbering_day` | NOT NULL ISO local date, updated by join transaction |
| `last_sequence_number` | NOT NULL, >= 0 |
| `created_at`, `updated_at` | NOT NULL timestamps |

Prefix uniqueness is an application-validated assumption (`ASSUMP-004`) and should also have a unique database constraint. Queue name does not require uniqueness unless approved.

### Token

| Column | Constraint |
|---|---|
| `id` | Primary key |
| `queue_id` | NOT NULL foreign key to Queue |
| `numbering_day` | NOT NULL local date |
| `sequence_number` | NOT NULL > 0 |
| `display_identifier` | NOT NULL, immutable snapshot of prefix plus number |
| `customer_name` | NOT NULL and nonblank after trimming |
| `phone` | NULL or trimmed/normalized |
| `status` | NOT NULL enum |
| `joined_at` | NOT NULL |
| `called_at`, `completed_at`, `skipped_at`, `cancelled_at` | Nullable status transition timestamps |
| `version` | JPA optimistic version for entity-level protection |

Unique constraints:
- `(queue_id, numbering_day, sequence_number)` prevents duplicate numbering.
- `(queue_id, prefix snapshot?)` is not required because display identifier is derived/snapshotted.
- `(queue_id, status)` partial/indicated one-CALLED invariant is enforced in transaction; a partial unique index is desirable but not required in the portable port.

Indexes:
- `idx_token_queue_status_sequence(queue_id, status, sequence_number)` for FIFO and waiting queries.
- `idx_token_queue_status(queue_id, status)` for counts/active lists.
- `idx_token_queue_day_sequence(queue_id, numbering_day, sequence_number)` for daily uniqueness and history.
- `idx_token_queue_status_joined(queue_id, status, joined_at)` for duplicate checks and oldest fallback.
- `idx_queue_prefix(prefix)` unique for configured prefix.

No customer account or authentication identity is stored. Phone is operational guest input only.

## 6. REST API contract

Base path: `/api`.

All mutation endpoints perform validation, acquire the queue lock, validate persisted state, mutate, and commit once. No endpoint exposes database exception text.

| Method and path | Purpose |
|---|---|
| `GET /api/queues` | List queue summaries |
| `POST /api/queues` | Create queue |
| `GET /api/queues/{queueId}` | Queue configuration/status |
| `PUT /api/queues/{queueId}` | Update name, prefix, fixed duration atomically |
| `PATCH /api/queues/{queueId}/status` | Open or close |
| `POST /api/queues/{queueId}/join` | Join and allocate token |
| `GET /api/queues/{queueId}/dashboard` | Status counts and active/called tokens |
| `GET /api/tokens/{tokenId}` | Customer token snapshot |
| `POST /api/tokens/{tokenId}/cancel` | Cancel WAITING token |
| `POST /api/queues/{queueId}/call-next` | Call oldest WAITING token |
| `POST /api/tokens/{tokenId}/complete` | Complete CALLED token |
| `POST /api/tokens/{tokenId}/skip` | Skip CALLED token |

Representative join request:

```json
{ "customerName": "Sam", "phone": "+15551234567" }
```

Representative success:

```json
{
  "tokenId": 42,
  "displayIdentifier": "A-12",
  "queueId": 3,
  "status": "WAITING",
  "position": 4,
  "estimatedWaitMinutes": 18
}
```

Representative queue response:

```json
{
  "queueId": 3,
  "name": "Returns",
  "prefix": "A",
  "averageServiceDurationMinutes": 6,
  "open": true,
  "dailySequenceResetOn": "2026-11-24",
  "lastSequenceNumber": 12
}
```

Representative dashboard:

```json
{
  "queueId": 3,
  "counts": {
    "WAITING": 7,
    "CALLED": 1,
    "COMPLETED": 20,
    "SKIPPED": 2,
    "CANCELLED": 3
  },
  "activeTokens": []
}
```

Representative error:

```json
{
  "error": "CONFLICT",
  "message": "This token cannot be cancelled in its current state.",
  "fieldErrors": []
}
```

### HTTP status mapping

- `200 OK`: successful retrieve/update/transition.
- `201 Created`: queue or token created.
- `400 Bad Request`: malformed JSON, Bean Validation failure, or invalid request fields.
- `404 Not Found`: unknown queue/token.
- `409 Conflict`: closed queue, duplicate join, existing CALLED, invalid lifecycle transition, optimistic conflict, or sequence conflict.
- `500 Internal Server Error`: generic `INTERNAL_ERROR`; details not exposed.

These mappings are fixed for the MVP; no 422 response is used.

## 7. Validation and error handling

- Trim and validate queue name, prefix and duration.
- Queue prefix: nonblank and unique, with no additional character policy inferred from requirements.
- Duration: positive integer in minutes, with no owner-approved maximum.
- Name required and nonblank.
- Phone optional; trim/normalize when present and perform no external format validation.
- Queue/token IDs must parse and exist.
- Lifecycle transitions validate both ID and current persisted state.
- Domain exceptions are translated at a single API exception handler.
- Mutations use `@Transactional`; exceptions result in rollback and no partial state.
- Frontend displays `message` and field errors, never stack traces or SQL details.

## 8. Data flow and calculations

### Join
1. Controller validates the body.
2. Transaction loads Queue with pessimistic lock.
3. Reject if closed.
4. Reset local numbering day/sequence if the date changed.
5. Reject duplicate WAITING identity.
6. Allocate next sequence and create `WAITING` token.
7. Commit once and return token snapshot with position/estimate.

### Call
1. Lock Queue.
2. Reject if a CALLED token exists.
3. Select oldest WAITING token.
4. Set CALLED and called timestamp.
5. Commit.

### Complete/skip
1. Lock Queue and load the selected Token.
2. Verify token belongs to queue and is CALLED.
3. Set terminal state and timestamp in the same transaction.
4. Commit.

### Cancel
1. Load token under queue lock.
2. Verify WAITING.
3. Set CANCELLED and timestamp.
4. Commit.

### Position
For a WAITING token, position = 1 + count of WAITING tokens in the same queue where `(sequenceNumber, id)` precedes this token. CALLED and terminal statuses are excluded.

### Estimate
Preceding waiting count = position - 1. Estimated minutes = preceding waiting count × queue `averageServiceDurationMinutes`; for non-WAITING tokens the wait estimate is unavailable rather than an invalid estimate. Queue duration is not null due to entity constraint.

When the queue duration or waiting set changes, subsequent polling responses recompute; historical tokens are not rewritten.

## 9. Concurrency and transactions

All join/call/complete/skip/cancel/configuration mutations use:
- `@Transactional`
- pessimistic queue-row lock for aggregate operations
- re-read of persisted status before transition
- optimistic `@Version` on Token as defense in depth

The queue lock serializes operations per queue. It prevents concurrent joins from duplicate number allocation and concurrent calls from creating two CALLED tokens. There is no global lock, so independent queues can progress concurrently.

The deployment must run exactly one Spring Boot backend process. Concurrency protection is provided by transactional H2 queue-row locks and persisted-state rechecks, not a process-local Java mutex. The H2 transaction isolation/JPA setup must be tested with concurrent requests inside that single process. A second backend process or horizontal scaling is unsupported. The position query executes from persisted state, not from in-memory counters.

## 10. Polling

Customer view polls token status and queue join view while waiting. Admin dashboard polls queue dashboard and configuration. A short implementation-selected interval (proposed 3 seconds) is not a correctness threshold. Polls use cache-safe API GETs and stop where appropriate; no push infrastructure is used.

## 11. Security boundary

The MVP has no production-grade authentication. The frontend routes to customer/admin views, and backend admin operations are intentionally demo-only.

Deployment should:
- bind demo access to local host or a controlled network,
- use no real personal data,
- avoid exposing H2 or data directories,
- display a local-demo label,
- mark all admin endpoints as demo-only in code/configuration,
- never claim production security.

This satisfies REQ-057/BR-013 and excludes REQ-032. A future production authentication design is outside scope.

## 12. Deployment and configuration

### Local demo
- Run Spring Boot on `http://localhost:8080`.
- Run Vite on its development port and proxy `/api` to Spring Boot, or configure a matching CORS origin.
- H2 file database under a configurable path outside source.
- Configure local timezone/calendar day for ASSUMP-001.
- Run frontend and backend separately for development.

### Configuration properties
- `queueless.database-path`
- `queueless.local-date-zone` (default system zone, explicit configuration preferred)
- `queueless.admin-demo-enabled=true`
- optional `queueless.cors.allowed-origin`

No secrets are required. Do not commit the H2 database or personal demo data.

## 13. Testing architecture

- JUnit 5 unit tests for numbering, lifecycle, FIFO, position and estimates.
- Mockito tests for service dependencies.
- Spring Data/JPA integration tests against isolated H2.
- MockMvc API tests for all operations and errors.
- Essential Playwright/Cypress or similarly appropriate browser tests for desktop/mobile journeys.
- React component tests for form validation and polling rendering.
- Supported browser baseline: latest two stable versions of Chrome, Edge, Firefox, and Safari. Record the browser/version and viewport for each executed E2E or smoke test; document any environment limitation rather than treating an unavailable browser as passed.
- Basic usability verification: manually or with essential browser checks complete the primary customer join/track/cancel journey and administrator configure/call/complete/skip journey; confirm primary actions and validation/errors are visible and usable at desktop and mobile layouts, with no blocking layout defects. This adds verification only, not product scope.

## 14. Risks and limitations

| Risk | Effect | Mitigation/limitation |
|---|---|---|
| H2 file database is demo-scale | No HA, horizontal scale or production backup behavior | Use a single application instance; document limitation; H2 file mode is sufficient for MVP/restart. |
| Polling latency | Status can lag briefly | Short interval implementation detail; use on-demand refresh and endpoint timestamps; no push guarantee. |
| Name/phone duplicate matching ambiguity | Definition does not include case or formatting rules | Implement documented trim/exact/optional-phone behavior and test it; unresolved case policy remains limited to explicit matching behavior. |
| Prefix uniqueness assumption | Two prefixes assumed unique by MVP assumption | Enforce unique constraint and reject duplicates; no owner decision for broader behavior. |
| Local-demo security | Public/network access could permit administrator actions | Bind locally/controlled network and label demo only. |
| Local clock/calendar day | A changed system zone may alter reset | Configure zone explicitly and test midnight boundary. |
| In-process singleton scaling | A second backend instance may break lock assumptions | MVP deployment is one process; multiple instances are outside scope. |

## 15. Out of scope

Do not implement:
- SMS notifications (REQ-036)
- online payments (REQ-035)
- multiple physical service centres (REQ-033)
- multi-tenancy (REQ-034)
- production authentication (REQ-032)
- external push services
- historical analytics

## 16. Complete requirement-to-component traceability

| Requirement | Architecture component |
|---|---|
| REQ-001 | AppShell/routes; customer and admin modules |
| REQ-002 | Join form; QueueService.join; Queue OPEN check |
| REQ-003 | TokenService allocation; unique sequence constraint |
| REQ-004 | Token snapshot position calculation |
| REQ-005 | Estimate calculation |
| REQ-006 | Cancel transition and lifecycle matrix |
| REQ-007 | Dashboard active token query |
| REQ-008 | Call-next service |
| REQ-009 | Complete transition |
| REQ-010 | Skip transition |
| REQ-011 | Queue CRUD/status |
| REQ-012 | Dashboard totals |
| REQ-013 | FIFO sequence query under queue lock |
| REQ-014 | Queue lock plus CALLED check |
| REQ-015 | Queue numbering_day/last_sequence reset |
| REQ-016 | Join closed check |
| REQ-017 | Waiting-only position/count queries |
| REQ-018 | Token lifecycle enum |
| REQ-019 | JPA/H2 file persistence |
| REQ-020 | REST API |
| REQ-021 | Simple responsive frontend |
| REQ-022 | `queue/day/sequence` uniqueness and allocation transaction |
| REQ-023 | Persisted snapshot queries and recalculation |
| REQ-024 | Bean/Jakarta validation and API error handler |
| REQ-025 | Modular packages and testable services |
| REQ-026 | Transaction locking, versioning and state checks |
| REQ-027 | JUnit/Mockito, Spring/API/component/browser tests |
| REQ-028 | React/Vite frontend |
| REQ-029 | Java/Spring backend |
| REQ-030 | H2 persistence |
| REQ-031 | Testing stack |
| REQ-032 | Demo boundary; no auth implementation |
| REQ-033 | Single service-centre configuration |
| REQ-034 | No tenant entity or routing |
| REQ-035 | No payment component |
| REQ-036 | No SMS integration |
| REQ-037 | Queue entity and queue list |
| REQ-038 | Join DTO/customer_name validation |
| REQ-039 | Optional phone field |
| REQ-040 | Duplicate query under queue lock |
| REQ-041 | Per-Queue numbering fields |
| REQ-042 | Queue prefix |
| REQ-043 | Display identifier snapshot |
| REQ-044 | Queue average duration |
| REQ-045 | Estimate formula |
| REQ-046 | Position minus 1 estimate |
| REQ-047 | SKIPPED terminal state |
| REQ-048 | Rejoin creates new token |
| REQ-049 | State-conflict exceptions/transaction rollback |
| REQ-050 | Queue create/update name/prefix |
| REQ-051 | Queue duration create/update |
| REQ-052 | Open/close endpoint |
| REQ-053 | Five-status dashboard |
| REQ-054 | Call-next does not accept token selection |
| REQ-055 | Polling frontend and GET snapshots |
| REQ-056 | Responsive browser UI |
| REQ-057 | Local/demo mode boundary |
| REQ-058 | Controller/domain validation, atomic mutation |
