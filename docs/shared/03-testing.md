# Testing Guidelines

## General Principles

- Write tests for every new feature (unit tests + integration tests where applicable)
- Test code is production code - keep it clean and maintainable
- One assertion per test; name tests to reflect the assertion
- Use AAA pattern: Arrange, Act, Assert

## F.I.R.S.T. Principles

- **F**ast: Tests should run quickly (unit tests < 100ms each)
- **I**ndependent: Tests don't rely on each other's state
- **R**epeatable: Same result everywhere (no network, time, random dependencies)
- **S**elf-validating: Pass/fail without manual inspection
- **T**imely: Write tests before or with production code

## Test Naming Conventions

### Java (JUnit 5)
- Use descriptive method names: `should_throwException_when_invalidInput()`
- For BDD style: `given_X_when_Y_then_Z()`
- Test class name: `ClassNameTest` or `ClassNameTests`

### TypeScript/Jasmine
- Use describe/it blocks with clear sentences:
  ```typescript
  it('should return customers when filter is empty', () => { ... });
  it('should throw error when customer not found', () => { ... });
  ```

## Test Coverage Guidelines

- Aim for meaningful coverage, not 100% coverage
- Critical business logic: 90%+ coverage
- Controllers/Repositories: 70%+ coverage
- DTOs/Entities: No tests needed (trivial code)
- Focus on behavior, not implementation

## Test Data Management

- Use test factories for complex objects:
  ```java
  class CustomerFactory {
      static Customer createCustomer() { ... }
      static Customer createPremiumCustomer() { ... }
  }
  ```
- Avoid shared mutable test state
- Clean up test data after each test (use `@Transactional` where possible)

## Backend Testing (Spring Boot)

### Unit Tests
- Mock services and repositories using Mockito
- Test domain logic in isolation
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CreateCustomerService createCustomerService;
    
    @Test
    void should_create_customer_when_email_not_exists() {
        // Arrange
        when(customerRepository.existsByEmail(any(Email.class))).thenReturn(false);
        CreateCustomerCommand command = new CreateCustomerCommand("John", "john@example.com");
        
        // Act
        CustomerView result = createCustomerService.execute(command);
        
        // Assert
        assertThat(result.name()).isEqualTo("John");
        verify(customerRepository).save(any(Customer.class));
    }
}
```

### Integration Tests
- Use `@SpringBootTest` for full context tests
- Test repository implementations with real database
- Use `@DataJpaTest` for repository-focused tests

```java
@SpringBootTest
class CustomerRepositoryIntegrationTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @Transactional
    void should_save_and_find_customer() {
        // Arrange
        Customer customer = Customer.create("John", "john@example.com");
        
        // Act
        Customer saved = customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findById(saved.getId());
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }
}
```

## Frontend Testing (Angular)

### Service Tests
- Use TestBed to configure testing module
- Test services in isolation
- Mock HTTP calls with `HttpClientTestingModule`

```typescript
describe('CustomerService', () => {
  let service: CustomerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CustomerService]
    });
    service = TestBed.inject(CustomerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should return customers when filter is empty', () => {
    const mockCustomers: Customer[] = [{ id: 1, name: 'John' }];

    service.getCustomers('').subscribe(customers => {
      expect(customers).toEqual(mockCustomers);
    });

    const req = httpMock.expectOne('/api/customers');
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomers);
  });
});
```

### Component Tests
- Use TestBed for component testing
- Test user interactions and data binding
- Mock services and inputs

```typescript
describe('CustomerListComponent', () => {
  let component: CustomerListComponent;
  let fixture: ComponentFixture<CustomerListComponent>;
  let customerServiceSpy: jasmine.SpyObj<CustomerService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('CustomerService', ['getCustomers']);

    await TestBed.configureTestingModule({
      imports: [CustomerListComponent],
      providers: [{ provide: CustomerService, useValue: spy }]
    }).compileComponents();

    customerServiceSpy = TestBed.inject(CustomerService) as jasmine.SpyObj<CustomerService>;
    fixture = TestBed.createComponent(CustomerListComponent);
    component = fixture.componentInstance;
  });

  it('should display customers when loaded', () => {
    customerServiceSpy.getCustomers.and.returnValue(of([{ id: 1, name: 'John' }]));

    fixture.detectChanges();

    expect(customerServiceSpy.getCustomers).toHaveBeenCalled();
    expect(component.customers.length).toBe(1);
  });
});
```
