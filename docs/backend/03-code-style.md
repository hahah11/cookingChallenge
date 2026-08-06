# Backend Code Style Preferences

## Java (Spring Boot)

### Naming Conventions
- Follow Oracle naming conventions: camelCase for methods/variables, PascalCase for classes/interfaces
- Use meaningful names: verbs for methods (`calculateTotal()`), nouns for classes (`CustomerService`)
- Constants: UPPER_SNAKE_CASE (`const MAX_RETRY_COUNT = 3`)
- Test methods: `should_doX_when_Y()` or `given_X_when_Y_then_Z()`
- Layer-boundary suffixes disambiguate a class from its domain counterpart of the same
  simple name, mirroring each other: JPA entities are `XxxJpaEntity`
  (`infrastructure/persistence`), generated OpenAPI request/response models are
  `XxxRestDto` (`shared.web.openapi.model`, via the `modelNameSuffix` openapi-generator
  option in `build.gradle.kts` — every generated model gets it, not just the ones that
  collide with a domain name, so the convention stays uniform). Both let the domain class
  itself (`Challenge`, `Account`, ...) keep the unqualified, undecorated name and be
  imported normally everywhere; only the adapter-layer type carries the suffix.

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

### Mapper Usage (MapStruct)
- Use **MapStruct** for every `infrastructure/persistence` mapper (`CustomerMapper`,
  `AccountMapper`, `ChallengeMapper`, ...) instead of hand-written `toDomain`/`toEntity`
  methods — less boilerplate, compile-time-checked field mapping, and one consistent
  pattern across modules.
- Declare the mapper as a plain interface annotated `@Mapper(componentModel = "spring")`
  in `infrastructure/persistence/mapper`; MapStruct generates the `Impl` class as a Spring
  bean at compile time (`org.mapstruct:mapstruct` + `org.mapstruct:mapstruct-processor` on
  the annotation processor path).
- Typed IDs, VOs, and enums (`AccountId`, `Email`, `SystemRole`) generally map 1:1 by
  field name — let MapStruct infer those. Only add an explicit `@Mapping`/default method
  when the shapes genuinely diverge (e.g. `Challenge`'s `List<CookAssignment>` vs. the
  JPA entity's separate `cookAAccountId`/`cookBAccountId` columns) — those cases need a
  hand-written `default` method on the mapper interface, not a switch to fully manual
  mapping for the whole class.
- Still respect the domain's factory methods: a MapStruct `toDomain` cannot call a
  private constructor, so target the aggregate's `reconstitute(...)` factory (via
  `@ObjectFactory` or a `default` method that delegates to it) rather than letting
  MapStruct attempt field injection into the domain class.
- Mappers live in the `mapper` package **of the layer they map for** — persistence mappers in
  `infrastructure/<adapter>/mapper`, domain → OpenAPI-model mappers in `application/mapper`.
  A mapper never lives in `service`, `port`, or next to the entity/aggregate it converts.
- **One mapper per mapped concept.** Every ValueObject, (non-root) Entity, and `@Embeddable`
  that appears as a sub-object inside an aggregate gets its own dedicated
  `@Mapper(componentModel = "spring")` interface — even simple single-field wrappers like a
  typed ID (`PlateColorId ↔ Long`). Don't inline its conversion as a helper method on the
  aggregate mapper; that's how the same logic ends up duplicated across multiple aggregate
  mappers (e.g. `PlateColorId` conversion existing independently inside both `ChallengeMapper`
  and `PlateColorMapper` instead of being shared from one place).
- **The aggregate root's mapper is a plain constructor-injected `@Component`, not a
  MapStruct `@Mapper`.** This is the one exception to "every mapper is MapStruct": MapStruct
  does not forward a hand-declared constructor to its generated `Impl` subclass (it only
  wires dependencies declared via `@Mapper(uses = {...})`, and those are only reachable from
  MapStruct's own auto-generated mapping code — never from a hand-written `default` method,
  since that method is compiled against the abstract class alone, before the `Impl` subclass
  exists). Since every aggregate mapper here is already 100% hand-written (reconstituting a
  private-constructor aggregate, flattening/nesting fields that don't line up 1:1 with the
  entity), there's no MapStruct-generated mapping code for `uses` to help with anyway — so
  skip `@Mapper` for the aggregate's own mapper and write it exactly like any other Spring
  bean in this codebase: `@Component` + Lombok `@RequiredArgsConstructor`, with the
  sub-mappers as constructor-injected fields it calls directly. Sub-object mappers
  (VO/Entity/Embeddable mappers) keep `@Mapper(componentModel = "spring")` as plain
  interfaces, since they have no dependencies of their own to inject.

  ```java
  @Mapper(componentModel = "spring")
  public interface PlateColorIdMapper {
      default PlateColorId toDomain(Long raw) {
          return raw == null ? null : new PlateColorId(raw);
      }

      default Long toRaw(PlateColorId id) {
          return id == null ? null : id.value();
      }
  }

  @Component
  @RequiredArgsConstructor
  public class ChallengeMapper {

      private final PlateColorIdMapper plateColorIdMapper;

      public Challenge toDomain(ChallengeJpaEntity entity) {
          // ...hand-written reconstitute logic, calling
          // plateColorIdMapper.toDomain(entity.getCookAColorId()) instead of a
          // private static helper duplicated per aggregate mapper
      }

      public ChallengeJpaEntity toEntity(Challenge challenge) {
          // ...
      }
  }
  ```
- This also covers domain → generated-OpenAPI-model mappers in the **application** layer
  (e.g. `auth.application.mapper.AccountModelMapper`, domain `Account` → the generated
  `AccountRestDto` model) — same `@Mapper(componentModel = "spring")` interface,
  hand-written `default` methods where a typed VO or enum needs explicit conversion, same
  reasoning as above. Not just an `infrastructure/persistence` rule despite the section
  title. Where the generated model needs cross-aggregate computed fields MapStruct can't
  derive from the domain aggregate alone (e.g. `cookoff.application.mapper.ChallengeModelMapper`),
  fall back to plain static helper methods instead of a `@Mapper` interface — see that
  class's javadoc.

```java
@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "id", source = "id", qualifiedByName = "toCustomerId")
    Customer toDomain(CustomerJpaEntity entity);

    @InheritInverseConfiguration
    CustomerJpaEntity toEntity(Customer customer);
}
```

### Data Type Placement
- Records/DTOs never live in `service` or `port`. App-layer types (commands, query results,
  port payloads) go in `application/dto`; domain types (aggregates, VOs, enums, domain
  records) go in `domain/model`; JPA entities and `@Embeddable`s go in
  `infrastructure/<adapter>/entity`.
- The single exemption: a use-case-private nested record (e.g. `SubmitScoreService.Result`)
  may stay nested inside its service, because it is part of that use case's signature and has
  no independent identity outside it.

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
src/test/java/com/cookingchallenge/
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

## ID Generation (TSID)

- Use **TSID** (time-sorted unique identifier, Twitter-Snowflake-style) for every
  aggregate root ID — one consistent strategy, no special-casing "internal" vs.
  "public-facing" aggregates.
- Generate with [`tsid-creator`](https://github.com/f4b6a3/tsid-creator); store as
  `BIGINT` in Postgres (k-sortable, index-friendly, unlike random UUIDv4).
- Encode as a Crockford Base32 string at the API/URL boundary (e.g.
  `"0S4G9FVCC9CPP"`) — compact, URL-safe, and not a bare sequential integer, so IDs
  exposed in links aren't trivially enumerable.
- Wrap the raw TSID in a typed ID value object per aggregate (`CustomerId`, not a bare
  `Long`), with `toString()`/`fromString()` handling the Base32 encoding at the
  boundary.

```java
// Domain value object
public record CustomerId(long value) {
    public static CustomerId generate() {
        return new CustomerId(TsidCreator.getTsid().toLong());
    }

    public static CustomerId fromString(String base32) {
        return new CustomerId(Tsid.from(base32).toLong());
    }

    @Override
    public String toString() {
        return Tsid.from(value).toString(); // Crockford Base32
    }
}

// JPA entity column
@Entity
public class CustomerJpaEntity {
    @Id
    private Long id; // raw TSID long, mapped to/from CustomerId at the boundary
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

See [`docs/backend/02-ddd-modulith.md#module-structure-ddd-bounded-context--spring-modulith-module`](02-ddd-modulith.md#module-structure-ddd-bounded-context--spring-modulith-module)
for the authoritative module layout. Summary:

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