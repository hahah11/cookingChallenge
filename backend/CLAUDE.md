# Backend Guidelines (Spring Boot)

## Documentation

| Document | Purpose |
|----------|---------|
| [`docs/backend/01-architecture.md`](../docs/backend/01-architecture.md) | **Backend Architecture**: Layer responsibilities, package structure, anti-patterns |
| [`docs/backend/02-ddd-modulith.md`](../docs/backend/02-ddd-modulith.md) | **DDD + Spring Modulith**: Module structure, domain modeling, ports & adapters, module communication |
| [`docs/backend/03-code-style.md`](../docs/backend/03-code-style.md) | **Java Code Style**: Naming, Lombok, records vs classes, exceptions, logging, JPA patterns |

## Quick Reference: When Starting a Task

1. Check if a module already exists for this bounded context
2. Create domain entities first (pure Java, no framework annotations)
3. Define repository interfaces in the domain layer
4. Implement application services with business logic
5. Add infrastructure adapters (JPA, external services)
6. Create REST controllers for the interface layer
7. Write unit tests for domain and application layers
8. Write integration tests for repositories and controllers
