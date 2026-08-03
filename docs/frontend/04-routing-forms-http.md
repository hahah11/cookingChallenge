# Angular Routing, Forms & HTTP

## Routing Configuration

### Route Setup (Standalone)

```typescript
// ✅ app.routes.ts
export const routes: Routes = [
  { path: '', redirectTo: 'customers', pathMatch: 'full' },
  {
    path: 'customers',
    loadComponent: () => import('./customers/customer-list.component')
      .then(m => m.CustomerListComponent),
    title: 'Customers'
  },
  {
    path: 'customers/:id',
    loadComponent: () => import('./customers/customer-detail.component')
      .then(m => m.CustomerDetailComponent),
    resolve: { customer: customerResolver },
    title: 'Customer Details'
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes')
      .then(m => m.ADMIN_ROUTES),
    canActivate: [adminGuard]
  },
  { path: '**', loadComponent: () => import('./not-found.component') }
];
```

### Feature Routes

```typescript
// ✅ customers.routes.ts
export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./customer-list.component')
      .then(m => m.CustomerListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./customer-form.component')
      .then(m => m.CustomerFormComponent),
    canActivate: [canActivateEditGuard]
  },
  {
    path: ':id',
    loadComponent: () => import('./customer-detail.component')
      .then(m => m.CustomerDetailComponent),
    resolve: { customer: customerResolver }
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./customer-form.component')
      .then(m => m.CustomerFormComponent),
    canActivate: [canActivateEditGuard],
    resolve: { customer: customerResolver }
  }
];
```

## Route Guards

### Functional Guards (Angular 15+)

```typescript
// ✅ Auth guard
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};

// ✅ Admin guard
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);

  if (authService.isAdmin()) {
    return true;
  }

  return false;  // Or redirect
};

// ✅ CanDeactivate for unsaved changes
export interface CanDeactivateComponent {
  canDeactivate: () => boolean | Observable<boolean>;
}

export const unsavedChangesGuard: CanDeactivateFn<CanDeactivateComponent> =
  (component) => {
    if (component.canDeactivate()) {
      return true;
    }

    return confirm('You have unsaved changes. Leave anyway?');
  };
```

### Using Guards in Routes

```typescript
{
  path: 'customers/:id/edit',
  loadComponent: () => import('./customer-form.component'),
  canActivate: [authGuard, unsavedChangesGuard],
  canDeactivate: [unsavedChangesGuard]
}
```

## Route Resolvers

### Basic Resolver

```typescript
// ✅ Resolver function
export const customerResolver: ResolveFn<Customer | null> = (route, state) => {
  const customerService = inject(CustomerService);
  const id = route.params['id'];

  return customerService.getCustomerById(id).pipe(
    tap(customer => console.log('Customer loaded')),
    catchError(error => {
      console.error('Failed to load customer', error);
      return of(null);  // Or trigger redirect
    })
  );
};

// ✅ Use in component
@Component({ ... })
export class CustomerDetailComponent implements OnInit {
  customer!: Customer | null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.customer = this.route.snapshot.data['customer'];
  }
}
```

### Multiple Resolvers

```typescript
// ✅ Resolve multiple data points
export const customerOrderResolver: ResolveFn<{
  customer: Customer;
  orders: Order[];
}> = (route, state) => {
  const customerService = inject(CustomerService);
  const orderService = inject(OrderService);
  const id = route.params['id'];

  return forkJoin({
    customer: customerService.getCustomerById(id),
    orders: orderService.getOrdersByCustomerId(id)
  });
};
```

## Navigation

### Programmatic Navigation

```typescript
@Component({ ... })
export class CustomerFormComponent {
  constructor(private router: Router) {}

  onSave() {
    this.customerService.create(this.form.value).subscribe(customer => {
      // ✅ Navigate after successful save
      this.router.navigate(['/customers', customer.id]);
    });
  }

  onCancel() {
    // ✅ Navigate back
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
```

### Navigation with Query Params

```typescript
// ✅ Navigate with query params
this.router.navigate(['/customers'], {
  queryParams: { page: 2, filter: 'active' }
});

// ✅ Read query params
const page = this.route.snapshot.queryParams['page'];
```

## Forms - Reactive Forms

### Basic Form Setup

```typescript
@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <div formGroupName="address">
        <input formControlName="street" placeholder="Street" />
        <input formControlName="city" placeholder="City" />
        <input formControlName="zipCode" placeholder="ZIP" />
      </div>

      <button type="submit" [disabled]="form.invalid">Save</button>
    </form>
  `
})
export class CustomerFormComponent implements OnInit {
  form!: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit() {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.pattern(/^[+]?[0-9]{10,15}$/)]],
      address: this.fb.group({
        street: [''],
        city: [''],
        zipCode: ['', [Validators.pattern(/^\d{5}$/)]]
      }),
      status: ['ACTIVE', [Validators.required]]
    });
  }

  onSubmit() {
    if (this.form.valid) {
      this.customerService.create(this.form.value).subscribe();
    }
  }
}
```

### Typed Forms (Angular 14+)

```typescript
// ✅ Use typed FormGroup
interface CustomerForm {
  name: FormControl<string>;
  email: FormControl<string>;
  phone: FormControl<string | null>;
  address: FormGroup<{
    street: FormControl<string>;
    city: FormControl<string>;
    zipCode: FormControl<string>;
  }>;
}

form: FormGroup<CustomerForm>;

ngOnInit() {
  this.form = this.fb.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    address: this.fb.group({
      street: [''],
      city: [''],
      zipCode: ['']
    })
  });
}

// ✅ Type-safe access
get nameControl() {
  return this.form.controls.name;  // Typed as FormControl<string>
}
```

### Custom Validators

```typescript
// ✅ Synchronous custom validator
export function notEmptyValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    return control.value !== null && control.value !== '' ? null : { notEmpty: true };
  };
}

// ✅ Async custom validator
export function uniqueEmailValidator(customerService: CustomerService): AsyncValidatorFn {
  return (control: AbstractControl): Observable<ValidationErrors | null> => {
    if (!control.value) {
      return of(null);
    }

    return customerService.existsByEmail(control.value).pipe(
      map(exists => exists ? { uniqueEmail: true } : null),
      catchError(() => of(null))
    );
  };
}

// ✅ Usage
form = this.fb.group({
  email: ['', [Validators.required, Validators.email], [uniqueEmailValidator(this.customerService)]]
});
```

### Validation Is UX Only

Reactive form validators (`Validators.required`, custom validators, async validators) exist purely to give the user instant feedback — they are never the security boundary. The backend re-validates everything with Bean Validation and business rules regardless of what the frontend already checked (see [`docs/shared/04-api-design.md#validation-duplication`](../shared/04-api-design.md#validation-duplication)). Never skip a frontend validator because "the backend already checks it" — the two serve different purposes (UX speed vs. security) and both are required.

### Form State and Validation Display

```html
<form [formGroup]="form" (ngSubmit)="onSubmit()">
  <input formControlName="name" placeholder="Name" />

  @if (nameControl.invalid && (nameControl.touched || nameControl.dirty)) {
    @if (nameControl.hasError('required')) {
      <small class="error">Name is required</small>
    }
    @if (nameControl.hasError('maxlength')) {
      <small class="error">Name must be at most 100 characters</small>
    }
  }

  <button type="submit" [disabled]="form.invalid || form.pristine">
    Save
  </button>
</form>
```

## HTTP Client Setup

### Configuration

```typescript
// ✅ app.config.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    )
  ]
};
```

### Auth Interceptor

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.accessToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
  }

  return next(req);
};
```

### Error Interceptor

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = 'An error occurred';

      if (error.status === 401) {
        message = 'Please log in';
        // Redirect to login
      } else if (error.status === 403) {
        message = 'You do not have permission';
      } else if (error.status === 404) {
        message = 'Resource not found';
      } else if (error.status === 500) {
        message = 'Server error. Please try again later.';
      } else if (error.error?.message) {
        message = error.error.message;
      }

      notificationService.error(message);
      return throwError(() => error);
    })
  );
};
```

## API Service Pattern

The client shown below (`getAll`, `getById`, `create`, ...) is generated by `@openapitools/openapi-generator-cli` (generator `typescript-angular`) from `openapi/cookingchallenge-api.yaml` — see [`docs/shared/04-api-design.md#api-contract-openapi-first`](../shared/04-api-design.md#api-contract-openapi-first). Never hand-edit the generated service; if a method or shape is wrong, fix the spec and regenerate. This example illustrates the shape of the generated client so any hand-written code that *wraps* it (e.g. adding local caching around a generated call) stays consistent with it.

```typescript
@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly baseUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  // ✅ GET all with pagination
  getAll(page: number = 0, size: number = 20, sort?: string): Observable<Page<Customer>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (sort) {
      params = params.set('sort', sort);
    }

    return this.http.get<Page<Customer>>(this.baseUrl, { params });
  }

  // ✅ GET by ID
  getById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  // ✅ POST create
  create(customer: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, customer).pipe(
      tap(created => console.log('Customer created:', created.id))
    );
  }

  // ✅ PUT update
  update(id: number, customer: UpdateCustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, customer);
  }

  // ✅ PATCH partial update
  patch(id: number, updates: Partial<Customer>): Observable<Customer> {
    return this.http.patch<Customer>(`${this.baseUrl}/${id}`, updates);
  }

  // ✅ DELETE
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ✅ Search with query params
  search(query: string): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/search`, {
      params: { q: query }
    });
  }
}
```

## Best Practices Summary

### DO
- ✅ Use functional guards (`CanActivateFn`)
- ✅ Use resolvers for data that must be loaded before route activation
- ✅ Use typed forms for type safety
- ✅ Use `HttpParams` for query parameters
- ✅ Handle errors in interceptors for global error handling
- ✅ Use `tap` for side effects (logging, notifications)
- ✅ Use `catchError` to transform errors

### DON'T
- ❌ Subscribe in services (return observables)
- ❌ Put business logic in guards
- ❌ Hardcode URLs (use environment variables)
- ❌ Ignore error handling
- ❌ Use `any` type for form controls
- ❌ Navigate without handling cancellation
