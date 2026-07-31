# Software Design Principles

**CRITICAL: All code must follow these fundamental principles**

## SOLID Principles

### S - Single Responsibility Principle (SRP)
- A class/module must have ONE reason to change
- Each class should handle ONE responsibility or concern
- Example: `CustomerRepository` handles data access ONLY; `CustomerService` handles business logic ONLY
- If a class does too much (e.g., validates, transforms, saves, and sends emails), split it

### O - Open/Closed Principle (OCP)
- Classes should be OPEN for extension, CLOSED for modification
- Add new behavior by creating new classes, not changing existing ones
- Use polymorphism and strategy patterns instead of large if/else chains
- Example: Add `PremiumCustomerDiscount` instead of modifying `CustomerDiscount` to handle premium customers

### L - Liskov Substitution Principle (LSP)
- Subtypes must be substitutable for their base types without breaking behavior
- Don't create subclasses that throw exceptions for unsupported methods
- Prefer composition over inheritance when subclass behavior varies significantly

### I - Interface Segregation Principle (ISP)
- Clients should not depend on methods they don't use
- Create focused, specific interfaces instead of one "god interface"
- Example: `Readable` and `Writable` instead of `ReadWrite` when a class only needs reading

### D - Dependency Inversion Principle (DIP)
- High-level modules must NOT depend on low-level modules; both depend on abstractions
- Dependencies should be injected (constructor injection), not created internally
- Example: `CustomerService` depends on `CustomerRepository` interface, not a concrete implementation

## Cohesion and Coupling

### High Cohesion (DO)
- Classes should have strongly related responsibilities
- Methods should operate on the class's own data
- Group related functionality together
- Example: `Order` class should calculate its own total (`order.calculateTotal()`)

### Low Coupling (DO)
- Minimize dependencies between classes
- Use dependency injection to decouple components
- Avoid "knows too much" anti-pattern (don't access internal data of other classes)
- Example: Pass only what a method needs, not the entire object graph

### Tight vs Loose Coupling (AVOID)
- ❌ `new EmailService()` inside a class (tightly coupled)
- ✅ `EmailService` injected via constructor (loosely coupled)
- ❌ `order.getCustomer().getAddress().getCity()` (deep coupling)
- ✅ `order.getShippingCity()` (facade method, decoupled)

## DRY (Don't Repeat Yourself)
- Extract duplicate logic into shared methods/classes
- BUT: Don't over-abstract; wait for "rule of three" (same pattern appears 3x)
- Shared utilities should be in `@Service` or `@Component` classes, not static helpers

## KISS (Keep It Simple, Stupid)
- Simple solutions beat clever ones
- Don't add abstractions "just in case" they're needed
- YAGNI: You Ain't Gonna Need It - only implement what's required now

## Separation of Concerns (SoC)
- UI, business logic, and data access must be in separate layers
- Controllers handle HTTP only; Services handle business rules; Repositories handle data
- Never mix concerns (e.g., don't put SQL in a service, don't put business logic in a controller)
