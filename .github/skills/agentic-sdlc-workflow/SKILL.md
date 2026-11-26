---
name: agentic-sdlc-workflow
description: Coordinates the complete application-independent software development lifecycle using specialist agents, Jira requirements and mandatory human approval gates. Use when starting or continuing an end-to-end software project.
---

# Agentic SDLC Workflow

## Mission

Coordinate the complete software development lifecycle using the available specialist agents.

The framework must work with different user stories, applications, architectures and technology stacks.

Never assume a predefined application or technology.

## Specialist Agents

Use the following agents for their assigned responsibilities:

1. SDLC Orchestrator — coordination and workflow state.
2. Requirements Agent — requirements analysis and Jira integration.
3. Architect-Planner Agent — architecture and implementation planning.
4. Developer Agent — application implementation.
5. Testing-Verification Agent — testing and verification.
6. Reviewer-PR Agent — independent reviews and PR preparation.

Do not claim an agent was invoked unless delegation actually occurred.

## Workflow

Use this sequence, advancing only after the stage exit criteria are satisfied:

1. **Requirements:** Choose user-provided documents or Jira. For Jira, discover accessible projects with `listJiraProjects` via `atlassian-executeRead` when needed, then retrieve only selected project/issues. Requirements Agent clarifies ambiguity, preserves source references, assigns stable `REQ-nnn` IDs, and produces `docs/requirements.md`. Present proposed Jira creates/edits/comments/transitions before writing. Obtain approval of the requirements and separately approve each Jira write operation, target, and content/status.
2. **Architecture and design review:** Architect-Planner creates `docs/architecture.md`, `docs/impl-plan.md`, and dependency-ordered tasks mapped to requirement IDs. Reviewer-PR independently creates `docs/design-review.md`. Resolve blocking findings and obtain approval of architecture and plan revisions.
3. **Implementation authorization:** Present the approved scope and task IDs. Obtain explicit approval before Developer edits application files. Developer implements only approved tasks and records requirement IDs for changes and tests.
4. **Verification:** Testing-Verification maps tests to requirement IDs and implementation tasks, executes approved checks, and records each test as `Passed`, `Failed`, `Skipped`, or `Blocked` with command/procedure and evidence. Any mandatory failure, unexplained skip, or blocker prevents acceptance and returns work to Developer.
5. **Independent code review:** Reviewer-PR reviews the current approved revisions and verification evidence. Blocking findings return to Developer; affected tests are rerun before independent re-review.
6. **Pull request:** Confirm repository and base branch, verify the approved head feature branch already exists remotely, prepare the PR details, and obtain explicit approval of repository, branches, title, and body. Reviewer-PR may create the PR only after that approval. No agent may merge; merge remains a human action.

### Change invalidation and recovery

Use the dependency chain `requirements -> architecture/design review -> plan -> implementation -> tests/verification -> code review -> PR`.

- Requirement changes return to Requirements. Invalidate architecture and every downstream artifact affected by the changed requirement; preserve unrelated approved requirements and unaffected evidence.
- Architecture or technology changes return to Architect-Planner. Repeat independent design review and planning; invalidate only impacted implementation tasks and their dependent tests/reviews.
- Plan or implementation-scope changes require approval before editing. Revalidate affected implementation, tests, and review.
- Failed tests return to Developer. After fixes, rerun failed tests and dependent regression checks, then resume independent review.
- Blocking code-review findings return to Developer, then affected testing, then independent re-review.
- A changed approved artifact invalidates its dependent approvals/evidence. Revalidate downstream stages in dependency order; do not restart unaffected stages.
- On tool/delegation failure, incomplete output, or interruption, record the error and recovery attempt. Resume from the last verified state; retry only after diagnosing the cause. After the initial failure, allow at most two correction attempts. Keep unresolved work `Blocked` and do not advance.

### Stage exit checks

The Orchestrator confirms inputs, required outputs, current revision identifiers, traceability, evidence, unresolved defects, and approval scope before recording a stage as complete. Agents report evidence and blockers; they never self-approve a stage or edit approval records.

## Human Approval Rules

Never assume approval.

Wait for explicit approval before:
- Finalizing requirements, architecture, and implementation plan.
- Each Jira creation, update, comment, or transition (exact target and proposed operation/content).
- Starting implementation or changing approved scope/architecture.
- Accepting final verification results.
- Creating a pull request (exact repository, existing remote head branch, base branch, title, and body).

## Workflow Tracking

Maintain:

`docs/workflow/state.md`

Use the initial schema in that file. Track current stage, responsible agent/task, completed and pending tasks, deliverable revision/hash, approval scope/status, validation evidence and test states, unresolved defects/review findings, invalidated downstream work, recovery attempts, and next action.

Only the Orchestrator updates workflow status, approvals, invalidation, and next actions after verifying actual evidence. State is an audit record, not authorization to invoke a protected operation.

## Token Efficiency

- Read only relevant files.
- Avoid repeated repository scans.
- Reuse approved deliverables.
- Delegate focused tasks.
- Avoid duplicate work.
- Keep reports concise.
- Never claim unverified completion.

## Final Principle

Requirements define what to build.
Architecture defines how to build it.
Specialist agents execute their responsibilities.
Verification provides evidence.
The human retains control over important decisions.
