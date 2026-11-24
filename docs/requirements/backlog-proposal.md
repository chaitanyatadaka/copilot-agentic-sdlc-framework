# QueueLess Revised Requirements Backlog

> **Status:** Local proposal only. This document does not publish or modify Jira work items.
>
> **Baseline and source:** Existing backlog revised using Jira issue `CSF-1` — “QueueLess – Project Requirements and Scope”.
>
> **Jira source revision:** Issue created `2026-11-24T13:36:17.909+0530`; updated `2026-11-24T13:36:18.770+0530`.

## 1. Project Overview

QueueLess is a web-based virtual queue management system for one service centre with one or more independently managed queues. Customers join queues remotely, receive digital tokens, and track their queue position and estimated wait. Administrators create and configure queues, open and close them, call customers in FIFO order, and record terminal outcomes.

### Problem statement

Physical queues create congestion, wasted time, and poor customer experience. QueueLess provides digital joining and queue monitoring without physical waiting.

### Objectives

- Allow guests to join available queues remotely.
- Generate unique, queue-specific, daily tokens.
- Display an accurate waiting position and estimate.
- Allow administrators to operate and monitor queues.
- Preserve persisted state and test all core behaviours.

## 2. Decision Classification

### 2.1 Owner-confirmed decisions

These decisions come from the product owner's confirmation.

- A service centre may contain multiple queues.
- Guest joining requires a customer name and accepts an optional phone number.
- Each queue resets its token number independently at midnight.
- Queue position counts only `WAITING` tokens.
- Each queue has a configurable, fixed average service duration used for waiting estimates.
- Administrators manually skip a nonresponder after that token is `CALLED`.
- Customers may cancel only `WAITING` tokens.
- `SKIPPED` is terminal; a skipped customer must rejoin to receive a new token.
- Each queue allows only one `CALLED` token.
- Administrator calling and queue processing are strict FIFO.

### 2.2 User-provided MVP defaults

These defaults were supplied for backlog preparation and are not architecture approvals.

- The administrator dashboard shows counts for `WAITING`, `CALLED`, `COMPLETED`, `SKIPPED`, and `CANCELLED`.
- Administrators can create/configure queues and open/close them.
- The MVP uses polling rather than push-based real-time updates.
- The MVP supports modern desktop and mobile browsers.
- Tokens use a queue-specific prefix plus a daily sequential number.
- Duplicate joins and conflicting operations must be prevented.
- Production-grade authentication is not required; local-demo administrator access is sufficient.
- The test suite includes unit, integration, API, and essential end-to-end tests.

### 2.3 Simple MVP assumptions

- Midnight uses the application's configured local clock/calendar day for reset and historical grouping.
- Polling may use short intervals selected during implementation; the functional requirement is eventual display refresh through polling.
- A duplicate join means the same guest name and optional phone for the same queue while that guest has an active `WAITING` token.
- The queue prefix and token display formatting are set by administrator configuration.

## 3. Stable Requirements Register

### 3.1 Core confirmed and baseline functional requirements

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-001 | Provide separate customer and administrator experiences. | Baseline from CSF-1 | CSF-1 §4, §10 |
| REQ-002 | Allow a guest to join an open queue. | Confirmed baseline | CSF-1 §3–§5 |
| REQ-003 | Generate a unique digital token for each successful join. | Confirmed baseline | CSF-1 §3 |
| REQ-004 | Display a customer's current queue position. | Confirmed baseline | CSF-1 §3–§4 |
| REQ-005 | Display an estimated waiting time to the customer. | Confirmed baseline | CSF-1 §3–§4 |
| REQ-006 | Allow cancellation only for a `WAITING` token. | Owner-confirmed | CSF-1 §4, §6; owner decision |
| REQ-007 | Allow an administrator to view active queue tokens. | Confirmed baseline | CSF-1 §4 |
| REQ-008 | Allow an administrator to call the next customer. | Confirmed baseline | CSF-1 §4 |
| REQ-009 | Allow an administrator to mark a `CALLED` token completed. | Confirmed baseline | CSF-1 §4 |
| REQ-010 | Allow an administrator to skip a nonresponding token after it is `CALLED`. | Owner-confirmed | CSF-1 §4, §6; owner decision |
| REQ-011 | Allow an administrator to create, configure, open, and close queues. | MVP default | CSF-1 §4; MVP default |
| REQ-012 | Allow an administrator to view queue status and status totals. | Confirmed baseline; expanded by MVP default | CSF-1 §4 |
| REQ-013 | Process queues in strict FIFO order. | Owner-confirmed | CSF-1 §5 |
| REQ-014 | Permit only one `CALLED` token per queue. | Owner-confirmed | CSF-1 §5 |
| REQ-015 | Reset token numbering independently for each queue at midnight. | Owner-confirmed | CSF-1 §5; owner decision |
| REQ-016 | Reject joins to a closed queue. | Confirmed baseline | CSF-1 §5 |
| REQ-017 | Count only `WAITING` tokens in queue position and waiting totals. | Owner-confirmed | CSF-1 §5; owner decision |
| REQ-018 | Use `WAITING`, `CALLED`, `COMPLETED`, `SKIPPED`, and `CANCELLED` lifecycle states. | Confirmed baseline | CSF-1 §6 |
| REQ-019 | Preserve queue and token data persistently. | Confirmed baseline | CSF-1 §10 |
| REQ-020 | Provide REST APIs for queue and token operations. | Confirmed baseline | CSF-1 §10 |

### 3.2 Customer identity and queue model

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-037 | Support multiple queues within one service centre. | Owner-confirmed | Owner decision |
| REQ-038 | Require customer name for every guest join. | Owner-confirmed | Owner decision |
| REQ-039 | Accept an optional customer phone number at join. | Owner-confirmed | Owner decision |
| REQ-040 | Prevent a duplicate active join in the same queue using the MVP duplicate definition. | MVP default plus simple assumption | MVP default; ASSUMP-003 |
| REQ-041 | Apply token-number reset independently to every queue. | Owner-confirmed | Owner decision |
| REQ-042 | Assign each queue a configurable prefix for token display. | MVP default | MVP default |
| REQ-043 | Display token identifiers as queue prefix plus the queue's daily sequential number. | MVP default | MVP default |

### 3.3 Waiting estimate and lifecycle

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-044 | Let an administrator configure a fixed average service duration for each queue. | Owner-confirmed | Owner decision |
| REQ-045 | Estimate a waiting token's time from the count of preceding `WAITING` tokens and the queue's fixed average duration. | Owner-confirmed and simple derivation | Owner decision |
| REQ-046 | Exclude the customer's own waiting position from the estimate's preceding-token count. | MVP derivation | Baseline and owner decision |
| REQ-047 | Keep a skipped token in `SKIPPED` and reject further normal lifecycle transitions. | Owner-confirmed | Owner decision |
| REQ-048 | Require a skipped customer to submit a new join to receive a new token. | Owner-confirmed | Owner decision |
| REQ-049 | Reject calls, skips, completions, cancellations, and joins that conflict with persisted state. | MVP default | MVP default |

### 3.4 Administrator and dashboard

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-050 | Allow administrators to create queues and configure the queue name and prefix. | MVP default | MVP default |
| REQ-051 | Allow administrators to configure a queue's fixed average service duration. | Owner-confirmed; defaulted to admin configuration | Owner decision; MVP default |
| REQ-052 | Allow administrators to open or close a queue. | Confirmed baseline; MVP default | CSF-1 §4 |
| REQ-053 | Administrator dashboard totals must show `WAITING`, `CALLED`, `COMPLETED`, `SKIPPED`, and `CANCELLED`. | MVP default | MVP default |
| REQ-054 | Administrators must not select a non-next waiting token for a call. | Owner-confirmed | Owner decision |

### 3.5 Non-functional, interface, testing, and boundaries

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-021 | The interface must be simple and user-friendly. | Baseline non-functional | CSF-1 §8 |
| REQ-022 | Token generation must be reliable and unique within its queue and daily scope. | Baseline non-functional | CSF-1 §8 |
| REQ-023 | Queue position and token status updates must be accurate. | Baseline non-functional | CSF-1 §8 |
| REQ-024 | Implement input validation and clear domain-error handling. | Baseline non-functional | CSF-1 §8 |
| REQ-025 | Produce maintainable and testable code. | Baseline non-functional | CSF-1 §8 |
| REQ-026 | Protect queue and token operations from duplicate or conflicting state changes. | Baseline non-functional plus MVP default | CSF-1 §8; MVP default |
| REQ-027 | Include unit, integration, API, and essential end-to-end automated tests. | MVP default | CSF-1 §7, §10; MVP default |
| REQ-055 | Dashboard and customer views must refresh through polling. | MVP default | MVP default |
| REQ-056 | Customer and administrator experiences must support modern desktop and mobile browsers. | MVP default | MVP default |
| REQ-057 | MVP administrator access may be local-demo access without production-grade authentication. | MVP default | CSF-1 §9; MVP default |
| REQ-058 | REST API behaviour must validate inputs and reject unsupported transitions atomically. | Baseline non-functional | CSF-1 §8, §10 |

### 3.6 Proposed stack and explicit exclusions

| ID | Requirement | Classification | Source |
|---|---|---|---|
| REQ-028 | React, Vite, HTML, CSS, and JavaScript are proposed for the frontend, not approved architecture decisions. | Proposed stack | CSF-1 §7 |
| REQ-029 | Java, Spring Boot, Spring Web, and Spring Data JPA are proposed for the backend, not approved architecture decisions. | Proposed stack | CSF-1 §7 |
| REQ-030 | H2 is proposed for persistence, not an approved architecture decision. | Proposed stack | CSF-1 §7 |
| REQ-031 | JUnit 5, Mockito, component testing, API testing, and end-to-end testing are proposed test approaches. | Baseline testing plus MVP detail | CSF-1 §7 |
| REQ-032 | Do not implement production-grade authentication in the MVP. | Confirmed exclusion; MVP default | CSF-1 §9 |
| REQ-033 | Do not require support for multiple physical service centres in this MVP. | Confirmed exclusion | CSF-1 §9 |
| REQ-034 | Do not implement multi-tenancy. | Confirmed exclusion | CSF-1 §9 |
| REQ-035 | Do not implement online payments. | Confirmed exclusion | CSF-1 §9 |
| REQ-036 | Do not implement SMS notifications. | Confirmed exclusion | CSF-1 §9 |

**Requirement ID range: REQ-001 through REQ-058.**

## 4. Epic and Story Backlog

### EPIC-001 — Queue and Token Foundation

**Objective:** Establish the persistent, configurable queue model and core token rules.

#### US-001 — Maintain persistent queue and token data
- **As an** application user, **I want** queue and token state to remain available after normal application restarts, **so that** operations are reliable.
- **Priority:** P0
- **Dependencies:** None for requirements; persistence technology decision remains an architecture concern.
- **Acceptance criteria:**
  - Queue, queue configuration, and token records persist and remain associated with the correct queue.
  - Every token record exposes one of the five lifecycle states.
  - Normal application restart does not create duplicate or conflicting token records.
- **Traceability:** REQ-018, REQ-019, REQ-022, REQ-023
- **Source:** CSF-1 §6, §8, §10

#### US-002 — Create and configure queues
- **As an** administrator, **I want** to create and configure queues, **so that** the service centre can operate multiple independent lines.
- **Priority:** P0
- **Dependencies:** US-001
- **Acceptance criteria:**
  - An administrator can create a queue with a name and queue-specific prefix.
  - An administrator can set a fixed average service duration for that queue.
  - A queue name, prefix, or duration that fails validation is rejected without creating or changing the queue.
  - Two queues are independently identifiable and maintain independent state and numbering.
- **Traceability:** REQ-037, REQ-042, REQ-050, REQ-051
- **Source:** Owner decision; MVP defaults

#### US-003 — Generate queue-specific daily tokens
- **As a** customer, **I want** a unique prefixed token when I join, **so that** the token identifies my queue and place in today's sequence.
- **Priority:** P0
- **Dependencies:** US-001, US-002
- **Acceptance criteria:**
  - A successful join creates exactly one `WAITING` token.
  - The display identifier combines the selected queue prefix and a sequential number.
  - Concurrent joins receive distinct numbers and persisted state remains consistent.
  - Each queue independently resets to its first daily sequence at midnight.
  - Historical tokens retain their original prefix and sequence; reset affects only later allocations.
- **Traceability:** REQ-003, REQ-015, REQ-022, REQ-041, REQ-042, REQ-043
- **Source:** CSF-1 §3, §5, §8; owner decisions; MVP defaults

#### US-004 — Expose queue operations through REST APIs
- **As a** client application, **I want** REST APIs for queue and token operations, **so that** customer and administrator views use consistent behavior.
- **Priority:** P0
- **Dependencies:** US-001
- **Acceptance criteria:**
  - APIs exist for join, status retrieval, cancellation, call, complete, skip, queue configuration, and open/close operations.
  - Invalid input and unsupported transitions return a non-destructive domain error.
  - Successful responses reflect the state subsequently retrievable from persistence.
- **Traceability:** REQ-020, REQ-024, REQ-049, REQ-058
- **Source:** CSF-1 §8, §10

### EPIC-002 — Customer Queue Participation

**Objective:** Let guests join, track, estimate, and cancel only valid waiting tokens.

#### US-005 — Join an open queue as a guest
- **As a** guest, **I want** to enter my name and optional phone and join an open queue, **so that** I receive a waiting token.
- **Priority:** P0
- **Dependencies:** US-001, US-002, US-003, US-004
- **Acceptance criteria:**
  - Joining requires a nonblank customer name and may include a phone value.
  - A join to an open queue creates one `WAITING` token.
  - A join to a closed queue is rejected and creates no token.
  - A duplicate active join under ASSUMP-003 is rejected and creates no second token.
- **Traceability:** REQ-002, REQ-016, REQ-038, REQ-039, REQ-040
- **Source:** CSF-1 §3–§5; owner decisions; MVP defaults

#### US-006 — View queue position
- **As a** customer, **I want** to see my current queue position, **so that** I know how many waiting customers remain ahead of me.
- **Priority:** P0
- **Dependencies:** US-005
- **Acceptance criteria:**
  - A `WAITING` token's position counts preceding `WAITING` tokens only.
  - `CALLED`, `COMPLETED`, `SKIPPED`, and `CANCELLED` tokens are not counted as waiting positions.
  - Position updates after join, call, complete, skip, or cancellation.
- **Traceability:** REQ-004, REQ-013, REQ-017, REQ-023
- **Source:** CSF-1 §3–§5; owner decisions

#### US-007 — View estimated waiting time
- **As a** customer, **I want** to view an estimate, **so that** I can plan around my place in the queue.
- **Priority:** P1
- **Dependencies:** US-005, US-006
- **Acceptance criteria:**
  - For a `WAITING` token, the estimate is the number of preceding `WAITING` tokens multiplied by that queue's fixed average service duration.
  - Calling or terminal state changes recalculates the estimate from current `WAITING` tokens.
  - A missing or invalid queue duration is not silently treated as a valid estimate.
- **Traceability:** REQ-005, REQ-044, REQ-045, REQ-046
- **Source:** CSF-1 §3–§4; owner decision

#### US-008 — Cancel a waiting token
- **As a** customer, **I want** to cancel my waiting token, **so that** I can leave the queue.
- **Priority:** P0
- **Dependencies:** US-005
- **Acceptance criteria:**
  - A `WAITING` token may transition to `CANCELLED`.
  - A `CALLED`, `COMPLETED`, `SKIPPED`, or already `CANCELLED` token cannot be cancelled.
  - A successful cancellation removes the token from position and waiting estimates.
  - A repeated or invalid cancellation leaves persisted state unchanged.
- **Traceability:** REQ-006, REQ-017, REQ-018, REQ-026
- **Source:** CSF-1 §4–§6; owner decision

### EPIC-003 — Administrator Queue Operations

**Objective:** Provide accurate queue control for opening, calling, completing, skipping, and monitoring.

#### US-009 — View active tokens and dashboard totals
- **As an** administrator, **I want** a queue dashboard with active tokens and all five status counts, **so that** I can monitor operations.
- **Priority:** P0
- **Dependencies:** US-001, US-004, US-005
- **Acceptance criteria:**
  - The dashboard displays separate counts for `WAITING`, `CALLED`, `COMPLETED`, `SKIPPED`, and `CANCELLED`.
  - Counts are specific to the selected queue unless an explicitly aggregate view is implemented.
  - Polling refreshes displayed queue and token state.
  - A terminal token is not included in the waiting count or waiting position.
- **Traceability:** REQ-007, REQ-012, REQ-017, REQ-053, REQ-055
- **Source:** CSF-1 §4–§5; MVP defaults

#### US-010 — Call the next customer in strict FIFO
- **As an** administrator, **I want** to call the oldest waiting customer, **so that** no queue position is bypassed.
- **Priority:** P0
- **Dependencies:** US-005, US-006
- **Acceptance criteria:**
  - The call operation selects the oldest `WAITING` token for the selected queue.
  - No administrator selection of a later waiting token is offered or accepted for call-next.
  - The selected token becomes `CALLED`.
  - A call is rejected when that queue already has a `CALLED` token.
  - Cancelling or terminal-state operations do not change strict FIFO order for remaining waiting tokens.
- **Traceability:** REQ-008, REQ-013, REQ-014, REQ-054
- **Source:** CSF-1 §4–§6; owner decisions

#### US-011 — Complete a called token
- **As an** administrator, **I want** to mark a `CALLED` token completed, **so that** service completion frees the queue for the next call.
- **Priority:** P0
- **Dependencies:** US-010
- **Acceptance criteria:**
  - Only a `CALLED` token can transition to `COMPLETED`.
  - Completing the token removes it from the waiting list and leaves no `CALLED` token.
  - Completion of a waiting, terminal, or already-completed token is rejected.
- **Traceability:** REQ-009, REQ-017, REQ-018
- **Source:** CSF-1 §4–§6

#### US-012 — Skip a called nonresponder
- **As an** administrator, **I want** to mark a `CALLED` nonresponder skipped, **so that** the queue can continue.
- **Priority:** P0
- **Dependencies:** US-010
- **Acceptance criteria:**
  - Only a `CALLED` token may manually transition to `SKIPPED`.
  - The skipped token is terminal and leaves no `CALLED` token.
  - The token is excluded from waiting positions and waiting totals.
  - The customer cannot change or cancel a skipped token.
  - Rejoining creates a new token rather than reactivating the skipped token.
- **Traceability:** REQ-010, REQ-047, REQ-048
- **Source:** CSF-1 §4–§6; owner decisions

#### US-013 — Open or close a queue
- **As an** administrator, **I want** to open or close a queue, **so that** guest joining can be controlled.
- **Priority:** P0
- **Dependencies:** US-002, US-004
- **Acceptance criteria:**
  - An open queue accepts joins.
  - A closed queue rejects joins and creates no token.
  - Closing does not automatically cancel existing waiting or called tokens.
- **Traceability:** REQ-011, REQ-016, REQ-052
- **Source:** CSF-1 §4–§5; MVP default

#### US-014 — View queue configuration and status
- **As an** administrator, **I want** to view and update queue configuration and open/closed status, **so that** operating parameters remain controlled.
- **Priority:** P1
- **Dependencies:** US-002, US-013
- **Acceptance criteria:**
  - The view shows queue name, prefix, fixed average duration, and open/closed status.
  - Updating a configured value applies to later estimates or numbering; historical tokens are not rewritten.
  - Invalid configuration changes are rejected atomically.
- **Traceability:** REQ-044, REQ-050, REQ-051, REQ-052
- **Source:** Owner decision; MVP default

### EPIC-004 — Quality, Validation, and MVP Guardrails

**Objective:** Verify correctness, resilience, usability, and MVP scope.

#### US-015 — Validate inputs and protect operations
- **As a** user, **I want** clear validation and conflict handling, **so that** invalid actions cannot corrupt queue state.
- **Priority:** P1
- **Dependencies:** US-004
- **Acceptance criteria:**
  - Guest name, queue configuration, and operation identifiers are validated.
  - Duplicate joins and repeated or conflicting state transitions are rejected.
  - Rejected operations leave queue and token state unchanged.
  - Errors are understandable and do not expose sensitive implementation details.
- **Traceability:** REQ-024, REQ-026, REQ-040, REQ-049, REQ-058
- **Source:** CSF-1 §8; MVP defaults

#### US-016 — Provide MVP automated coverage
- **As a** development team, **I want** unit, integration, API, and essential end-to-end coverage, **so that** core requirements are verifiable.
- **Priority:** P1
- **Dependencies:** Core implementation stories
- **Acceptance criteria:**
  - Unit tests cover lifecycle transitions, FIFO, per-queue numbering reset, position, and estimate calculations.
  - Integration tests cover persistence and queue operation interactions.
  - API tests cover join, cancel, call, complete, skip, configuration, open/close, validation, and conflict behavior.
  - Essential end-to-end tests cover the primary guest join/track/cancel journey and administrator configure/call/complete/skip journey.
  - The suite passes in the supported modern desktop/mobile browser test environment.
- **Traceability:** REQ-025, REQ-027, REQ-056
- **Source:** CSF-1 §7, §10; MVP default

## 5. Business Rules and Lifecycle

| ID | Rule | Status |
|---|---|---|
| BR-001 | Queue processing is strict FIFO. | Owner-confirmed |
| BR-002 | Only one token may be `CALLED` per queue. | Owner-confirmed |
| BR-003 | Each queue independently resets its sequential number at midnight. | Owner-confirmed |
| BR-004 | Closed queues reject joins. | Confirmed |
| BR-005 | Queue position counts only `WAITING` tokens. | Owner-confirmed |
| BR-006 | Waiting estimate equals preceding `WAITING` count multiplied by the queue's fixed average duration. | Owner-confirmed |
| BR-007 | Only `WAITING` tokens may be cancelled by the customer. | Owner-confirmed |
| BR-008 | Administrators may manually skip only a `CALLED` token. | Owner-confirmed |
| BR-009 | `SKIPPED` is terminal; rejoining requires a new token. | Owner-confirmed |
| BR-010 | `COMPLETED`, `SKIPPED`, and `CANCELLED` are terminal for waiting calculations. | Confirmed |
| BR-011 | Administrators call the oldest waiting token and cannot call a later token directly. | Owner-confirmed |
| BR-012 | Tokens use queue prefix plus daily sequential number. | MVP default |
| BR-013 | A local-demo administrator may access admin operations without production-grade authentication. | MVP default |

### Lifecycle transition matrix

| From | Allowed transitions |
|---|---|
| `WAITING` | Administrator call to `CALLED`; customer cancellation to `CANCELLED` |
| `CALLED` | Administrator completion to `COMPLETED`; administrator skip to `SKIPPED` |
| `COMPLETED` | No transition |
| `SKIPPED` | No transition; a customer must rejoin for a new token |
| `CANCELLED` | No transition |

## 6. Acceptance-Criteria System-Level Checks

1. A guest joins an open queue with a name and optional phone and receives exactly one prefixed, sequential token.
2. A closed queue rejects joins; existing tokens remain governed by their existing lifecycle state.
3. Queue position counts only earlier `WAITING` tokens and updates after lifecycle operations.
4. Waiting estimates use only preceding waiting tokens and the queue's fixed average duration.
5. Midnight independently resets each queue's sequence and preserves historical token values.
6. Calling strict FIFO selects the oldest waiting token and is blocked by an existing called token.
7. Only a called token can complete or be manually skipped.
8. Only waiting tokens can be cancelled.
9. A skipped token remains terminal and rejoining creates a new token.
10. The dashboard displays all five status counts for the selected queue.
11. Duplicate and conflicting operations leave persisted state unchanged.
12. Unit, integration, API, and essential end-to-end tests verify the core journeys and rules.

## 7. Dependencies

| Story | Depends on | Reason |
|---|---|---|
| US-001 | Persistence implementation decision | Persistence behavior is not yet an architecture decision. |
| US-002 | US-001 | Queue configuration requires persisted queue state. |
| US-003 | US-001, US-002 | Token generation requires queue state and configuration. |
| US-004 | US-001 | APIs expose persisted data. |
| US-005 | US-001–US-004 | Join requires queue state, token generation, validation, and API behavior. |
| US-006 | US-005 | Position is derived from waiting token state. |
| US-007 | US-005, US-006 | Estimate requires position and queue duration. |
| US-008 | US-005 | Cancellation operates on the customer's waiting token. |
| US-009 | US-001, US-004, US-005 | Dashboard data derives from queues and token operations. |
| US-010 | US-005, US-006 | Strict FIFO call needs waiting order. |
| US-011 | US-010 | Completion applies to a called token. |
| US-012 | US-010 | Skip applies to a called nonresponder. |
| US-013 | US-002, US-004 | Open/closed state requires configuration and API behavior. |
| US-014 | US-002, US-013 | Configuration status is part of queue administration. |
| US-015 | US-004 | Validation is exercised by API operations. |
| US-016 | Core implementation stories | Tests verify implemented behavior. |

## 8. Assumptions and Remaining Open Questions

### Simple MVP assumptions

- **ASSUMP-001:** Midnight reset uses the application's configured local calendar day.
- **ASSUMP-002:** Polling refresh interval is an implementation detail, not an acceptance-criteria threshold.
- **ASSUMP-003:** A duplicate join is detected for the same queue when customer name and optional phone match an existing active `WAITING` token. If no phone is supplied, name comparison is used.
- **ASSUMP-004:** Administrator-entered queue prefixes remain unique within the demo.
- **ASSUMP-005:** Waiting estimates use a direct multiplication result from preceding waiting count and fixed average duration; display rounding is an implementation detail.

### Blocking conflicts

- None identified. The confirmed decisions and MVP defaults are internally consistent.

## 9. Jira Issue References

- `CSF-1` — QueueLess – Project Requirements and Scope
- Project: `CSF` — QueueLess – Virtual Queue Management System
- Jira issue type: Task
- Jira status: To Do
- Priority: High
- Labels: `project-requirements`, `queueless`

No Jira work items were created or modified. No Jira links, comments, issue children, or external issue references are included beyond `CSF-1`.
