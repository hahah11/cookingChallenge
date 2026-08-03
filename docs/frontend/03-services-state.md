# Angular Services & State Management

## Service Design Patterns

### Service Types

```typescript
// ✅ Stateless utility service
@Injectable({ providedIn: 'root' })
export class DateUtilsService {
  formatDate(date: Date): string {
    return date.toLocaleDateString();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  }
}

// ✅ Stateful business service
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private customers = new BehaviorSubject<Customer[]>([]);

  get customers$(): Observable<Customer[]> {
    return this.customers.asObservable();
  }

  loadCustomers() {
    this.http.get<Customer[]>('/api/customers').subscribe(
      customers => this.customers.next(customers)
    );
  }
}

// ✅ HTTP API service
@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly baseUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  getAll(page: number, size: number): Observable<Page<Customer>> {
    return this.http.get<Page<Customer>>(this.baseUrl, {
      params: { page, size }
    });
  }

  getById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  create(customer: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, customer);
  }

  update(id: number, customer: UpdateCustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, customer);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

### Config Service

Roles, permissions, and feature flags are fetched once at app bootstrap — not per page — from `GET /api/v1/config` (see [`docs/shared/04-api-design.md#configuration-endpoint`](../shared/04-api-design.md#configuration-endpoint)).

```typescript
// ✅ core/services/config.service.ts
@Injectable({ providedIn: 'root' })
export class ConfigService {
  private config = signal<AppConfig | null>(null);

  readonly roles = computed(() => this.config()?.roles ?? []);
  readonly featureFlags = computed(() => this.config()?.featureFlags ?? {});

  constructor(private http: HttpClient) {}

  load(): Observable<AppConfig> {
    return this.http.get<AppConfig>('/api/v1/config').pipe(
      tap(config => this.config.set(config))
    );
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }
}

// ✅ app.config.ts — load before the app renders
export const appConfig: ApplicationConfig = {
  providers: [
    provideAppInitializer(() => inject(ConfigService).load())
  ]
};
```

Guards and components call `configService.hasRole(...)` — never hardcode role checks against raw API responses or infer permissions locally.

### Service Methods Best Practices

```typescript
// ✅ Return Observables/Promises, don't subscribe internally
@Injectable({ providedIn: 'root' })
export class CustomerService {
  // ✅ Good - returns observable for caller to subscribe
  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>('/api/customers');
  }

  // ❌ Bad - subscribes internally (memory leak, breaks reactivity)
  getCustomers(): void {
    this.http.get<Customer[]>('/api/customers').subscribe(
      data => this.customers = data  // Don't do this!
    );
  }
}
```

## State Management with Signals (Angular 16+)

### Basic Signal Usage

```typescript
@Component({
  standalone: true,
  selector: 'app-customer-list',
  template: `
    @for (customer of customers(); track customer.id) {
      <app-customer-card [customer]="customer" />
    }
  `
})
export class CustomerListComponent {
  // ✅ Read-only signal for template
  customers = signal<Customer[]>([]);

  // ✅ Computed signal
  customerCount = computed(() => this.customers().length);

  // ✅ Effect for side effects
  constructor(private notificationService: NotificationService) {
    effect(() => {
      const count = this.customerCount();
      if (count > 0) {
        this.notificationService.info(`Showing ${count} customers`);
      }
    });
  }

  // ✅ Update with functional update
  addCustomer(customer: Customer) {
    this.customers.update(customers => [...customers, customer]);
  }

  // ✅ Update with set
  setCustomers(customers: Customer[]) {
    this.customers.set(customers);
  }

  // ✅ Delete
  deleteCustomer(id: number) {
    this.customers.update(customers => customers.filter(c => c.id !== id));
  }
}
```

### Signal Best Practices

```typescript
// ✅ Proper signal organization
@Component({ ... })
export class CustomerListComponent implements OnInit {
  // State signals
  customers = signal<Customer[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  // Computed signals
  hasCustomers = computed(() => this.customers().length > 0);
  errorMessage = computed(() => this.error());

  // Derived state (computed)
  activeCustomers = computed(() =>
    this.customers().filter(c => c.status === 'ACTIVE')
  );

  constructor(private customerService: CustomerService) {}

  ngOnInit() {
    this.loadCustomers();
  }

  // Actions
  loadCustomers() {
    this.isLoading.set(true);
    this.error.set(null);

    this.customerService.getCustomers().subscribe({
      next: customers => {
        this.customers.set(customers);
        this.isLoading.set(false);
      },
      error: err => {
        this.error.set(err.message);
        this.isLoading.set(false);
      }
    });
  }
}
```

## State Management with RxJS

### Observable Patterns

```typescript
@Component({ ... })
export class CustomerListComponent {
  // ✅ Private subject for internal emissions
  private refresh$ = new Subject<void>();

  // ✅ Public observable for template
  customers$: Observable<Customer[]>;

  constructor(private customerService: CustomerService) {
    // ✅ Combine refresh with initial load
    this.customers$ = merge(
      this.customerService.getCustomers(),
      this.refresh$.pipe(
        switchMap(() => this.customerService.getCustomers())
      )
    ).pipe(
      catchError(this.handleError),
      startWith([])
    );
  }

  refresh() {
    this.refresh$.next();
  }

  private handleError(error: HttpErrorResponse) {
    this.notificationService.error('Failed to load customers');
    return of([]);
  }
}
```

### RxJS Operators Best Practices

```typescript
// ✅ Use switchMap for canceling previous requests
search(term: string) {
  return this.searchService.search(term).pipe(
    switchMap(results => this.processResults(results))
  );
}

// ✅ Use debounceTime for search inputs
searchTerms = new Subject<string>();

ngOnInit() {
  this.searchTerms.pipe(
    debounceTime(300),        // Wait 300ms after each keystroke
    distinctUntilChanged(),   // Ignore if next value is same
    switchMap(term => this.searchService.search(term))
  ).subscribe(results => {
    this.results = results;
  });
}

// ✅ Use retry for transient failures
getData() {
  return this.http.get('/api/data').pipe(
    retry(3),  // Retry up to 3 times
    catchError(this.handleError)
  );
}

// ✅ Use shareReplay for multicasting
private customersSubject = new BehaviorSubject<Customer[]>([]);
customers$ = this.customersSubject.asObservable().pipe(
  shareReplay(1)  // Cache last value for new subscribers
);
```

## Avoiding Direct State Mutation

```typescript
// ❌ BAD: Direct mutation
class CustomerStore {
  customers: Customer[] = [];

  addCustomer(customer: Customer) {
    this.customers.push(customer);  // Mutation!
  }

  updateCustomer(id: number, data: Partial<Customer>) {
    const customer = this.customers.find(c => c.id === id);
    Object.assign(customer, data);  // Mutation!
  }
}

// ✅ GOOD: Immutable updates with signals
class CustomerStore {
  private customers = signal<Customer[]>([]);

  customers$ = this.customers.asReadonly();

  addCustomer(customer: Customer) {
    this.customers.update(customers => [...customers, customer]);
  }

  updateCustomer(id: number, data: Partial<Customer>) {
    this.customers.update(customers =>
      customers.map(c => c.id === id ? { ...c, ...data } : c)
    );
  }

  deleteCustomer(id: number) {
    this.customers.update(customers =>
      customers.filter(c => c.id !== id)
    );
  }
}
```

## HTTP Client Patterns

### Request/Response Interceptors

```typescript
// ✅ Auth interceptor - add token to requests
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.accessToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};

// ✅ Error interceptor - handle errors globally
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Handle unauthorized - redirect to login
      } else if (error.status === 403) {
        // Handle forbidden
      } else {
        // Handle other errors
      }
      return throwError(() => error);
    })
  );
};

// ✅ Configure in app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    )
  ]
};
```

### Handling HTTP Responses

```typescript
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly baseUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  // ✅ Handle success and error together
  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.baseUrl).pipe(
      tap(customers => console.log(`Loaded ${customers.length} customers`)),
      catchError(this.handleError)
    );
  }

  // ✅ Handle void response
  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => console.log(`Deleted customer ${id}`)),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    let message = 'An error occurred';

    if (error.error?.message) {
      message = error.error.message;
    }

    this.notificationService.error(message);
    return throwError(() => error);
  }
}
```

## Service Communication Patterns

### Event Bus Pattern

```typescript
// ✅ Shared event service for component communication
@Injectable({ providedIn: 'root' })
export class EventBusService {
  private events = new Subject<AppEvent>();
  events$ = this.events.asObservable();

  emit(event: AppEvent) {
    this.events.next(event);
  }
}

// Usage in component A
this.eventBus.emit({ type: 'CUSTOMER_SELECTED', payload: customer });

// Usage in component B
this.eventBus.events$.subscribe(event => {
  if (event.type === 'CUSTOMER_SELECTED') {
    this.handleCustomerSelected(event.payload);
  }
});
```

### Parent-Child via Service

```typescript
// ✅ Use a shared service for parent-child that's hard to wire
@Injectable({ providedIn: 'root' })
export class CustomerSelectionService {
  private selectedCustomerId = new Subject<number | null>();
  selectedCustomerId$ = this.selectedCustomerId.asObservable();

  selectCustomer(id: number) {
    this.selectedCustomerId.next(id);
  }

  clearSelection() {
    this.selectedCustomerId.next(null);
  }
}
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

## Anti-Patterns to Avoid

```typescript
// ❌ Avoid excessive dependencies
@Component({ ... })
export class BadComponent {
  constructor(
    private customerService: CustomerService,
    private orderService: OrderService,
    private productService: ProductService,
    private userService: UserService,
    private notificationService: NotificationService
  ) {}  // Too many dependencies!
}

// ✅ Extract to focused service
@Component({ ... })
export class GoodComponent {
  constructor(
    private dashboardService: DashboardService  // Single, focused dependency
  ) {}
}

// ❌ Avoid storing observables in component
@Component({ ... })
export class BadComponent {
  customers: Observable<Customer[]>;  // Should be signal or use async pipe

  ngOnInit() {
    this.customers = this.customerService.getCustomers();
  }
}

// ✅ Use signal or async pipe
@Component({ ... })
export class GoodComponent {
  customers = signal<Customer[]>([]);
  // or
  customers$ = this.customerService.getCustomers();
}
```
