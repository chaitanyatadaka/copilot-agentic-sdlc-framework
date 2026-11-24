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

### Stage 1: Requirements

- Ask the user to select Jira or an alternative document source.
- Fetch relevant Jira stories through Atlassian MCP when selected.
- Analyse requirements and identify missing information.
- Ask clarification questions.
- Produce `docs/requirements.md`.
- Obtain human approval.

### Stage 2: Architecture

- Invoke Architect-Planner Agent.
- Recommend architecture and technologies based on approved requirements.
- Produce `docs/architecture.md`.
- Obtain independent architecture review from Reviewer-PR Agent.
- Produce `docs/design-review.md`.
- Resolve critical findings.
- Obtain human approval.

### Stage 3: Implementation Planning

- Produce `docs/impl-plan.md`.
- Organize tasks by dependencies and priority.
- Identify blocked tasks.
- Obtain human approval.

### Stage 4: Implementation

- Invoke Developer Agent.
- Create application structure based on approved architecture.
- Implement planned features.
- Write relevant unit tests.
- Track progress and report blockers.
- Obtain approval before significant scope or architecture changes.

### Stage 5: Testing and Verification

- Invoke Testing-Verification Agent.
- Create and execute relevant tests.
- Verify requirements and acceptance criteria.
- Produce test and verification reports.
- Obtain human acceptance.

### Stage 6: Independent Review

- Invoke Reviewer-PR Agent.
- Review implementation, security and code quality.
- Report findings and severity.
- Resolve critical and high-severity findings.
- Re-review corrected changes.

### Stage 7: Pull Request

- Prepare `docs/pr-description.md`.
- Include summary, changes, test evidence and limitations.
- Present the PR for human approval.
- Create the PR only after explicit authorization.
- Never merge automatically.

## Human Approval Rules

Never assume approval.

Wait for explicit approval before:
- Finalizing requirements.
- Approving architecture.
- Starting implementation.
- Accepting final verification.
- Creating a pull request.
- Merging a pull request.

## Workflow Tracking

Maintain:

`docs/workflow/state.md`

Track current stage, assigned agents, deliverables, approvals, validation results, blockers and next actions.

Update the state only after verifying actual work.

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
