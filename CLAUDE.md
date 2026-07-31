# CookingChallenge Workspace

This workspace contains two projects:

- **cookingChallenge-backend**: Spring Boot 4 with DB2
- **cookingChallenge-angular**: Angular 21 with standalone components

## General Rules

- If a change affects both frontend and backend, keep them consistent.
- Prefer updating backend and frontend in the same task when appropriate.
- Don't modify generated code.
- Ask before changing public APIs.

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

### Backend (Spring Boot / Java)

| Document | Purpose |
|----------|---------|
| [`docs/backend/01-architecture.md`](docs/backend/01-architecture.md) | **Backend Architecture**: Layer responsibilities, package structure, anti-patterns |
| [`docs/backend/02-ddd-modulith.md`](docs/backend/02-ddd-modulith.md) | **DDD + Spring Modulith**: Module structure, domain modeling, ports & adapters, module communication |
| [`docs/backend/03-code-style.md`](docs/backend/03-code-style.md) | **Java Code Style**: Naming, Lombok, records vs classes, exceptions, logging, JPA patterns |

### Frontend (Angular / TypeScript)

| Document | Purpose |
|----------|---------|
| [`docs/frontend/01-architecture.md`](docs/frontend/01-architecture.md) | **Frontend Architecture**: Layer responsibilities, project structure, component communication |
| [`docs/frontend/02-components.md`](docs/frontend/02-components.md) | **Components & Templates**: Component design, @Input/@Output, templates, change detection, forms |
| [`docs/frontend/03-services-state.md`](docs/frontend/03-services-state.md) | **Services & State**: Service patterns, signals, RxJS, HTTP interceptors, state management |
| [`docs/frontend/04-routing-forms-http.md`](docs/frontend/04-routing-forms-http.md) | **Routing, Forms & HTTP**: Guards, resolvers, reactive forms, HTTP client setup, API services |
| [`docs/frontend/05-performance-testing.md`](docs/frontend/05-performance-testing.md) | **Performance & Testing**: Optimization techniques, component/service testing, best practices |

---

## Critical Rules (Non-Negotiable)

1. **Follow SOLID principles** - See [`01-principles.md`](docs/shared/01-principles.md#solid-principles)
2. **Only refactor code you touch** - See [`02-clean-code.md`](docs/shared/02-clean-code.md#refactoring-rule)
3. **Keep layers separated** - See [`backend/01-architecture.md`](docs/backend/01-architecture.md#layer-responsibilities-strict-boundaries)
4. **Use DDD patterns for domain logic** - See [`backend/02-ddd-modulith.md`](docs/backend/02-ddd-modulith.md#ddd-domain-entities-rich-domain-model)
5. **Write tests for every feature** - See [`03-testing.md`](docs/shared/03-testing.md#general-principles)
6. **Follow REST API conventions** - See [`04-api-design.md`](docs/shared/04-api-design.md#rest-conventions)

---

## Quick Reference: When Starting a Task

### Backend (Spring Boot)

1. Check if a module already exists for this bounded context
2. Create domain entities first (pure Java, no framework annotations)
3. Define repository interfaces in the domain layer
4. Implement application services with business logic
5. Add infrastructure adapters (JPA, external services)
6. Create REST controllers for the interface layer
7. Write unit tests for domain and application layers
8. Write integration tests for repositories and controllers

### Frontend (Angular)

1. Check existing feature modules for similar patterns
2. Create model/interfaces matching backend DTOs
3. Build API service extending base HTTP patterns
4. Create presentational components first
5. Build container components with state management
6. Add route configuration and guards if needed
7. Write component tests and service tests

---

## Planning

When creating implementation plans:

- Save every plan to `docs/cookingChallenge/plans/`.
- Use Markdown.
- Update the existing plan instead of creating duplicates.
- Treat plans as project artifacts that should be committed to git.

## Project Structure

```
/home/nfrai/projects/cookingChallenge/
├── cookingChallenge-backend/    # Spring Boot 4 application
│   └── src/main/java/com/cookingchallenge/
│       ├── customer/            # Customer module (DDD bounded context)
│       ├── order/               # Order module
│       └── shared/              # Shared kernel
├── cookingChallenge-angular/    # Angular 21 application
│   └── src/app/
│       ├── features/            # Feature modules (standalone components)
│       ├── shared/              # Reusable components
│       └── core/                # Services, guards, interceptors
└── docs/                        # Development guidelines
    ├── shared/
    │   ├── 01-principles.md
    │   ├── 02-clean-code.md
    │   ├── 03-testing.md
    │   └── 04-api-design.md
    ├── backend/
    │   ├── 01-architecture.md
    │   ├── 02-ddd-modulith.md
    │   └── 03-code-style.md
    └── frontend/
        ├── 01-architecture.md
        ├── 02-components.md
        ├── 03-services-state.md
        ├── 04-routing-forms-http.md
        └── 05-performance-testing.md
```
