---
name: code-review
description: Reviews software architecture, source code, security, maintainability and testing evidence. Use when conducting independent reviews or preparing pull requests.
---

# Code Review

## 1. Understand the Changes

- Read approved requirements and architecture.
- Review the implementation plan.
- Inspect relevant source code and changed files.
- Understand the purpose of each change.
- Compare implementation against acceptance criteria.
- Use only approved revisions recorded in workflow state and verify requirement-to-code-to-test traceability.

## 2. Architecture Review

Evaluate:
- Alignment with approved requirements.
- Component responsibilities.
- Separation of concerns.
- Technology compatibility.
- Scalability and maintainability.
- Security considerations.
- Unnecessary complexity and dependencies.

Record architecture findings in `docs/design-review.md`.

## 3. Code Quality Review

Check for:
- Incorrect or incomplete functionality.
- Poor error handling.
- Missing input validation.
- Code duplication.
- Unnecessary abstractions.
- Naming and readability issues.
- Maintainability concerns.
- Missing or inadequate tests.

## 4. Security Review

Check for:
- Hardcoded secrets and credentials.
- Unsafe input handling.
- Authentication and authorization weaknesses.
- Sensitive information exposure.
- Insecure dependencies.
- Common application security vulnerabilities.

Do not expose discovered secrets in reports.

## 5. Test Evidence Review

- Verify that relevant tests were executed.
- Review actual test results.
- Identify missing scenarios.
- Check requirement-to-test traceability.
- Never assume tests passed based on generated reports alone.
- Confirm each test is marked `Passed`, `Failed`, `Skipped`, or `Blocked` and supported by execution evidence.

## 6. Finding Classification

Classify findings as:

- Critical: Severe security or functional risk.
- High: Major defect or significant risk.
- Medium: Important quality or reliability concern.
- Low: Minor improvement or maintainability issue.

For each finding include:
- Description.
- Affected file or component.
- Impact.
- Evidence.
- Recommended correction.

## 7. Pull Request Preparation

Create `docs/pr-description.md`.

Include:
- Change summary.
- Requirements addressed.
- Files and components changed.
- Tests executed and results.
- Known limitations.
- Reviewer checklist.
- Changelog.

## 8. Approval and Safety

- Perform reviews independently.
- Do not silently modify application code.
- Report critical and high-severity findings.
- Request corrections from the responsible agent.
- Re-review corrected changes.
- When findings require code changes, return to Developer, rerun affected tests, then independently re-review. If an input revision changes, invalidate only dependent review/verification and downstream artifacts.
- Obtain explicit approval before creating a pull request.
- Never merge automatically.
- Verify the approved repository, base branch, and pre-existing remote head feature branch before presenting exact PR details for approval. Keep merge under human control.

## 9. Completion Report

Report:
- Architecture review findings.
- Code review findings.
- Security concerns.
- Test evidence.
- Unresolved issues.
- Pull request readiness.
- Pending approvals.
