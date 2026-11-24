# QueueLess Test Strategy

## Scope
Independent verification of the approved `docs/impl-plan.md`, `docs/architecture.md`, and the requirements baseline referenced by those documents: `docs/requirements/backlog-proposal.md`.

Planned verification includes:
- Backend Maven build and unit/API/integration suite.
- Persistence restart through two sequential Spring application contexts.
- Backend concurrent join/call/duplicate-join behavior.
- Existing frontend Vitest component suite.
- Existing Vite production build.
- Java 21 configuration and dependency-manifest/lock consistency.
- Scope-exclusion and remaining P0-P6 gap review.

## Test mapping
- T-P0-01/T-P0-02: `mvn verify`, restart test, build startup evidence.
- T-P1-01 to T-P1-03: API and service integration coverage.
- T-P2-01 to T-P2-08: `QueueServiceTest` and `ApiIntegrationTest`.
- T-P3-01 to T-P3-05: MockMvc API integration coverage.
- T-P4-01 to T-P4-03: `frontend/src/App.test.jsx` customer cases.
- T-P5-01 to T-P5-03: `frontend/src/App.test.jsx` administrator cases.
- T-P6-01/T-P6-03: Maven suites and concurrency test.
- T-P6-02: Component/build verification only; real-browser desktop/mobile matrix is blocked.
- T-P6-04: Source-tree scope review.
- T-P6-05: This verification report and human approval gate.

## Environment
- Backend: Temurin JDK 21.0.13.1, Maven 3.9.11.
- Frontend: installed Node.js 26.13.1/npm 11.20.0 and existing `node_modules`; no install or upgrade command was run.
- Browser: no real-browser automation or browser binaries were executed.
