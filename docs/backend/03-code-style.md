# Backend Code Style Preferences

## Java (Spring Boot)

### Naming Conventions
- Follow Oracle naming conventions: camelCase for methods/variables, PascalCase for classes/interfaces
- Use meaningful names: verbs for methods (`calculateTotal()`), nouns for classes (`CustomerService`)
- Constants: UPPER_SNAKE_CASE (`const MAX_RETRY_COUNT = 3`)
- Test methods: `should_doX_when_Y()` or `given_X_when_Y_then_Z()`

### Code Structure
- Add Javadoc for public APIs (controllers, service interfaces)
- Keep methods under 50 lines; extract helper methods for complex logic
- Use `var` for local variables when type is obvious, explicit types otherwise
- Prefer constructor injection over field injection (avoid `@Autowired` on fields)

### Lombok Usage
Use Lombok extensively to reduce boilerplate:
- `@Getter/@Setter` - Generate getters/setters
- `@Builder` - Builder pattern for complex objects
- `@NoArgsConstructor/@AllArgsConstructor` - Constructors
- `@RequiredArgsConstructor` - Constructor with final fields
- `@Slf4j` - Logging field
- `@Value` - Immutable classes (like record but with annotations)

```java
@Value
@Builder
public class Customer {
    Long id;
    String name;
    Email email;
}
```

### Records vs Classes
- Use **records** for DTOs and value objects (immutable, data-only)
- Use **classes** for entities and domain objects with behavior

```java
// DTO - use record
public record CreateCustomerRequest(String name, String email) {}

// Domain entity - use class
public class Customer {
    private CustomerId id;
    private Email email;
    
    public void placeOrder(Money amount) { ... }
}
```

### Exception Handling
- Use exceptions, not return codes
- Catch specific exceptions, not `Exception`
- Create domain-specific exceptions
- Use `@ControllerAdvice` for global error handling

```java
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) {
        super("Customer not found: " + id);
    }
}

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
}
```

## Testing (Backend)

### Unit Tests
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests
- Mock dependencies with `@Mock`
- Inject mocks with `@InjectMocks`

### Integration Tests
- Use `@SpringBootTest` for full context tests
- Use `@DataJpaTest` for repository tests
- Use `@WebMvcTest` for controller tests
- Mark integration tests with `@Transactional` for automatic rollback

### Test Organization
```
src/test/java/com/impepi/
├── unit/
│   ├── service/
│   │   └── CustomerServiceTest.java
│   └── domain/
│       └── CustomerTest.java
└── integration/
    ├── repository/
    │   └── CustomerRepositoryIntegrationTest.java
    └── controller/
        └── CustomerControllerIntegrationTest.java
```

## Logging

- Use SLF4J with `@Slf4j`
- Log at appropriate levels:
  - `DEBUG` - Detailed debugging information
  - `INFO` - Important application events (user actions, system events)
  - `WARN` - Potential issues that don't stop execution
  - `ERROR` - Errors that affect functionality
- Include context in logs (customer ID, order ID, etc.)
- Never log sensitive data (passwords, tokens, PII)

```java
@Slf4j
@Service
public class CustomerService {
    public Customer create(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());
        // ...
        log.info("Customer created with ID: {}", customer.getId());
    }
}
```

## Database Patterns (JPA/Hibernate)

### Entity Design
- Use `@Entity` only in infrastructure layer
- Keep entities simple - avoid business logic in entities if they're JPA entities
- Use separate domain entities for business logic

```java
// Domain entity (no JPA annotations)
public class Customer {
    private CustomerId id;
    private Email email;
}

// JPA entity (infrastructure only)
@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    @Id
    private Long id;
    private String email;
    
    // Map to/from domain entity
}
```

### Repository Patterns
- Use Spring Data JPA for simple queries
- Use custom repository implementation for complex queries
- Never put business logic in repositories

```java
// Simple repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(Email email);
    List<Customer> findByStatus(CustomerStatus status);
}

// Custom query with @Query
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :since")
    List<Customer> findCreatedAfter(@Param("since") LocalDateTime since);
}
```

### Transaction Management
- Use `@Transactional` at service layer, not repository or controller
- Keep transactions short
- Read-only queries: `@Transactional(readOnly = true)`

```java
@Service
public class CustomerService {
    @Transactional
    public Customer create(CreateCustomerRequest request) {
        // ...
    }
    
    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        // ...
    }
}
```

## Code Organization

### File Naming
- Classes: PascalCase (`CustomerService`)
- Files: Match class name (`CustomerService.java`)
- Test files: `ClassNameTest.java` or `ClassNameTests.java`

### Package Structure
```
com.impepi.customer/
├── domain/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── event/
├── application/
│   ├── service/
│   ├── dto/
│   └── port/
├── infrastructure/
│   ├── persistence/
│   ├── notification/
│   └── config/
└── interface/
    ├── rest/
    └── event/
```

## Performance Considerations

- Avoid N+1 queries (use `@EntityGraph` or JOIN FETCH)
- Use pagination for large datasets
- Index frequently queried columns
- Use `@Transactional(readOnly = true)` for read operations
- Cache frequently accessed, rarely changed data

```java
// Avoid N+1
@Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders")
List<Customer> findAllWithOrders();
```