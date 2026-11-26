# QueueLess Implementation Plan

**Status:** Approved implementation plan; owner review decisions incorporated  
**Architecture:** `docs/architecture.md`  
**Requirements:** `docs/requirements/backlog-proposal.md` (REQ-001–REQ-058)

Implementation must not begin until architecture and this plan receive explicit approval.

## 1. Phase summary

| Phase | Goal | Exit evidence |
|---|---|---|
| P0 | Repository and application foundation | Builds run; H2 profile and test configuration available |
| P1 | Persistent queue and token domain | Entities, constraints and repositories pass integration tests |
| P2 | Core queue/token services | Join, numbering, lifecycle, FIFO, position and estimate rules pass unit/integration tests |
| P3 | REST APIs | API contract and error behavior pass MockMvc/API tests |
| P4 | React customer experience | Join/tracking/cancel journey passes component and browser tests |
| P5 | React administrator experience | Dashboard/configure/call/complete/skip journey passes component/browser tests |
| P6 | Concurrency, quality and verification | Concurrent test evidence, full suite and documented limitations |

## 2. Stable task list

### Phase P0 — Foundation

#### T-P0-01 — Establish backend and frontend workspaces
- Description: Add React/Vite and Spring Boot project structure according to the approved architecture; keep Copilot/docs separate from application code.
- Requirements: REQ-025, REQ-028, REQ-029, REQ-031
- Dependencies: None
- Expected outputs: Buildable frontend/backend skeleton, package boundaries, standard scripts.
- Tests/evidence: Frontend build, backend build, startup smoke test.
- Completion: Both projects build and start without application feature implementation errors.

#### T-P0-02 — Configure H2 file persistence and test profiles
- Description: Configure a demo H2 file database, JPA, Flyway/Liquibase only if a small migration approach is selected, and isolated test H2.
- Requirements: REQ-019, REQ-030, REQ-033, REQ-034
- Dependencies: T-P0-01
- Expected outputs: Persistence configuration and configurable database/local-zone properties.
- Tests/evidence: Normal restart integration test, test-profile isolation check.
- Completion: A persisted record survives a normal backend restart and test data cannot leak into demo storage.

#### T-P0-03 — Establish domain/config conventions
- Description: Add Clock/zone configuration, lifecycle enum, error envelope, common validation and package structure.
- Requirements: REQ-015, REQ-018, REQ-024, REQ-057, REQ-058
- Dependencies: T-P0-01
- Expected outputs: Clock bean, enums, exception types, API error handler skeleton.
- Tests/evidence: Configuration/unit tests for local clock and error mapping.
- Completion: Services use an injected Clock and all domain/API errors have a stable public shape.

### Phase P1 — Persistence and domain model

#### T-P1-01 — Implement Queue entity and repository
- Description: Implement Queue fields, open state, prefix, fixed duration, local numbering day and sequence; unique prefix constraint and validation.
- Requirements: REQ-011, REQ-037, REQ-041, REQ-042, REQ-044, REQ-050, REQ-051, REQ-052
- Dependencies: T-P0-02, T-P0-03
- Expected outputs: Queue JPA entity/repository and schema.
- Tests/evidence: JPA integration tests for persistence, independent numbering fields and prefix constraint.
- Completion: Queue records persist across restart and invalid/duplicate-prefix values are rejected.

#### T-P1-02 — Implement Token entity and repository indexes
- Description: Implement lifecycle enum, immutable display snapshot, customer fields, timestamps and queue/day/sequence uniqueness; add required indexes.
- Requirements: REQ-003, REQ-018, REQ-019, REQ-022, REQ-038, REQ-039, REQ-043
- Dependencies: T-P1-01
- Expected outputs: Token entity/repository and schema migration.
- Tests/evidence: JPA integration tests and schema constraint tests.
- Completion: Every token belongs to a queue and has exactly one lifecycle enum; duplicate sequence and invalid associations are rejected.

#### T-P1-03 — Implement persisted status counts and waiting queries
- Description: Add repository queries for counts, oldest waiting token, waiting-by-sequence and active token dashboard data.
- Requirements: REQ-007, REQ-012, REQ-017, REQ-053
- Dependencies: T-P1-02
- Expected outputs: Efficient status/count queries.
- Tests/evidence: Integration tests with mixed lifecycle states.
- Completion: Five totals are correct, waiting queries exclude all terminal/CALLED rows and terminal states do not count as waiting.

### Phase P2 — Domain services

#### T-P2-01 — Implement queue configuration service
- Description: Create/update queue name/prefix/duration and open/close state atomically.
- Requirements: REQ-011, REQ-050, REQ-051, REQ-052, REQ-058
- Dependencies: T-P1-01
- Expected outputs: Queue command methods and domain exceptions.
- Tests/evidence: Unit/integration tests for success, invalid updates and open/close behavior.
- Completion: Invalid configuration produces no state change and open/closed state persists.

#### T-P2-02 — Implement join and daily numbering service
- Description: Lock Queue; reset numbering day/sequence independently; validate open/duplicate/name/phone; allocate one unique token atomically.
- Requirements: REQ-002, REQ-003, REQ-015, REQ-016, REQ-022, REQ-038, REQ-039, REQ-040, REQ-041, REQ-042, REQ-043
- Dependencies: T-P1-02
- Expected outputs: `joinQueue` operation and numbering logic.
- Tests/evidence: Unit and concurrent integration tests for midnight, independent queues, closed joins and duplicates.
- Completion: Exactly one WAITING token is created for each successful join; concurrent joins receive distinct numbers; closed/duplicate joins change no persisted state.

#### T-P2-03 — Implement duplicate matching
- Description: Implement the documented same-name/optional-phone `ASSUMP-003` check inside the queue transaction.
- Requirements: REQ-040
- Dependencies: T-P2-02
- Expected outputs: Duplicate predicate and repository query.
- Tests/evidence: Unit/integration matrix for name-only, matching phone, differing phone and terminal status.
- Completion: An active WAITING match rejects join; terminal or different identity can rejoin according to rules.

#### T-P2-04 — Implement strict FIFO call-next
- Description: Lock Queue; reject existing CALLED; select oldest WAITING and transition CALLED.
- Requirements: REQ-008, REQ-013, REQ-014, REQ-054
- Dependencies: T-P1-03, T-P2-02
- Expected outputs: `callNext` service.
- Tests/evidence: Unit/concurrency tests for oldest selection and second call rejection.
- Completion: Call is FIFO and at most one CALLED exists per queue in all tested concurrent calls.

#### T-P2-05 — Implement completion and skip transitions
- Description: Validate CALLED state and owner queue, then complete or skip in the same queue transaction.
- Requirements: REQ-009, REQ-010, REQ-018, REQ-047, REQ-048
- Dependencies: T-P2-04
- Expected outputs: Complete/skip operations and terminal timestamps.
- Tests/evidence: Unit/integration tests for accepted and invalid transitions.
- Completion: Only CALLED can be completed/skipped; terminal states persist and allow no further normal transition.

#### T-P2-06 — Implement waiting cancellation
- Description: Lock Queue/token aggregate and set WAITING to CANCELLED only; reject called/terminal/repeated cancellation.
- Requirements: REQ-006, REQ-017, REQ-018, REQ-026
- Dependencies: T-P2-02
- Expected outputs: Cancel operation.
- Tests/evidence: Unit/integration repeated/conflict tests.
- Completion: Only WAITING is cancelled; failed cancellation leaves all persistence unchanged.

#### T-P2-07 — Implement position and estimate calculations
- Description: Compute waiting position by preceding WAITING sequence and estimate by `position - 1 × duration`.
- Requirements: REQ-004, REQ-005, REQ-017, REQ-023, REQ-044, REQ-045, REQ-046
- Dependencies: T-P1-03, T-P2-02
- Expected outputs: Snapshot position/estimate DTO logic.
- Tests/evidence: Unit tests for first/middle/last, calls/cancellations and missing/invalid duration handling.
- Completion: Position and estimate use only current waiting tokens and fixed queue duration; terminal/non-WAITING estimates are not falsely valid.

#### T-P2-08 — Implement lifecycle conflict and rollback guard
- Description: Centralize valid transitions and verify token/queue persisted state before mutation; prevent partial writes.
- Requirements: REQ-024, REQ-026, REQ-049, REQ-058
- Dependencies: T-P2-04, T-P2-05, T-P2-06
- Expected outputs: Domain exception hierarchy and transactional invariants.
- Tests/evidence: Unit/integration tests for every invalid transition matrix combination.
- Completion: All conflicting operations raise predictable errors and roll back; no partial lifecycle mutation occurs.

### Phase P3 — REST API

#### T-P3-01 — Implement queue REST endpoints
- Description: Add list/get/create/update/open-close endpoints and DTO validation.
- Requirements: REQ-011, REQ-020, REQ-024, REQ-037, REQ-050, REQ-051, REQ-052
- Dependencies: T-P2-01
- Expected outputs: Queue controllers and DTOs.
- Tests/evidence: MockMvc tests for success and invalid/atomic updates.
- Completion: API behavior matches contract and leaves state unchanged on validation/configuration conflict.

#### T-P3-02 — Implement join/token REST endpoints
- Description: Add join, token snapshot and cancel endpoints.
- Requirements: REQ-002, REQ-004, REQ-005, REQ-006, REQ-020, REQ-038, REQ-039, REQ-040
- Dependencies: T-P2-02, T-P2-03, T-P2-06, T-P2-07
- Expected outputs: Token controllers and join/cancel APIs.
- Tests/evidence: MockMvc/API tests for validation, closed/duplicate join and cancellation.
- Completion: Successful responses match persisted state and rejected operations are non-destructive.

#### T-P3-03 — Implement dashboard and lifecycle REST endpoints
- Description: Add dashboard, call-next, complete and skip endpoints.
- Requirements: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012, REQ-020, REQ-053, REQ-054
- Dependencies: T-P2-04, T-P2-05, T-P1-03
- Expected outputs: Dashboard/lifecycle controllers.
- Tests/evidence: MockMvc/API tests for counts, strict call and invalid transitions.
- Completion: Dashboard and lifecycle APIs expose only persisted current state and enforce call-next semantics.

#### T-P3-04 — Add global API error handling
- Description: Map validation/domain/conflict/not-found/data-integrity errors to the documented JSON envelope.
- Requirements: REQ-024, REQ-049, REQ-058
- Dependencies: T-P2-08, T-P3-01, T-P3-02, T-P3-03
- Expected outputs: Global exception handler and status mapping.
- Tests/evidence: API negative test matrix.
- Completion: No mutation leaks details; validation maps to 400, missing resources to 404, state conflicts to 409, and unexpected errors to generic 500; no 422 is used; all mappings are test-covered.

#### T-P3-05 — Add CORS/local-demo boundary
- Description: Configure local frontend origin and explicitly mark demo admin boundary.
- Requirements: REQ-001, REQ-057
- Dependencies: T-P3-01
- Expected outputs: CORS/demo configuration.
- Tests/evidence: Local API/browser smoke test and configuration check.
- Completion: Local frontend can call API; demo mode is clearly configured and production authentication is absent.

### Phase P4 — Customer frontend

#### T-P4-01 — Build responsive customer shell and queue list
- Description: Implement simple customer route, responsive layout and open queue selection.
- Requirements: REQ-001, REQ-002, REQ-021, REQ-037, REQ-056
- Dependencies: T-P3-01
- Expected outputs: Customer experience and queue directory.
- Tests/evidence: Component tests and browser viewport smoke tests for the latest two stable versions of Chrome, Edge, Firefox, and Safari, where available.
- Completion: Customer route works on the supported desktop/mobile browser baseline and shows independently managed queues. Any unavailable test browser is reported as blocked, not passed.

#### T-P4-02 — Build join form and token snapshot view
- Description: Validate name and optional phone, join, display identifier/status/position/estimate and API errors.
- Requirements: REQ-003, REQ-004, REQ-005, REQ-038, REQ-039, REQ-040
- Dependencies: T-P3-02, T-P4-01
- Expected outputs: Join form and token view.
- Tests/evidence: Component/API browser tests for success, closed, duplicate and invalid name.
- Completion: A successful join displays exactly one server-returned token and invalid join never shows a false success.

#### T-P4-03 — Implement customer polling and cancellation
- Description: Poll token snapshot, render status/position/estimate and allow WAITING cancellation.
- Requirements: REQ-006, REQ-023, REQ-055
- Dependencies: T-P4-02
- Expected outputs: Polling hook and cancel control.
- Tests/evidence: Component/browser tests for updates and terminal/called cancel rejection.
- Completion: Polling refreshes status and cancellation removes waiting state only after persisted success.

### Phase P5 — Administrator frontend

#### T-P5-01 — Build responsive admin shell and queue configuration
- Description: Implement admin route, queue create/update/open-close forms and display name/prefix/duration/status.
- Requirements: REQ-011, REQ-021, REQ-050, REQ-051, REQ-052, REQ-056, REQ-057
- Dependencies: T-P3-01, T-P3-05
- Expected outputs: Admin queue configuration UI.
- Tests/evidence: Component/browser tests for create/update/open-close and validation against the supported browser baseline where available.
- Completion: Admin UI reflects persisted config and rejected updates do not appear as successful.

#### T-P5-02 — Build dashboard polling and totals
- Description: Poll selected queue dashboard and render all five status counts and active/called tokens.
- Requirements: REQ-007, REQ-012, REQ-053, REQ-055
- Dependencies: T-P3-03, T-P5-01
- Expected outputs: Admin dashboard.
- Tests/evidence: Component/browser tests for mixed statuses and polling.
- Completion: Counts match the API and a terminal token is not included in active/waiting.

#### T-P5-03 — Implement call/complete/skip actions
- Description: Provide call-next and complete/skip for CALLED token only; no UI call-token selection.
- Requirements: REQ-008, REQ-009, REQ-010, REQ-054
- Dependencies: T-P3-03, T-P5-02
- Expected outputs: Admin action controls.
- Tests/evidence: Browser/component tests for success/conflict and no skip of later waiting token.
- Completion: UI offers only call-next and only valid CALLED terminal actions.

### Phase P6 — Verification

#### T-P6-01 — Complete unit and integration test coverage
- Description: Cover lifecycle matrix, FIFO, numbering reset, duplicate, position, estimate and persistence restart.
- Requirements: REQ-022, REQ-025, REQ-027, REQ-031
- Dependencies: P1–P5 domain tasks
- Expected outputs: JUnit/Mockito and Spring integration suites.
- Tests/evidence: Executed test reports.
- Completion: Every core rule has at least one positive and negative test and suite passes in an isolated environment.

#### T-P6-02 — Complete API and browser test coverage
- Description: Cover join/cancel/call/complete/skip/config/open-close/validation/conflict and both primary journeys.
- Requirements: REQ-027, REQ-056
- Dependencies: T-P3-04, T-P4-03, T-P5-03
- Expected outputs: API and essential E2E suites.
- Tests/evidence: Executed reports/screenshots or logs.
- Completion: Both primary journeys pass on the supported desktop/mobile browser baseline where available, with recorded browser/version/viewport; report unsupported test environments as blocked. No blocking layout defect remains. The basic usability check confirms primary actions and validation/errors are visible and usable in customer and administrator journeys.

#### T-P6-03 — Verify concurrency and data constraints
- Description: Execute concurrent requests within the single Spring Boot process against H2, including joins, duplicate joins, calls and existing CALLED state.
- Requirements: REQ-013, REQ-014, REQ-026, REQ-049
- Dependencies: T-P2-02, T-P2-04, T-P2-08
- Expected outputs: Concurrency test evidence and constraint report.
- Tests/evidence: JUnit integration/concurrency tests.
- Completion: Concurrent joins are unique and concurrent call-next creates no more than one CALLED per queue within the supported single-process deployment; multi-process operation is not claimed or tested.

#### T-P6-04 — Review scope exclusions and demo limitations
- Description: Verify no SMS, payment, physical-centre, tenant or production-auth implementation is added.
- Requirements: REQ-032–REQ-036
- Dependencies: T-P0-01
- Expected outputs: Scope audit.
- Tests/evidence: Diff/repository review.
- Completion: No excluded feature or external integration exists.

#### T-P6-05 — Independent review and final verification package
- Description: Produce test/build evidence and map every requirement to testable tasks; hold implementation review output until human approval.
- Requirements: REQ-001–REQ-058
- Dependencies: T-P6-01 through T-P6-04
- Expected outputs: Verification summary and requirement/task/test traceability.
- Tests/evidence: Final build/test reports and reviewed artifact revisions.
- Completion: No P0/P1 review finding remains unresolved or is documented as accepted; all 58 requirements are covered.

## 3. Dependency graph

```text
T-P0-01
  ├─> T-P0-02 ─> T-P1-01 ─> T-P1-02 ─> T-P1-03
  │                                  └─────> T-P2-04
  └─> T-P0-03 ─> T-P1-01

T-P1-01 ─> T-P2-01 ─> T-P3-01 ─┬─> T-P4-01 ─> T-P4-02 ─> T-P4-03
                               └─> T-P5-01 ─> T-P5-02 ─> T-P5-03

T-P1-02 ─> T-P2-02 ─┬─> T-P2-03
                    ├─> T-P2-06
                    └─> T-P2-07
T-P1-03 ────────────┴─> T-P2-04 ─> T-P2-05 ─> T-P2-08
T-P2-06 ───────────────────────────┘

T-P2-08 ─┐
T-P3-01 ─┤
T-P3-02 ─┼─> T-P3-04 ─> T-P4/P5 UI ─> T-P6-01/02/03/04/05
T-P3-03 ─┘
```

## 4. Requirement-to-task coverage

| Requirements | Architecture components | Implementation tasks |
|---|---|---|
| REQ-001 | AppShell/customer/admin routes | T-P3-05, T-P4-01, T-P5-01 |
| REQ-002 | Join form, Join service | T-P2-02, T-P3-02, T-P4-02 |
| REQ-003 | Token allocation | T-P1-02, T-P2-02, T-P4-02 |
| REQ-004 | Snapshot position | T-P2-07, T-P3-02, T-P4-02 |
| REQ-005 | Estimate calculator | T-P2-07, T-P3-02, T-P4-02 |
| REQ-006 | Cancellation domain/service/API/UI | T-P2-06, T-P3-02, T-P4-03 |
| REQ-007 | Active token dashboard query | T-P1-03, T-P3-03, T-P5-02 |
| REQ-008 | Call-next | T-P2-04, T-P3-03, T-P5-03 |
| REQ-009 | Complete transition | T-P2-05, T-P3-03, T-P5-03 |
| REQ-010 | Skip transition | T-P2-05, T-P3-03, T-P5-03 |
| REQ-011 | Queue CRUD/open-close | T-P1-01, T-P2-01, T-P3-01, T-P5-01 |
| REQ-012 | Dashboard totals | T-P1-03, T-P3-03, T-P5-02 |
| REQ-013 | FIFO service/query | T-P2-04, T-P6-03 |
| REQ-014 | Queue lock/CALLED check | T-P2-04, T-P6-03 |
| REQ-015 | Per-Queue numbering reset | T-P2-02, T-P6-01 |
| REQ-016 | Queue closed check | T-P2-02, T-P3-02 |
| REQ-017 | Waiting-only queries | T-P1-03, T-P2-06, T-P2-07 |
| REQ-018 | Lifecycle enum/transitions | T-P0-03, T-P1-02, T-P2-05, T-P2-06 |
| REQ-019 | H2/JPA persistence | T-P0-02, T-P1-01, T-P1-02 |
| REQ-020 | REST controllers | T-P3-01, T-P3-02, T-P3-03 |
| REQ-021 | Simple responsive UI | T-P4-01, T-P5-01 |
| REQ-022 | Sequence uniqueness and allocation | T-P1-02, T-P2-02, T-P6-01 |
| REQ-023 | Persisted recalculation/snapshot | T-P2-07, T-P4-03 |
| REQ-024 | Validation/error handler | T-P0-03, T-P3-04 |
| REQ-025 | Modular/testable implementation | T-P0-01, T-P6-01 |
| REQ-026 | Lock/version/state checks | T-P2-06, T-P2-08, T-P6-03 |
| REQ-027 | Test suites | T-P0-01, T-P6-01, T-P6-02 |
| REQ-028 | React/Vite frontend | T-P0-01 |
| REQ-029 | Java/Spring backend | T-P0-01 |
| REQ-030 | H2 persistence | T-P0-02 |
| REQ-031 | JUnit/Mockito/browser testing | T-P0-01, T-P6-01, T-P6-02 |
| REQ-032 | No production auth | T-P3-05, T-P6-04 |
| REQ-033 | Single centre/no centre entity | T-P0-02, T-P6-04 |
| REQ-034 | No tenant model | T-P0-02, T-P6-04 |
| REQ-035 | No payment component | T-P6-04 |
| REQ-036 | No SMS component | T-P6-04 |
| REQ-037 | Multiple Queue records/list | T-P1-01, T-P4-01 |
| REQ-038 | Join name validation | T-P1-02, T-P2-02, T-P4-02 |
| REQ-039 | Join optional phone | T-P1-02, T-P2-02, T-P4-02 |
| REQ-040 | Duplicate join predicate | T-P2-02, T-P2-03, T-P4-02 |
| REQ-041 | Per-Queue numbering fields | T-P1-01, T-P2-02 |
| REQ-042 | Queue prefix | T-P1-01, T-P2-02 |
| REQ-043 | Display identifier snapshot | T-P1-02, T-P2-02 |
| REQ-044 | Queue duration/estimate | T-P1-01, T-P2-07 |
| REQ-045 | Direct preceding×duration formula | T-P2-07 |
| REQ-046 | Exclude own waiting position | T-P2-07 |
| REQ-047 | SKIPPED terminal | T-P2-05, T-P2-08 |
| REQ-048 | Rejoin new token | T-P2-05, T-P2-02 |
| REQ-049 | Conflict exceptions/no mutation | T-P2-08, T-P3-04, T-P6-03 |
| REQ-050 | Create/configure name/prefix | T-P1-01, T-P2-01, T-P3-01, T-P5-01 |
| REQ-051 | Configure fixed duration | T-P1-01, T-P2-01, T-P5-01 |
| REQ-052 | Open/close | T-P1-01, T-P2-01, T-P3-01, T-P5-01 |
| REQ-053 | Five dashboard totals | T-P1-03, T-P3-03, T-P5-02 |
| REQ-054 | Call-next without token selection | T-P2-04, T-P3-03, T-P5-03 |
| REQ-055 | Polling | T-P4-03, T-P5-02 |
| REQ-056 | Latest two stable Chrome/Edge/Firefox/Safari, desktop/mobile layouts | T-P4-01, T-P5-01, T-P6-02 |
| REQ-057 | Local-demo admin | T-P0-03, T-P3-05, T-P5-01 |
| REQ-058 | Validation and atomic API transitions | T-P0-03, T-P2-08, T-P3-04 |

## 5. Testing requirements and evidence

At completion, evidence must include:
- backend unit test report,
- backend integration/API test report,
- frontend component test report,
- essential desktop/mobile browser E2E report,
- build logs,
- concurrency test log,
- schema/data constraint review,
- scope review for exclusions,
- requirement/test traceability summary.

No task is complete without its listed evidence and measurable criteria.

## 6. Blocked tasks and unresolved decisions

No task is blocked by Jira publication. Jira work-item publication is deferred and must not occur in this stage.

Architecture plan assumptions that remain implementation details under documented MVP assumptions:
- polling interval: implementation selected, not threshold (ASSUMP-002);
- exact duplicate name/phone matching as documented (ASSUMP-003);
- prefix uniqueness (ASSUMP-004);
- estimate display rounding (ASSUMP-005);
- local timezone configuration (ASSUMP-001).

Owner decisions should not be inferred beyond the approved backlog. Any owner clarification that changes requirements invalidates this architecture/plan and restarts affected work.
