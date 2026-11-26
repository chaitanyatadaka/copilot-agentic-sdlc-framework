---
name: testing
description: Plans, creates and executes automated tests and verifies application behaviour against approved requirements. Use when testing, debugging or validating software.
---

# Testing

## 1. Understand Requirements

- Read approved requirements and acceptance criteria.
- Review architecture and implementation.
- Identify critical application workflows.
- Map test scenarios to requirements.
- Assign test IDs and map each test to requirement IDs and implementation task IDs.
- Use the testing tools appropriate to the approved technology stack.

## 2. Test Planning

Identify relevant:
- Unit tests.
- Integration tests.
- API tests.
- UI and end-to-end tests.
- Regression tests.
- Security tests, where applicable.

Prioritize tests based on application risk and functionality.

## 3. Test Design

Cover:
- Positive scenarios.
- Negative scenarios.
- Boundary conditions.
- Invalid and missing inputs.
- Error handling.
- Business rules.
- Authentication and authorization, where applicable.

Define clear expected results for every test.

## 4. Test Implementation

- Use appropriate testing frameworks.
- Follow existing project conventions.
- Keep tests independent and repeatable.
- Avoid unnecessary duplication.
- Mock external services where appropriate.
- Never use real production credentials or sensitive data.

## 5. Test Execution

- Run relevant automated tests.
- Execute application builds.
- Verify application startup and important workflows.
- Capture actual execution results.
- Identify failed, skipped and blocked tests.
- Classify every test as exactly one of `Passed`, `Failed`, `Skipped`, or `Blocked`; record the procedure or command, evidence, and a reason for every skipped or blocked test.
- Never claim a test passed without execution evidence.

## 6. Defect Management

For each failure, record:
- Test scenario.
- Expected result.
- Actual result.
- Reproduction steps.
- Error details.
- Severity.
- Relevant evidence.

Coordinate fixes with the Developer Agent.

Re-run affected tests after corrections.
Re-run dependent regression checks after fixes and update their evidence and statuses.

## 7. Verification Reports

Create:
- `docs/test-strategy.md`
- `docs/test-results.md`
- `docs/verification-report.md`

Include test coverage, execution evidence, defects and unresolved limitations.

## 8. Completion Criteria

- Relevant tests have been executed.
- Critical application workflows have been verified.
- Acceptance criteria have been evaluated.
- Remaining defects and limitations are documented.
- Results are presented for human acceptance.

Never mark verification complete when important tests remain unresolved.
Block completion when a mandatory test is `Failed`, `Blocked`, or `Skipped` without explicit human acceptance of the limitation.
