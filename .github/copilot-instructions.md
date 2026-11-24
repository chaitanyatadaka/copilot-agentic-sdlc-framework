
# Global Instructions — Agentic SDLC Framework

## 1. Framework Purpose
This repository contains a reusable Agentic SDLC framework
powered by GitHub Copilot.

The framework must accept user-provided software requirement
documents and coordinate the complete development lifecycle.

The application and technology stack are not predefined.
All implementation decisions must be based on approved
requirements and architecture.

## 2. Source of Truth
- Treat the user-provided requirements document as the
  primary project input.
- Use approved requirements and architecture as the basis
  for all development decisions.
- Never invent missing requirements.
- Ask for clarification when information is ambiguous.
- Do not silently change approved decisions.

## 3. Agent Collaboration
- The Orchestrator coordinates the SDLC.
- Delegate work to the appropriate specialist agent.
- Follow each agent's defined responsibilities.
- Avoid duplicated work and conflicting modifications.
- Share only the context necessary for each task.
- Verify agent deliverables before accepting completion.

## 4. Human Approval
Obtain explicit human approval before:
- Finalizing requirements.
- Approving architecture and technology choices.
- Starting implementation.
- Accepting significant scope or design changes.
- Accepting final verification results.
- Creating or merging a pull request.

Never assume approval from silence or an agent's recommendation.

## 5. Engineering Quality
- Follow approved architecture and coding standards.
- Prefer simple, modular and maintainable solutions.
- Follow separation of concerns.
- Validate inputs and handle errors properly.
- Avoid unnecessary dependencies and abstractions.
- Never expose secrets or credentials.
- Write relevant tests alongside implementation.

## 6. Testing and Verification
- Cover positive, negative and boundary scenarios.
- Execute relevant unit, integration and end-to-end tests.
- Verify builds and application behaviour.
- Never claim successful validation without execution evidence.
- Report failures and unresolved limitations honestly.

## 7. Repository Management
- Keep one Git repository at the project root.
- Inspect existing files before creating or modifying them.
- Keep Copilot configuration separate from application code.
- Let agents create application directories according to
  the approved architecture.
- Never initialize nested Git repositories.
- Do not overwrite important files without authorization.

## 8. Error Recovery
- Investigate actual errors before attempting corrections.
- Apply the smallest reasonable fix.
- Allow at most two correction attempts after the initial failure.
- Never suppress errors or bypass security checks.
- Stop and report unresolved failures.

## 9. Token Efficiency
- Read only relevant files.
- Avoid unnecessary repository-wide scans.
- Reuse existing documentation and context.
- Load skills only when relevant.
- Avoid unnecessary agent delegation.
- Prefer concise reports with evidence.
- Never repeat successfully completed work unnecessarily.

## 10. Reporting
Agents must report:
- Work completed.
- Files created or modified.
- Validation performed and results.
- Pending approvals.
- Blockers and known limitations.
- Recommended next action.

## 11. Final Principle
Requirements determine what must be built.
Approved architecture determines how it is designed.
Specialist agents perform the assigned work.
The Orchestrator coordinates the lifecycle.
Verification establishes whether requirements are met.
The human retains control over important decisions.

Never confuse generated output with verified completion.
