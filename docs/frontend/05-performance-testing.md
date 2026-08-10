# Angular Performance & Testing

## Performance Optimization

### Change Detection

```typescript
// ✅ Always use OnPush
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ...
})
export class CustomerCardComponent {
  @Input() customer!: Customer;
}

// ✅ Use immutable updates to trigger change detection
@Component({ ... })
export class CustomerListComponent {
  customers = signal<Customer[]>([]);

  // ✅ Good - creates new reference
  addCustomer(customer: Customer) {
    this.customers.update(list => [...list, customer]);
  }

  // ❌ Bad - mutation doesn't trigger change detection
  addCustomerBad(customer: Customer) {
    this.customers().push(customer);
  }
}
```

### TrackBy / @for Tracking

```html
<!-- ✅ Use track by id (Angular 17+) -->
@for (customer of customers(); track customer.id) {
  <app-customer-card [customer]="customer" />
}

<!-- ✅ Old syntax with trackBy -->
@for (customer of customers; track customer.id; let i = index) {
  <app-customer-card [customer]="customer" />
}
```

### Lazy Loading

```typescript
// ✅ Lazy load heavy components
@Component({ ... })
export class DashboardComponent {
  ngOnInit() {
    // Load chart library only when needed
    import('chart.js').then(chart => {
      this.initChart(chart);
    });
  }
}

// ✅ Lazy load routes
{
  path: 'reports',
  loadChildren: () => import('./reports/reports.routes')
    .then(m => m.REPORTS_ROUTES)
}
```

### Debouncing User Input

```typescript
@Component({ ... })
export class SearchComponent {
  searchTerms = new Subject<string>();

  ngOnInit() {
    this.searchTerms.pipe(
      debounceTime(300),        // Wait 300ms after keystroke
      distinctUntilChanged(),   // Ignore if same value
      switchMap(term => this.searchService.search(term))
    ).subscribe(results => {
      this.results = results;
    });
  }

  onSearch(term: string) {
    this.searchTerms.next(term);
  }
}
```

### Virtual Scrolling (CDK)

```typescript
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

@Component({
  template: `
    <cdk-virtual-scroll-viewport itemSize="50" class="viewport">
      @for (customer of customers(); track customer.id) {
        <app-customer-card [customer]="customer" />
      }
    </cdk-virtual-scroll-viewport>
  `
})
export class LargeListComponent {
  customers = signal<Customer[]>([]);
}
```

### Memoization with Computed

```typescript
@Component({ ... })
export class CustomerListComponent {
  customers = signal<Customer[]>([]);
  filter = signal('');

  // ✅ Computed - only recalculates when dependencies change
  filteredCustomers = computed(() => {
    const filter = this.filter().toLowerCase();
    return this.customers().filter(c =>
      c.name.toLowerCase().includes(filter)
    );
  });
}
```

### Optimize HTTP Calls

```typescript
// ✅ Use shareReplay to cache HTTP responses
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private customersCache = new BehaviorSubject<Customer[]>([]);
  
  getCustomers(): Observable<Customer[]> {
    if (this.customersCache.value.length > 0) {
      return this.customersCache.asObservable();
    }

    return this.http.get<Customer[]>('/api/customers').pipe(
      tap(customers => this.customersCache.next(customers)),
      shareReplay(1)
    );
  }
}
```

## Component Testing

### Basic Component Test

```typescript
describe('CustomerCardComponent', () => {
  let component: CustomerCardComponent;
  let fixture: ComponentFixture<CustomerCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerCardComponent, CommonModule]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display customer name', () => {
    component.customer = { id: 1, name: 'John Doe', email: 'john@example.com' };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.name').textContent).toContain('John Doe');
  });

  it('should emit deleted event when delete button clicked', () => {
    const spy = vi.spyOn(component.deleted, 'emit');
    component.customer = { id: 1, name: 'John' };
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.delete-btn').click();

    expect(spy).toHaveBeenCalled();
  });
});
```

### Component with Dependencies

Mock services as plain objects with `vi.fn()` methods passed via `useValue` — no spy-object helper needed:

```typescript
describe('CustomerListComponent', () => {
  function setup(customerService: Partial<CustomerService>) {
    TestBed.configureTestingModule({
      imports: [CustomerListComponent],
      providers: [{ provide: CustomerService, useValue: customerService }]
    });

    const fixture = TestBed.createComponent(CustomerListComponent);
    return { fixture };
  }

  it('should load customers on init', () => {
    const mockCustomers: Customer[] = [{ id: 1, name: 'John' }];
    const getCustomers = vi.fn().mockReturnValue(of(mockCustomers));
    const { fixture } = setup({ getCustomers });

    fixture.detectChanges();

    expect(getCustomers).toHaveBeenCalled();
    expect(fixture.componentInstance.customers().length).toBe(1);
  });

  it('should delete customer when delete button clicked', () => {
    const customer: Customer = { id: 1, name: 'John' };
    const deleteCustomer = vi.fn().mockReturnValue(of(undefined));
    const { fixture } = setup({ deleteCustomer });
    fixture.componentInstance.customers.set([customer]);

    fixture.componentInstance.deleteCustomer(1);

    expect(deleteCustomer).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.customers().length).toBe(0);
  });
});
```

## Service Testing

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

  afterEach(() => {
    httpMock.verify();  // Ensure no outstanding requests
  });

  it('should fetch customers', () => {
    const mockCustomers: Customer[] = [{ id: 1, name: 'John' }];

    service.getCustomers().subscribe(customers => {
      expect(customers).toEqual(mockCustomers);
    });

    const req = httpMock.expectOne('/api/v1/customers');
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomers);
  });

  it('should create customer', () => {
    const newCustomer: CreateCustomerRequest = { name: 'John', email: 'john@example.com' };
    const createdCustomer: Customer = { id: 1, ...newCustomer };

    service.createCustomer(newCustomer).subscribe(customer => {
      expect(customer.id).toBe(1);
    });

    const req = httpMock.expectOne('/api/v1/customers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newCustomer);
    req.flush(createdCustomer);
  });

  it('should handle error', () => {
    service.getCustomers().subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/api/v1/customers');
    req.flush('Server Error', { status: 500, statusText: 'Server Error' });
  });
});
```

## Guard Testing

Guards are plain functions — run them inside `TestBed.runInInjectionContext` and mock dependencies as `useValue` objects, same as any other unit under test:

```typescript
describe('authGuard', () => {
  function runGuard(authenticated: boolean) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { isAuthenticated: signal(authenticated) } }]
    });
    return TestBed.runInInjectionContext(() => authGuard(null as any, null as any));
  }

  it('should activate when user is authenticated', () => {
    expect(runGuard(true)).toBe(true);
  });

  it('should redirect to login when user is not authenticated', () => {
    const result = runGuard(false) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login');
  });
});
```

## Performance Testing Best Practices

### Avoid Memory Leaks

```typescript
// ✅ Use takeUntilDestroyed (Angular 16+)
@Component({ ... })
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];

  constructor(private customerService: CustomerService) {}

  ngOnInit() {
    this.customerService.getCustomers()
      .pipe(takeUntilDestroyed())
      .subscribe(customers => {
        this.customers = customers;
      });
  }
}

// ✅ Or use async pipe in template
@Component({
  template: `<ng-container *ngFor="let c of customers$ | async"></ng-container>`
})
export class CustomerListComponent {
  customers$ = this.customerService.getCustomers();
}
```

### Optimize Bundle Size

```typescript
// ✅ Lazy load heavy libraries
async export function downloadReport() {
  const pdfLib = await import('pdf-lib');  // Only loaded when needed
  // Use pdfLib...
}

// ✅ Use environment-specific imports
const chartModule = import(environment.production ? './chart.prod.js' : './chart.dev.js');
```

### Measure Performance

```typescript
// ✅ Use Angular DevTools for profiling
// ✅ Use Performance API
ngAfterViewInit() {
  const observer = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      console.log(entry.name, entry.startTime, entry.duration);
    }
  });
  observer.observe({ entryTypes: ['measure'] });
}
```

## Testing Utilities

### Test Factories

```typescript
// ✅ Create reusable test data factories
function createCustomerFactory(overrides?: Partial<Customer>): Customer {
  return {
    id: 1,
    name: 'John Doe',
    email: 'john@example.com',
    status: 'ACTIVE',
    ...overrides
  };
}

function createCustomersFactory(count: number): Customer[] {
  return Array.from({ length: count }, (_, i) =>
    createCustomerFactory({ id: i + 1, name: `Customer ${i + 1}` })
  );
}

// Usage in tests
it('should display 5 customers', () => {
  const customers = createCustomersFactory(5);
  component.customers.set(customers);
  fixture.detectChanges();

  expect(component.customers().length).toBe(5);
});
```

### Mock Providers

```typescript
// ✅ Create reusable mock providers
export const mockCustomerService: Provider = {
  provide: CustomerService,
  useValue: { getCustomers: vi.fn(), createCustomer: vi.fn() }
};

// Usage
TestBed.configureTestingModule({
  imports: [CustomerListComponent],
  providers: [mockCustomerService]
});
```

## Best Practices Summary

### Performance DO
- ✅ Use OnPush change detection
- ✅ Use track by in @for loops
- ✅ Lazy load routes and heavy components
- ✅ Debounce user input
- ✅ Use computed signals for derived state
- ✅ Cache HTTP responses when appropriate

### Performance DON'T
- ❌ Use Default change detection unnecessarily
- ❌ Mutate arrays/objects directly
- ❌ Subscribe without unsubscribing
- ❌ Load all libraries upfront
- ❌ Make unnecessary HTTP calls

### Testing DO
- ✅ Test behavior, not implementation
- ✅ Use test factories for data
- ✅ Mock external dependencies
- ✅ Verify HTTP calls with HttpTestingController
- ✅ Test error cases

### Testing DON'T
- ❌ Test getters/setters
- ❌ Test framework behavior
- ❌ Use real services in unit tests
- ❌ Forget to verify outstanding HTTP requests
