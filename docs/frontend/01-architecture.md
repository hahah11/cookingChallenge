# Frontend Architecture & Layering

## Layer Responsibilities

### Component Layer (UI)
- Template rendering and user interaction
- Bind to service data via `@Input`/`@Output`
- NO business logic in components
- ❌ Component calculating totals or validating email format
- ✅ Component displaying data, emitting events on user action

### Service Layer (Business Logic)
- Angular `@Injectable` services contain business rules
- HTTP calls to backend via `HttpClient`
- State management (RxJS subjects, signals)
- ✅ `CustomerService` calling `GET /api/customers` and mapping responses

### Model Layer (Data Structures)
- TypeScript interfaces/classes for API data
- DTOs match backend DTOs (keep naming consistent)
- ✅ `interface Customer { id: number; name: string; }`

### Guard/Interceptor Layer (Cross-Cutting)
- Route guards (`CanActivateFn`) for authentication
- HTTP interceptors for auth tokens, error handling
- Keep these focused on single responsibilities

## Project Structure

```
src/app/
├── features/              # Feature modules (standalone components)
│   ├── customers/
│   │   ├── components/
│   │   │   ├── customer-list/
│   │   │   │   ├── customer-list.component.ts
│   │   │   │   ├── customer-list.component.html
│   │   │   │   └── customer-list.component.scss
│   │   │   └── customer-card/
│   │   ├── services/
│   │   │   └── customer.service.ts
│   │   ├── models/
│   │   │   └── customer.ts
│   │   ├── guards/
│   │   │   └── customer-edit.guard.ts
│   │   ├── resolvers/
│   │   │   └── customer.resolver.ts
│   │   └── customers.routes.ts
│   └── orders/
│       └── ...
├── shared/                # Reusable components and utilities
│   ├── components/        # Generic UI components
│   │   ├── loading-spinner/
│   │   ├── error-message/
│   │   └── confirm-dialog/
│   ├── pipes/             # Transformation pipes
│   │   └── format-date.pipe.ts
│   └── utils/             # Helper functions
│       └── validators.ts
├── core/                  # Singletons and infrastructure
│   ├── services/          # Auth, HTTP interceptors
│   │   ├── auth.service.ts
│   │   └── notification.service.ts
│   ├── guards/            # Global route guards
│   │   └── auth.guard.ts
│   ├── interceptors/      # HTTP interceptors
│   │   ├── auth.interceptor.ts
│   │   └── error.interceptor.ts
│   └── models/            # Core domain models
│       └── user.ts
├── app.component.ts
├── app.config.ts
└── app.routes.ts
```

## Component Communication Patterns

### Parent to Child (@Input)
```typescript
// Child
@Component({ ... })
export class CustomerCardComponent {
  @Input({ required: true }) customer!: Customer;
}

// Parent
<app-customer-card [customer]="selectedCustomer" />
```

### Child to Parent (@Output)
```typescript
// Child
@Component({ ... })
export class CustomerCardComponent {
  @Output() deleted = new EventEmitter<void>();
  
  onDelete() {
    this.deleted.emit();
  }
}

// Parent
<app-customer-card (deleted)="handleCustomerDeleted()" />
```

### Service-Based Communication
```typescript
// For unrelated components, use a shared service
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private notification$ = new Subject<Notification>();
  
  show(notification: Notification) {
    this.notification$.next(notification);
  }
  
  getNotifications(): Observable<Notification> {
    return this.notification$.asObservable();
  }
}
```

## State Management Layers

### Simple State (Signals)
- Use for local component state
- Use for simple shared state

```typescript
@Component({ ... })
export class CustomerListComponent {
  customers = signal<Customer[]>([]);
  isLoading = signal(false);
  
  filteredCustomers = computed(() => {
    return this.customers().filter(c => c.status === 'ACTIVE');
  });
}
```

### Complex Async State (RxJS)
- Use for HTTP calls
- Use for complex event streams

```typescript
@Component({ ... })
export class CustomerListComponent {
  customers$ = this.customerService.getCustomers();
  error$ = this.errorService.getError();
}
```

### Global State (When Needed)
- Use NgRx/Elf only for truly complex global state
- Prefer service-based state with signals/RxJS

## Anti-Patterns to Avoid

### ❌ Fat Components
```typescript
// BAD: Component with too much logic
@Component({ ... })
export class CustomerComponent {
  // HTTP calls, validation, calculations all here
}

// GOOD: Extract to services
@Component({ ... })
export class CustomerComponent {
  constructor(
    private customerService: CustomerService,
    private validationService: ValidationService
  ) {}
}
```

### ❌ Direct DOM Manipulation
```typescript
// BAD
constructor(private el: ElementRef) {
  this.el.nativeElement.style.color = 'red';
}

// GOOD - Use Angular bindings
@Component({ template: `<div [style.color]="'red'"></div>` })
```

### ❌ Subscribing in Components
```typescript
// BAD - Memory leak risk
ngOnInit() {
  this.customerService.getCustomers().subscribe(data => {
    this.customers = data;
  });
}

// GOOD - Use async pipe
@Component({ template: `<ng-container *ngFor="let c of customers$ | async"></ng-container>` })
export class CustomerComponent {
  customers$ = this.customerService.getCustomers();
}
```

## File Naming Conventions

- Components: `feature-name.component.ts`
- Services: `feature-name.service.ts`
- Models: `feature-name.ts` or `types.ts`
- Guards: `feature-name.guard.ts`
- Resolvers: `feature-name.resolver.ts`
- Routes: `feature-name.routes.ts`
- Tests: `*.component.spec.ts`, `*.service.spec.ts`
