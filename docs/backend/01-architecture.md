# Backend Architecture & Layering

## Layer Responsibilities (Strict Boundaries)

### Controller Layer (HTTP/REST)
- Handle HTTP request/response only
- Extract parameters from request, map to service method calls
- Return appropriate HTTP status codes
- NEVER contain business logic
- ❌ `@RestController` calculating prices or validating business rules
- ✅ `@RestController` calling `orderService.calculatePrice()` and returning result

### Service Layer (Business Logic)
- Contains ALL business rules and logic
- Orchestrates multiple repositories if needed
- Handles transactions (`@Transactional`)
- Can be called by multiple controllers or other services
- Should NOT know about HTTP (no `HttpServletRequest`, `@RequestParam`, etc.)
- ✅ `CustomerService` validates customer data, applies discount rules, sends notifications

### Repository Layer (Data Access)
- ONE responsibility: CRUD operations to database
- Use Spring Data JPA repositories where possible
- Custom queries ONLY when JPA can't express them
- NEVER contain business logic
- ❌ Repository calculating "is customer premium" - that's service logic
- ✅ Repository providing `findCustomersByStatus()` - data filtering only

### Model/Entity Layer (Data Structures)
- Entities: Map directly to database tables, minimal behavior — live in
  `infrastructure/<adapter>/entity` (`CustomerJpaEntity`, `@Embeddable`s)
- DTOs: Transfer data between layers (commands, query results, port payloads) — live in
  `application/dto`
- Value Objects/aggregates: Immutable domain types with behavior (`Money`, `Address`,
  `Customer`) — live in `domain/model`
- ✅ Entity: `CustomerJpaEntity` with `@Id`, `@Column` annotations, in
  `infrastructure/persistence/entity`
- ✅ DTO: `CustomerView` with only fields needed for API response, in `application/dto`

### Configuration Layer (Setup)
- `@Configuration` classes for Spring beans
- Security configuration, CORS, interceptors
- Database connection setup
- Keep configuration separate from business logic

## Layer Interaction Rules

### Data Flow (Bottom → Top)
- Database → Repository → Service → Controller → Client
- Each layer depends ONLY on the layer below it (or abstractions)

### Dependency Direction
- Controller depends on Service (interface)
- Service depends on Repository (interface)
- Repository depends on Entity
- **Never**: Service depends on Controller, Repository depends on Service

### Cross-Layer Concerns
- Validation: Use `@Valid` in Controller, business validation in Service
- Error Handling: `@ControllerAdvice` for global exceptions, domain exceptions in Service
- Logging: Log at layer entry/exit, not inside business logic
- Security: Authentication in Controller (filters), authorization in Service

## Anti-Patterns to Avoid

### ❌ Anemic Domain Model
- Entities with only getters/setters, no behavior
- ✅ Give entities behavior: `order.calculateTotal()`, not `orderCalculator.calculate(order)`

### ❌ God Service
- One service doing everything (validation, business logic, email, PDF generation)
- ✅ Split: `CustomerValidationService`, `CustomerBusinessService`, `CustomerNotificationService`

### ❌ Service in Controller
- Business logic written directly in `@RestController` methods
- ✅ Extract to `@Service`, inject into controller

### ❌ Repository with Business Logic
- Repository methods like `findPremiumCustomers()` (premium is business logic)
- ✅ Repository: `findByStatus()`, Service: filter for premium

### ❌ Fat DTO
- DTOs with 20+ fields, mixing concerns
- ✅ Split: `CreateCustomerRequest`, `UpdateCustomerRequest`, `CustomerResponse`

### ❌ Circular Dependencies
- Service A depends on Service B, Service B depends on Service A
- ✅ Extract shared logic to Service C, or use events/callbacks

## Package Structure

This project organizes by DDD/Spring-Modulith module, not by technical layer at the top level.
`docs/backend/02-ddd-modulith.md` is the authoritative module layout; summary:

```
<module>/
├── domain/
│   ├── model/                     aggregates, VOs, enums, domain records
│   ├── service/                   domain services only
│   └── event/
├── application/
│   ├── dto/                       commands, query results, port payloads
│   ├── mapper/                    domain → generated-OpenAPI-model mappers
│   ├── port/                      interfaces only
│   ├── service/                   use cases only
│   ├── event/
│   └── exception/
├── infrastructure/<adapter>/      adapters: persistence, image, accesslink, registrationinvite
│   ├── entity/                    *JpaEntity, @Embeddable
│   ├── mapper/                    domain ↔ entity mappers
│   └── <X>RepositoryImpl.java, <X>JpaRepository.java
└── interfaces/rest/               controllers (request/response DTOs are generated → shared.web.openapi.model)
```

See [`docs/backend/02-ddd-modulith.md`](02-ddd-modulith.md) for the full rationale.
