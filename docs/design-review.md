# QueueLess Architecture and Implementation Plan — Independent Design Review

**Review date:** 2026-11-24  
**Status:** Independent review complete; DR-002–DR-006 resolved in approved revisions
**Scope:** `docs/architecture.md` and `docs/impl-plan.md` only  
**Requirements baseline:** `docs/requirements/backlog-proposal.md`, REQ-001–REQ-058  
**Workflow evidence:** `docs/workflow/state.md`, `.github/copilot-instructions.md`, `.github/skills/architecture-design/SKILL.md`

## 1. Review scope and revisions

This review independently evaluated the proposed QueueLess architecture and implementation plan against the finalized local requirements backlog and workflow standards. It did not inspect or change application code, Jira, or workflow state.

The reviewed revisions are:

- `docs/architecture.md`: **Proposed architecture for independent review and human approval**
- `docs/impl-plan.md`: **Proposed plan for independent review and human approval**
- `docs/requirements/backlog-proposal.md`: finalized local backlog with 58 stable requirement IDs
- `docs/workflow/state.md`: Architecture and design review in progress; implementation not started

Key checks performed:

- Alignment of the modular monolith, REST API, persistence, UI, polling, and local-demo boundaries with REQ-001–REQ-058.
- Traceability from each requirement into architecture components and implementation tasks.
- Queue numbering, FIFO, lifecycle, duplicate-join, position, estimate, conflict, persistence, and concurrency design.
- Technology trade-offs and scope exclusions.
- Implementation phase dependencies, deliverables, tests, and completion criteria.
- Compliance with human-approval and invalidation rules in the workflow standards.

No test execution evidence or application implementation was available for this design-stage review.

## 2. Executive recommendation

**Initial recommendation:** Ready for human approval, subject to the findings below. The focused re-review and explicit owner approval recorded later in this document supersede this initial recommendation.

There are **no blocking or high-severity unresolved architecture concerns**. The proposed modular monolith is proportionate to the one-centre MVP, and the plan traces all 58 requirements. The remaining findings are medium/low documentation and verification-coverage gaps, not reason to reject the architecture.

Human approval should explicitly approve:

1. the proposed architecture revision in `docs/architecture.md`;
2. the proposed implementation-plan revision in `docs/impl-plan.md`; and
3. the listed implementation assumptions and test scope recorded in these documents.

Before implementation starts, the approval record should confirm that the browser support matrix, P0/P1 verification exits, and exact API status conventions are acceptable as implementation commitments.

## 3. Findings

### Finding DR-001 — High-severity criteria are not present

**Severity:** Not applicable / no finding  
**Evidence:** Architecture sections 1–16 and implementation-plan phases P0–P6.  
**Rationale:** The review identified no unresolved critical or high-severity architectural concern that would violate a requirement or make the design unsafe to proceed for the explicitly scoped local MVP.

This conclusion does not constitute implementation approval or evidence that tests will pass.

### DR-002 — Browser support verification matrix is not specified

**Severity:** Medium  
**Requirement references:** REQ-056; US-016.  
**Evidence:**

- `docs/architecture.md`, section 13, uses “essential Playwright/Cypress or similarly appropriate browser tests” and responsive UI is mapped to REQ-056.
- `docs/impl-plan.md`, T-P6-02, requires “desktop and at least one supported mobile viewport,” but defines no supported desktop/mobile browser names or versions.
- T-P4-01 and T-P5-01 require browser viewport smoke tests but do not define the viewport matrix.

**Rationale:** The requirement permits modern desktop and mobile browsers, but there is no stable definition of the supported test environment. One mobile viewport can demonstrate layout behavior, but does not establish a browser/viewport baseline for REQ-056 or “modern desktop/mobile browser test environment” in US-016.

**Recommendation:** Before approval or implementation completion, define at least one explicit desktop browser and mobile browser or responsive viewport matrix, or obtain a human decision that “modern browser latest versions” plus one mobile viewport is sufficient. Map that matrix into T-P6-02.

### DR-003 — One-CALLED invariant depends on a portable queue lock and the H2 single-instance constraint

**Severity:** Medium  
**Requirement references:** REQ-014, REQ-026, REQ-049.  
**Evidence:**

- `docs/architecture.md`, AD-05 and section 9, use the Queue pessimistic lock and re-read persisted status. A partial H2 index is described only as desirable when supported.
- Section 14 limits deployment to one application instance because scaling to multiple instances could invalidate locking assumptions.
- T-P6-03 requires concurrent H2 join/call tests but does not separately require a database-constraint fallback for the one-CALLED invariant.

**Rationale:** The design is internally consistent for a single-process local demo: the queue-row lock prevents concurrent calls within that process and joins are serialized per queue. However, it remains important that the implementation not claim the invariant is enforced by JPA or H2 if the lock is bypassed or a second process starts. A database constraint or explicit out-of-process restriction would make this stronger.

**Recommendation:** Keep the MVP to one backend process, as explicitly documented, and ensure T-P6-03 verifies the lock behavior. If a persistent database-level one-CALLED constraint is feasible in the selected H2/JPA version, use it as defense in depth; otherwise document in completion evidence that it is not a supported constraint and retain the single-process deployment limitation.

### DR-004 — Dependency graph omits T-P2-08 and conflict/rollback sequencing

**Severity:** Medium  
**Requirement references:** REQ-024, REQ-026, REQ-049, REQ-058.  
**Evidence:**

- T-P2-08 is defined as the centralized lifecycle conflict and rollback guard.
- Its dependencies are T-P2-04, T-P2-05, and T-P2-06.
- T-P3-04 defines global API error handling and depends only on T-P3-01 through T-P3-03 in the stable task list.
- The dependency graph in `docs/impl-plan.md`, section 3, shows P2/P3 activity but does not include T-P2-08 or T-P2-08’s relationship to P3.

**Rationale:** The task descriptions themselves indicate T-P2-08 should provide the domain exception hierarchy consumed by API error handling. The omission could allow the implementer to schedule T-P3-04 before the domain exception model is complete or produce an inconsistent status mapping.

**Recommendation:** Make T-P3-04 explicitly depend on T-P2-08, and include T-P2-08 in the dependency graph. This is a plan correction, but it does not require changing architecture.

### DR-005 — API status mapping retains an optional 422 alternative

**Severity:** Low  
**Requirement references:** REQ-024, REQ-049, REQ-058.  
**Evidence:** `docs/architecture.md`, section 6, documents 400/404/409/500/optional 422 and states that either 422 or 400/409 may be chosen as long as mappings are consistent.

**Rationale:** The requirement requires clear domain errors but not HTTP 422 specifically. Allowing two mappings is acceptable only if the implementation chooses and documents one stable mapping. It is an implementation detail, not a requirement ambiguity.

**Recommendation:** At implementation start, choose 422 or 400/409 and remove or document the alternative as non-applicable. T-P3-04’s negative test matrix should assert the final mapping.

### DR-006 — Measurable usability criteria for REQ-021 are limited

**Severity:** Low  
**Requirement references:** REQ-021, REQ-056.  
**Evidence:**

- T-P4-01 and T-P5-01 require responsive layouts and component/browser viewport smoke tests.
- T-P6-02 requires desktop/mobile browser journeys.
- No explicit acceptance criterion defines user-friendly behavior beyond successful journeys and validation visibility.

**Rationale:** The backlog only defines REQ-021 as simple and user-friendly, with no formal accessibility or usability threshold. The plan reasonably addresses layout and journey usability but has no measurable usability exit metric.

**Recommendation:** Keep this as a human/implementation limitation, or add a concise approval criterion such as primary actions and validation visible in the reviewed desktop/mobile journeys and no blocking layout defect. Do not invent WCAG or accessibility requirements not present in the backlog.

## 4. Requirement coverage gaps

No requirement REQ-001–REQ-058 is completely untraceable.

- Architecture section 16 traces all 58 stable IDs to one or more components.
- The implementation-plan requirement/task table likewise has one or more task mappings for all 58 IDs.
- P0–P6 include test or completion evidence for the core functional and non-functional areas.

The following are coverage refinements rather than missing requirement mappings:

- REQ-021 lacks a measurable usability acceptance threshold; this is covered by implementation tasks and browser journeys but not an explicit usability metric.
- REQ-056 needs an explicit browser/viewport matrix rather than “at least one supported mobile viewport.”
- REQ-014/REQ-026 has concurrency verification, but only in a documented single-process H2 deployment.
- REQ-024/REQ-049/REQ-058 would benefit from T-P3-04 explicitly consuming T-P2-08, as identified in DR-004.

## 5. Risks and inconsistencies

| Risk | Impact | Mitigation in the reviewed artifacts |
|---|---|---|
| H2 file persistence, demo-scale only | No HA or horizontal scaling; no backup guarantee | Single-process deployment, file profile, isolated test profile, restart tests |
| Pessimistic Queue locking and one-CALLED/FIFO concurrency | A second backend instance may violate lock assumptions | MVP deploys one process; T-P6-03 verifies concurrent joins/calls |
| Polling update lag | Customer/admin status may update after a short delay | Polling GET snapshots, implementation-selected interval, no push dependency |
| Duplicate identity rule ambiguity | Customer name/phone matching may vary unless normalized consistently | Architecture documents trim/exact matching and optional-phone matrix tests |
| Prefix uniqueness assumption | Duplicate prefixes remain possible only if constraint or validation is omitted | Unique DB constraint and application validation planned |
| Local clock/time-zone changes | Midnights and daily numbering may be inconsistent | Injected Clock and explicit local zone configuration |
| Local-demo access | Anyone with network access could invoke demo admin operations | Bind locally or to a controlled network, label demo-only, no real personal data |
| Requirement traceability can drift | Task or test omissions may not be visible until late verification | Stable requirement IDs, task coverage table, T-P6-05 final traceability and review |

No critical, blocking, or high-severity unresolved concern was found.

## 6. Approval readiness

The architecture and plan are ready to be presented to the human approver, provided the findings are accepted as documented limitations or corrected in place as part of the approved revision.

**Initial review findings:** No blocking/high findings; 3 medium and 2 low findings, all resolved in the focused re-review addendum below.  

The approval recommendation is to approve the architecture and plan for implementation only after the approver acknowledges:

- the single-process H2/local-demo deployment boundary;
- the browser support matrix or accepted implementation detail;
- the final API status mapping;
- the explicit T-P2-08 → T-P3-04 dependency correction.

Changes to approved requirements or architecture invalidate affected implementation-planning work. If the browser baseline or deployment model is treated as an approved requirement decision rather than an implementation detail, it must be clarified through requirements/architecture review rather than silently inferred.

## 7. Focused re-review addendum — DR-002 through DR-006

**Review date:** 2026-11-24  
**Scope:** Updated `docs/architecture.md` and `docs/impl-plan.md`, checked against the requested owner decisions and prior findings DR-002–DR-006. No Jira access was used. No application code, workflow state, architecture, or implementation-plan content was changed.

### Outcomes by prior finding

| Finding | Outcome | Re-review evidence |
|---|---|---|
| **DR-002 — Browser support baseline** | **Resolved in the reviewed documents.** | Architecture sections 2 and 13 define support as the latest two stable versions of Chrome, Edge, Firefox, and Safari. Implementation tasks T-P4-01, T-P5-01, and T-P6-02 carry the browser baseline into verification, require recording browser/version/viewport, and say unavailable environments must be reported as blocked rather than passed. |
| **DR-003 — Single-process concurrency boundary** | **Resolved in the design and test plan; execution evidence remains pending implementation.** | Architecture sections 2, 9, and 14 constrain the MVP to exactly one Spring Boot backend process and specify transaction-scoped H2 queue-row locking plus persisted-state checks (not a process-local Java mutex). T-P6-03 requires concurrent join, duplicate-join, and call testing within that process and explicitly excludes claims about multi-process operation. No test has yet run, consistent with workflow state. |
| **DR-004 — T-P2-08 dependency and graph** | **Resolved.** | T-P3-04 explicitly depends on T-P2-08. The section 3 dependency graph includes T-P2-08 among the prerequisites feeding T-P3-04; T-P2-08 is also shown downstream of the lifecycle transition tasks and the graph includes the T-P2-06 path. |
| **DR-005 — HTTP status mapping** | **Resolved.** | Architecture section 6 fixes validation/request errors at 400, missing resources at 404, conflicts at 409, and unexpected errors at generic 500, and explicitly excludes 422. T-P3-04 repeats these mappings and requires negative-test coverage, with no 422. |
| **DR-006 — Basic usability check** | **Resolved without product-scope expansion.** | Architecture section 13 and T-P6-02 require basic verification of the customer join/track/cancel journey and administrator configure/call/complete/skip journey, checking that primary actions and validation/errors are visible and usable in desktop/mobile layouts with no blocking layout defects. The text defines verification only and does not add product features or invent accessibility standards. |

### Traceability and new findings

Requirement traceability remains complete for **REQ-001–REQ-058**: architecture section 16 has an entry for each requirement, and implementation-plan section 4 maps each requirement to architecture components and implementation tasks. The re-review found **no new design or planning findings**.

### Readiness recommendation

**Technical review recommendation: Ready to proceed to implementation after explicit human approval of the current architecture and implementation-plan revisions. No technical blocker remains from DR-002 through DR-006.** Concurrency and browser verification are planned evidence, not completed tests; they must be executed and reported during implementation/verification, and unavailable browser environments must remain marked blocked rather than passed.

This addendum records the independent re-review only. The product owner subsequently approved the current architecture and implementation plan and supplied the decisions for DR-002–DR-006. The Orchestrator records that approval and the resolved finding status in `docs/workflow/state.md`. Browser and concurrency checks remain planned evidence and must be executed during implementation/verification; unavailable browser environments must remain marked blocked rather than passed.

## 8. Current independent implementation review

**Review date:** 2026-11-24  
**Scope:** Current QueueLess backend and frontend source, architecture, approved requirements, implementation plan, automated test source, and available test/verification evidence. This review does not modify application code, tests, manifests, dependencies, requirements, architecture, plan, workflow state, Jira, GitHub, or PR artifacts.  
**Baseline:** `docs/requirements/backlog-proposal.md` REQ-001–REQ-058; `docs/architecture.md`; `docs/impl-plan.md`; `docs/workflow/state.md`. The previous architecture findings DR-002–DR-006 remain resolved in the approved design documents; the implementation evidence below is assessed separately.

### Review method and evidence

- Read the current backend Java sources, application/demo/test properties, Maven manifest, backend test sources, frontend source/styles, frontend test source, and frontend manifest/Vite configuration.
- Compared implementation behavior and test coverage to the approved requirement and plan traceability, especially queue lifecycle, FIFO, numbering, duplicates, estimates, error handling, local-demo boundary, and browser support.
- Reviewed `docs/test-results.md`, `docs/verification-report.md`, workflow-state test entries and latest implementation log, and the available Maven Surefire summaries. No test, build, browser, or coverage command was independently executed for this review.
- No Jira access was used.

At the time of the independent review, recorded backend evidence was `mvn.cmd verify`: 13 tests passed (11 API integration, one restart, one service test); the three available Surefire summaries agreed with those counts. After that review, the demo-boundary correction was added and the recorded Maven result became 14 tests passed, including one focused boundary test. This follow-on test result verifies the correction but is not an independent re-review. The API suite includes lifecycle, duplicate, position/estimate, dashboard, lazy numbering reset, and single-process concurrency cases. The restart test exercises two Spring contexts against the same temporary H2 file.

The test/build count mismatch identified in this review is reconciled in `docs/test-results.md` and `docs/verification-report.md`: the later recorded frontend result is 10 tests and the backend result after the demo-boundary test is 14 tests. These are consolidated execution records, not new executions. Existing workflow evidence records a customer/admin browser spot-check at desktop and mobile viewport sizes, endpoint HTTP 200 responses, and no runtime errors after reload; this is limited manual smoke evidence and does not substitute for the required browser/version/viewport E2E matrix.

### Findings

#### CR-001 — Unauthenticated demo administrator endpoints are not restricted to local access by default

**Severity:** High  
**Status:** Resolved; verification recorded. Independent post-fix re-review not performed.

**Requirements/architecture:** REQ-057; `docs/architecture.md` §11–12; `docs/impl-plan.md` T-P3-05.  
**Original evidence and impact:** `backend/src/main/resources/application.properties:1-2` activates the `demo` profile by default. `application-demo.properties` enabled demo admin operations but did not restrict the server address. The API exposes queue creation/configuration/status and token call/complete/skip operations without authentication, so the listener could be reachable from other interfaces.

**Resolution and verification:** Added `server.address=127.0.0.1` to `backend/src/main/resources/application-demo.properties`. The default demo-profile listener is now restricted to IPv4 loopback; no authentication framework or dependency was added. `backend/src/test/java/com/queueless/DemoAdminBoundaryTest.java` starts the demo profile on an isolated in-memory database, verifies the active bind address resolves to loopback, and exercises local queue creation, joining, call-next, and completion. `mvn.cmd verify` passed after this change: 14 tests, 0 failures, 0 errors, 0 skipped, including the new demo-boundary integration test.

#### CR-002 — Mandatory browser E2E and viewport verification remains blocked

**Severity:** High  
**Requirements/plan:** REQ-027, REQ-056; `docs/impl-plan.md` T-P4-01, T-P5-01, T-P6-02.  
**Evidence and impact:** `frontend/src/App.test.jsx:7` mocks the API client; its 10 cases are component tests, not browser E2E. Existing workflow records describe a limited customer/admin desktop/mobile spot-check, but there is no executed latest-two-version Chrome/Edge/Firefox/Safari matrix, browser automation source, screenshots, or browser/version/viewport results. The recorded manual smoke checks do not establish the required customer and administrator journeys and responsive/browser baseline.

**Recommendation:** Execute and record the planned customer join/track/cancel and administrator configure/call/complete/skip journeys at desktop and mobile viewports against the supported browser baseline, including browser versions and results; report unavailable engines as blocked. Alternatively, obtain an explicit human acceptance of the limitation before final verification approval. Component tests or a single informal spot-check must not be represented as completion of T-P6-02.

#### CR-003 — Queue-prefix uniqueness is not atomic for case-insensitive duplicates

**Severity:** Medium  
**Requirements/architecture:** REQ-026, REQ-042, REQ-050; `docs/architecture.md` §5; `docs/impl-plan.md` T-P1-01.  
**Evidence and impact:** `backend/src/main/java/com/queueless/api/QueueService.java:23-27,30-33` checks prefixes using `existsByPrefixIgnoreCase` but the check and insert/update are not protected by a global uniqueness lock. `QueueRepository.java:8-9` confirms case-insensitive prechecks, while `QueueEntity.java:7` defines a database unique constraint on the raw `prefix` column. A concurrent create/update using case variants can pass both application checks, and the raw case-sensitive database constraint can accept both. Tokens can then have ambiguous display identifiers (for example `A-1` and `a-1`), despite the application's case-insensitive uniqueness check.

**Recommendation:** Enforce one canonical prefix representation in persisted data and constrain that canonical value uniquely, or use a database-supported case-insensitive unique constraint with matching validation. Add sequential and concurrent case-variant tests proving only one queue can own the logical prefix.

#### CR-004 — Required direct persistence-constraint tests are missing

**Severity:** Medium  
**Requirements/plan:** REQ-019, REQ-022; `docs/impl-plan.md` T-P1-01 and T-P1-02.  
**Evidence and impact:** `QueueEntity.java:7` and `TokenEntity.java:6-15` declare unique constraints and a required queue association. The plan explicitly calls for JPA integration/schema-constraint tests and says duplicate sequence values and invalid associations must be rejected. The reviewed test sources exercise service-level uniqueness/concurrency and persistence restart, but do not attempt direct duplicate `(queue_id, numbering_day, sequence_number)` inserts, invalid token associations, or database rejection of duplicate prefixes. Current reports likewise do not provide schema-constraint test evidence. The declared constraints therefore lack the planned direct regression evidence.

**Recommendation:** Add isolated persistence integration tests that bypass the service checks and flush violating rows, asserting the database rejects each required invalid state. Include the case-insensitive-prefix behavior from CR-003 in those tests.

#### CR-005 — Requirement-to-test traceability is incomplete

**Severity:** Medium  
**Status:** Partially resolved: test counts are reconciled in the consolidated reports; complete requirement mapping remains open.
**Requirements/plan:** REQ-025, REQ-027; `docs/impl-plan.md` T-P6-05.  
**Evidence and impact:** `docs/workflow/state.md` still records requirement-by-requirement mapping as partial. The consolidated test-results and verification reports now consistently record the later 10-test frontend result and 14-test backend result, but there is no verified REQ-001–REQ-058 mapping to implementation and passing test evidence. It remains difficult to identify coverage gaps or confirm positive/negative evidence for every requirement.

**Recommendation:** Complete the REQ-001–REQ-058 implementation/test/evidence matrix, identifying uncovered or indirectly tested requirements. Preserve command, result, and report provenance; do not claim independent execution unless it occurred.

#### CR-006 — Customer token polling continues after terminal states

**Severity:** Low  
**Requirements/architecture:** REQ-023, REQ-055; `docs/architecture.md` §10; `docs/impl-plan.md` T-P4-03.  
**Evidence and impact:** `frontend/src/main.jsx:99-103` starts a 3-second interval keyed only by `token?.tokenId`; it is cleared only when the component unmounts or the token ID changes. A token response with `COMPLETED`, `SKIPPED`, or `CANCELLED` does not stop the interval, so the customer page continues issuing token GET requests indefinitely for a terminal token. The frontend cancellation test verifies that cancellation is no longer offered, but does not check polling shutdown.

**Recommendation:** Stop or otherwise bound polling once the token reaches a terminal state, while continuing polling for `CALLED` if the customer must learn its eventual outcome. Add a fake-timer test proving terminal status clears/stops polling and that called-to-terminal updates remain observable.

#### CR-007 — The generic HTTP 500 mapping has no direct regression test

**Severity:** Low  
**Requirements/plan:** REQ-024, REQ-049, REQ-058; `docs/impl-plan.md` T-P3-04.  
**Evidence and impact:** `backend/src/main/java/com/queueless/api/ExceptionHandler.java:35-37` maps unexpected exceptions to a generic 500 envelope. `ApiIntegrationTest.java:44-48` covers 400 validation and 404 not-found, and other tests cover domain conflicts; no test deliberately triggers an unexpected exception and asserts status 500, generic error code/message, and absence of exception details. T-P3-04 requires all mappings to be test-covered.

**Recommendation:** Add an API-level test using an injected failing service/controller path and assert the generic 500 envelope does not disclose exception, SQL, or implementation details.

#### CR-008 — Previously reported dependency audit findings remain untriaged

**Severity:** Medium  
**Requirements/plan:** REQ-025; dependency safety review.  
**Evidence and impact:** `docs/workflow/state.md:27` records six npm audit findings (three moderate, one high, two critical) with no dependency version changes or remediation. The reviewed build/test records establish functionality, not whether the affected packages are production/runtime dependencies, exploitable in this deployment, or development-only. No advisory/package-level audit output was included in the reviewed evidence, so this review does not claim that a particular vulnerability is exploitable or that remediation is authorized.

**Recommendation:** Preserve the existing dependencies pending approval, but attach the audit details and triage package, dependency path, affected version, and runtime reachability. Obtain an authorized decision on remediation or documented acceptance before calling dependency safety resolved.

### 9. Architecture, quality, and security assessment

The approved modular-monolith, REST, file-backed H2, per-queue locking, polling, and local-demo technology choices remain proportionate to the one-service-centre MVP. The reviewed business logic and integration tests provide meaningful evidence for queue lifecycle, strict FIFO, per-queue daily numbering, duplicate joins, waiting-only positions/estimates, dashboard totals, persistence restart, and single-process concurrent join/call behavior. No additional confirmed defect in these business rules was found from the reviewed source.

The admin network-boundary omission reported in CR-001 has been addressed with loopback-only binding for the default demo profile and a passing focused integration test. The API validation/error-envelope implementation is centralized and avoids returning exception details in the generic 500 path. The current tests do not, however, cover every planned error mapping or persistence constraint. Frontend component tests and recorded manual smoke checks provide useful evidence, but do not validate the full browser matrix or all browser-specific journeys.

**403/configuration question:** No authentication/authorization rule or security filter in the reviewed source explains a 403 for a valid queue-create request. With the default UI origin `http://localhost:5173`, the Vite `/api` proxy, and a valid create payload, the controller returns HTTP 201 if the backend receives and accepts the request (`backend/src/main/java/com/queueless/api/Controllers.java:15-16`). The configured CORS policy (`backend/src/main/java/com/queueless/config/CorsConfig.java:8-10`) permits only the configured origin; a browser request from another origin can be rejected by CORS and may receive a 403, which is distinct from authorization or application validation. The recorded invalid payload correctly returns 400. A valid live create was not submitted in the recorded spot-check, and this review did not execute a request, so an environment/proxy/browser-specific failure cannot be ruled out; it is not reproducible from the valid same-origin source path alone.

### 10. Readiness conclusion

**Not ready to mark final verification approved or create a PR.** No Critical finding was confirmed. CR-001 is resolved with recorded automated evidence, but has not received an independent post-fix re-review. CR-002 remains open because mandatory browser E2E/viewport verification is not evidenced. CR-003, CR-004, CR-006, CR-007, and CR-008 remain open; CR-005 is partially resolved because the result counts are now aligned but full requirement-to-test traceability remains incomplete. No explicit user acceptance of these open findings or the browser limitation is recorded. Numeric coverage remains unmeasured and has no approved numeric threshold.

The independent review was static and no tests/builds were rerun as part of it. Later recorded test results, including the demo-boundary change, are consolidated in `docs/test-results.md` and `docs/verification-report.md`; this report does not claim independent re-execution or re-review of that change.
