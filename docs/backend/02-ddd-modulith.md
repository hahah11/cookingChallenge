# Clean Architecture + DDD + Spring Modulith

## How They Work Together

| Approach | Role | Focus |
|----------|------|-------|
| **Clean Architecture** | Structural framework | Layering and dependency flow |
| **DDD** | Domain modeling | Business language and boundaries |
| **Spring Modulith** | Runtime organization | Module isolation and communication |

**Core Principle**: Each Spring Modulith module is a self-contained Clean Architecture unit with DDD patterns.

## Module Structure (DDD Bounded Context = Spring Modulith Module)

```
com.cookingchallenge.customer/
├── domain/                    # Inner circle - Entities + DDD
│   ├── model/
│   │   ├── Customer.java      # Aggregate Root
│   │   ├── CustomerId.java    # Value Object
│   │   └── Email.java         # Value Object
│   ├── repository/
│   │   └── CustomerRepository.java  # Domain Interface (port)
│   ├── service/
│   │   └── CustomerPricingService.java  # Domain Service
│   └── event/
│       └── CustomerCreatedEvent.java    # Domain Events
│
├── application/               # Use Cases - DDD Application Layer
│   ├── service/
│   │   ├── CreateCustomerService.java   # Application Service
│   │   └── UpdateCustomerService.java
│   ├── dto/
│   │   ├── CreateCustomerCommand.java   # CQRS Command
│   │   └── CustomerView.java            # CQRS Query Result
│   └── port/
│       └── NotificationPort.java        # Outgoing port (interface)
│
├── infrastructure/            # Outer circle - Frameworks & Drivers
│   ├── persistence/
│   │   ├── CustomerJpaEntity.java       # DB mapping
│   │   ├── CustomerRepositoryImpl.java  # Adapter (implements domain repo)
│   │   └── CustomerMapper.java          # Domain ↔ Entity conversion (MapStruct interface)
│   ├── notification/
│   │   └── EmailNotificationAdapter.java # Implements NotificationPort
│   └── config/
│       └── CustomerModuleConfig.java
│
└── interface/                 # Interface Adapters - REST/Events
    ├── rest/
    │   ├── CustomerController.java
    │   └── CustomerDto.java
    └── event/
        └── CustomerEventHandler.java    # Listen to other modules
```

## DDD Domain Entities (Rich Domain Model)

```java
// Domain layer knows NOTHING about JPA, Spring, or DB
public class Customer {
    private CustomerId id;
    private Email email;
    private CustomerStatus status;
    private List<Order> orders;
    
    // Business behavior - NO getters/setters for state
    public void placeOrder(Money amount) {
        if (this.status == CustomerStatus.BANNED) {
            throw new CustomerBannedException();
        }
        Order order = new Order(this.id, amount);
        this.orders.add(order);
        this.registerEvent(new OrderPlacedEvent(order.getId()));
    }
    
    public boolean canReceiveDiscount() {
        return this.status == CustomerStatus.PREMIUM;
    }
    
    // No @Entity, no @Autowired, no framework annotations
}
```

## Repository Pattern (Ports and Adapters)

```java
// Domain interface (port) - in domain package
public interface CustomerRepository {
    Optional<Customer> findById(CustomerId id);
    Customer save(Customer customer);
    boolean existsByEmail(Email email);
}

// Infrastructure adapter - in infrastructure package
// CustomerMapper is a MapStruct interface (see docs/backend/03-code-style.md#mapper-usage-mapstruct);
// Spring injects the generated Impl like any other bean.
@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {
    private final CustomerJpaRepository jpaRepository;
    private final CustomerMapper mapper;
    
    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaRepository.findById(id.getValue()).map(mapper::toDomain);
    }
    
    @Override
    public Customer save(Customer customer) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(customer)));
    }
}
```

## Application Services (Use Cases)

```java
@Service
@RequiredArgsConstructor
public class CreateCustomerService {
    private final CustomerRepository customerRepository;
    private final NotificationPort notificationService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public CustomerView execute(CreateCustomerCommand command) {
        if (customerRepository.existsByEmail(command.email())) {
            throw new CustomerAlreadyExistsException();
        }
        
        Customer customer = Customer.create(command.name(), command.email());
        customerRepository.save(customer);
        notificationService.sendWelcomeEmail(customer.getEmail());
        eventPublisher.publishEvent(new CustomerCreatedEvent(customer.getId()));
        
        return CustomerView.from(customer);
    }
}
```

## Module Communication (Spring Modulith)

```java
// Event published in one module
@Component
@RequiredArgsConstructor
public class CustomerEventListener {
    private final ApplicationEventPublisher publisher;
    
    @EventListener
    public void onCustomerCreated(CustomerCreatedEvent event) {
        publisher.publishEvent(new CustomerRegisteredForOrderModule(event.customerId()));
    }
}

// Other module listens
@Component
@RequiredArgsConstructor
public class OrderModuleCustomerListener {
    @EventListener
    public void handleCustomerRegistered(CustomerRegisteredForOrderModule event) {
        customerCache.register(event.customerId());
    }
}
```

## Module Contracts (Interfaces Between Modules)

```java
// Define what this module exposes to others
public class CustomerModule {
    public interface CustomerLookup {
        Optional<CustomerView> findById(CustomerId id);
    }
    
    @Component
    public class CustomerLookupImpl implements CustomerLookup {
        // ...
    }
}

// Other modules depend on the interface
@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomerModule.CustomerLookup customerLookup;
}
```

## Key Integration Rules

### 1. Dependency Direction
- Domain layer: NO dependencies on Spring, JPA, or infrastructure
- Application layer: Depends on domain interfaces only
- Infrastructure layer: Implements domain interfaces (adapters)
- Interface layer: Calls application services

### 2. Module Boundaries
- Each module = one bounded context
- Modules communicate ONLY via events or interfaces
- NO direct database access across modules
- NO direct entity access across modules

## Anti-Patterns to Avoid

### ❌ Leaky Abstractions Across Modules
```java
// BAD: Order module directly accesses Customer JPA entity
@Entity
public class Order {
    @ManyToOne
    private CustomerJpaEntity customer;  // Wrong!
}

// GOOD: Order module uses Customer ID only
public class Order {
    private CustomerId customerId;  // Correct
}
```

### ❌ Anemic Domain Model
```java
// BAD: All logic in service
@Entity
public class Customer { /* only getters/setters */ }

// GOOD: Rich domain model
public class Customer {
    public boolean canPlaceOrder() {
        return status == CustomerStatus.ACTIVE;
    }
}
```

### ❌ Infrastructure Leakage into Domain
```java
// BAD: Domain entity with JPA annotations
@Entity
public class Customer { @Id private Long id; }

// GOOD: Domain entity is pure Java
public class Customer { private CustomerId id; }

// JPA entity is separate, mapped in infrastructure
@Entity
public class CustomerJpaEntity { @Id private Long id; }
```

## When to Create a New Module

**Create when:**
1. Different Bounded Context (different business language/rules)
2. Independent deployment needed
3. Different team owns this functionality
4. Needs its own database/schema
5. Has distinct aggregates and invariants

**Don't create when:**
1. Same context - just a different feature
2. Thin wrapper - only adds logging/validation
3. Needs to access another module's entities directly
4. Simple CRUD that doesn't need isolation
