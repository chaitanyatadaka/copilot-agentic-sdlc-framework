---
name: development
description: Implements software features using approved requirements, architecture and implementation plans. Use when creating or modifying application source code.
---

# Development

## 1. Understand the Project

Before implementation:
- Read approved requirements.
- Read approved architecture.
- Read the implementation plan.
- Inspect relevant existing source code.
- Identify the assigned implementation tasks.
- Confirm each task and requirement ID is approved in `docs/workflow/state.md`.

Never assume a predefined application or technology stack.

## 2. Project Structure

- Create the application directory structure based on approved architecture.
- Organize source code logically.
- Separate application components and responsibilities.
- Keep tests and documentation appropriately organized.
- Do not initialize nested Git repositories.

## 3. Implementation

- Follow approved architecture and design decisions.
- Implement only approved functionality.
- Map changed components and tests to approved requirement IDs and plan task IDs.
- Follow language-specific coding standards.
- Use appropriate design patterns without overengineering.
- Implement input validation and error handling.
- Apply secure coding practices.
- Avoid unnecessary dependencies.
- Never hardcode credentials or secrets.

## 4. Code Quality

Ensure code is:
- Readable.
- Modular.
- Maintainable.
- Reusable where appropriate.
- Consistent with existing conventions.
- Properly documented where necessary.

## 5. Unit Testing

- Write relevant unit tests alongside implementation.
- Cover positive, negative and boundary scenarios.
- Test important business rules.
- Mock external dependencies when appropriate.
- Execute tests and record actual results.

## 6. Error Handling

- Investigate failures using actual logs and errors.
- Identify the root cause.
- Apply the smallest reasonable correction.
- Avoid suppressing errors or bypassing validation.
- Report unresolved blockers.
- Stop and return changes to the Orchestrator when requirements, architecture, or approved scope changes; do not continue against stale approvals.

## 7. Documentation

Update relevant documentation when implementation changes affect:
- Setup instructions.
- Configuration.
- APIs.
- Application behaviour.
- Dependencies.

Never include secrets in documentation.

## 8. Approval and Completion

- Do not start implementation without explicit approval.
- Obtain approval before significant architectural or scope changes.
- Report completed tasks and remaining work.
- Provide files changed and test execution evidence.
- Do not claim successful implementation without verification.
