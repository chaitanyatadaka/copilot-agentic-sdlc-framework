---
name: architecture-design
description: Designs application-independent software architectures, evaluates technology choices, defines components and documents technical decisions. Use when designing or reviewing software architecture.
---

# Architecture Design

## 1. Understand Requirements

- Read approved requirements.
- Identify functional and non-functional needs.
- Understand expected users, workloads and constraints.
- Identify integrations and external dependencies.
- Never assume a predefined application or technology stack.

## 2. Architecture Selection

Evaluate suitable architectural approaches based on:
- Application complexity.
- Scalability requirements.
- Performance expectations.
- Security requirements.
- Maintainability.
- Deployment constraints.
- Development cost and complexity.

Recommend the simplest architecture that satisfies the requirements.

## 3. Technology Selection

Evaluate appropriate:
- Programming languages.
- Frameworks and libraries.
- Databases and storage.
- APIs and communication protocols.
- Infrastructure and deployment technologies.
- Testing tools.

Explain important choices, alternatives and trade-offs.

Avoid unnecessary dependencies and overengineering.

## 4. Component Design

Define:
- Application components.
- Component responsibilities.
- Interfaces and interactions.
- Data flows.
- External integrations.
- Security boundaries.
- Error-handling approaches.

## 5. Architecture Documentation

Document:
- Architecture overview.
- Technology stack and rationale.
- Component descriptions.
- Data design.
- API and integration design.
- Security considerations.
- Deployment approach.
- Risks and limitations.

Save the approved architecture in `docs/architecture.md`.

## 6. Implementation Planning

Break the approved architecture into:
- Implementation phases.
- Dependency-ordered tasks.
- Task descriptions.
- Expected deliverables.
- Dependencies and blockers.
- Completion criteria.

Save the plan in `docs/impl-plan.md`.

## 7. Design Review

Check architecture alignment with requirements.

Identify:
- Missing components.
- Unnecessary complexity.
- Security concerns.
- Scalability limitations.
- Technology incompatibilities.
- Unresolved design decisions.

Record findings in `docs/design-review.md`.

## 8. Approval Rules

- Present recommendations before finalizing architecture.
- Never silently change approved decisions.
- Obtain explicit human approval before implementation.
- Resolve critical design concerns before proceeding.
