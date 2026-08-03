# CookingChallenge Workspace

This workspace contains two projects: a Spring Boot backend and an Angular frontend with standalone components.

## General Rules

- If a change affects both frontend and backend, keep them consistent.
- Prefer updating backend and frontend in the same task when appropriate.
- Don't modify generated code.
- Ask before changing public APIs.
- Use intellij MCP server as a read-only oracle where ever possible. Also for running tests
- - DON'T use intellij when you want to run bash commands
---

## Documentation Structure

All development guidelines are organized in the `docs/` folder by technology:

### Shared (Backend + Frontend)

| Document | Purpose |
|----------|---------|
| [`docs/shared/01-principles.md`](docs/shared/01-principles.md) | **Software Design Principles**: SOLID, Cohesion/Coupling, DRY, KISS, Separation of Concerns |
| [`docs/shared/02-clean-code.md`](docs/shared/02-clean-code.md) | **Clean Code**: Naming, functions, comments, error handling, refactoring rules |
| [`docs/shared/03-testing.md`](docs/shared/03-testing.md) | **Testing Guidelines**: F.I.R.S.T. principles, test naming, coverage, data management |
| [`docs/shared/04-api-design.md`](docs/shared/04-api-design.md) | **API Design**: REST conventions, status codes, response formats, versioning, validation |

Backend-specific and frontend-specific guideline docs are indexed in `backend/CLAUDE.md` and `frontend/CLAUDE.md` respectively.

---

## Critical Rules (Non-Negotiable)

1. **Follow SOLID principles** - See [`01-principles.md`](docs/shared/01-principles.md#solid-principles)
2. **Only refactor code you touch** - See [`02-clean-code.md`](docs/shared/02-clean-code.md#refactoring-rule)
3. **Keep layers separated** - See [`backend/01-architecture.md`](docs/backend/01-architecture.md#layer-responsibilities-strict-boundaries)
4. **Use DDD patterns for domain logic** - See [`backend/02-ddd-modulith.md`](docs/backend/02-ddd-modulith.md#ddd-domain-entities-rich-domain-model)
5. **Write tests for every feature** - See [`03-testing.md`](docs/shared/03-testing.md#general-principles)
6. **Follow REST API conventions** - See [`04-api-design.md`](docs/shared/04-api-design.md#rest-conventions)

---

## Planning

When creating implementation plans:

- Save every plan to `docs/cookingChallenge/plans/`.
- Use Markdown.
- Update the existing plan instead of creating duplicates.
- Treat plans as project artifacts that should be committed to git.
