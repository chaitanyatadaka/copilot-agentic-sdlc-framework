# QueueLess Test Results

## Consolidated status

This report consolidates existing execution records; no tests or audits were repeated for this update.

| Result | Count / status |
|---|---:|
| Backend automated tests | 14 passed, 0 failed, 0 errors, 0 skipped |
| Frontend component tests | 10 passed, 0 failed |
| Frontend production build | Passed |
| Recorded manual application checks | Passed as limited smoke checks; see below |
| Required full browser/version/viewport E2E matrix | Blocked / not evidenced |
| Numeric coverage report | Not measured |

The backend count includes the later demo-boundary integration test and supersedes the earlier 13-test record. The frontend count of 10 supersedes earlier reports of 9. Build success is reported separately and is not included in test counts.

## Backend

### `mvn.cmd verify`

- Environment: Temurin Java 21.0.13.1 and Maven 3.9.11, configured through the existing local tool locations.
- Result: `BUILD SUCCESS`; 14 tests, 0 failures, 0 errors, 0 skipped.
- Breakdown:
  - `ApiIntegrationTest`: 11
  - `DemoAdminBoundaryTest`: 1
  - `PersistenceRestartTest`: 1
  - `QueueServiceTest`: 1
- Evidence: Maven output and Surefire reports under `backend/target/surefire-reports/`.
- The latest recorded backend run includes the local demo-boundary fix. No test was rerun for this consolidation.

Coverage evidenced by these tests includes API validation and lifecycle behavior, queue/token operations, dashboard counts, persistence restart, single-process concurrent joins/calls, and local demo admin operations. The persistence restart test uses a temporary file-backed H2 database across two sequential Spring contexts. The concurrency test uses eight workers and confirms unique joins, one successful call-next, and duplicate-join conflict behavior.

`DemoAdminBoundaryTest` runs the demo profile with an isolated in-memory database, verifies that `server.address` resolves to loopback, and exercises local queue creation, join, call-next, and completion.

## Frontend

### `npm.cmd test`

- Result recorded after the UI refresh: Vitest 2.1.9, 1 test file, 10 tests passed, 0 failed.
- The component suite covers customer queue listing/join/error/cancellation/polling behavior and administrator configuration, dashboard totals, call/complete/skip, and update conflicts.
- The API client is mocked in component tests; these tests do not establish end-to-end browser behavior.

### `npm.cmd run build`

- Result: Vite 5.4.21 production build passed.
- This build result is separate from the test count.

## Manual application verification

The owner reports completion of manual application testing. Existing recorded manual evidence specifically includes:

- Customer and administrator screens opened during a local browser spot-check at desktop and mobile viewport sizes.
- Backend `/api/queues`, frontend `/`, and the Vite-proxied `/api/queues` returned HTTP 200.
- No browser runtime errors were reported after reload.
- An invalid queue-create request through Vite returned HTTP 400 with the expected structured validation response.

The recorded live check did not submit a valid create against persistent demo data. The automated API suite and the later demo-boundary integration test do exercise successful creation. Browser versions, viewport dimensions, full journey steps, and screenshots/logs for a formal matrix are not recorded here.

## Outstanding verification evidence

- **Required browser matrix — Blocked:** No executed latest-two-version Chrome, Edge, Firefox, and Safari desktop/mobile E2E matrix with versioned evidence is recorded. The local browser spot-check does not replace that matrix.
- **Numeric code coverage — Not measured:** No coverage report was produced; the approved plan specifies no numeric threshold.
- **Requirement-to-test mapping — Partial:** Broad requirement ranges are mapped in workflow state, but a complete REQ-001–REQ-058 test/evidence matrix is not present.

These are not marked as accepted limitations; see the open review findings and approval status in `docs/design-review.md` and `docs/workflow/state.md`.
