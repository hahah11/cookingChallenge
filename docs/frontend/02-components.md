# Angular Components & Templates

## Component Design

### Single Responsibility
- Each component should have ONE purpose
- Split large components into smaller, focused ones
- Extract presentational logic to separate components

```typescript
// ❌ BAD: Component doing too much
@Component({ template: `...lots of markup and logic...` })
export class CustomerComponent { /* Too much! */ }

// ✅ GOOD: Split into focused components
<app-customer-form (saved)="onCustomerSaved($event)"></app-customer-form>
<app-order-list [orders]="customer.orders"></app-order-list>
```

### Component Options
```typescript
@Component({
  selector: 'app-user-profile',  // kebab-case with prefix
  standalone: true,              // Use standalone components
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,  // Default to OnPush
  encapsulation: ViewEncapsulation.Emulated
})
export class UserProfileComponent { ... }
```

## Component API Design

### @Input Properties
```typescript
@Component({ ... })
export class CustomerCardComponent {
  // Required input
  @Input({ required: true }) customer!: Customer;

  // Optional input with default
  @Input() showOrders: boolean = false;

  // Input with alias
  @Input('customerData') customer!: Customer;

  // Input with setter for side effects
  @Input()
  set customer(value: Customer) {
    this._customer = value;
    this.loadOrders();
  }
  private _customer!: Customer;
}
```

### @Output Events
```typescript
@Component({ ... })
export class CustomerFormComponent {
  // Use EventEmitter with specific type
  @Output() saved = new EventEmitter<Customer>();
  @Output() cancelled = new EventEmitter<void>();

  onSave() {
    this.saved.emit(this.customer);  // Emit meaningful payload
  }
}
```

### Avoid @ViewChild When Possible
```typescript
// ❌ BAD: Tight coupling
@Component({ ... })
export class ParentComponent {
  @ViewChild(CustomerFormComponent) form!: CustomerFormComponent;
  
  submit() {
    this.form.save();  // Tight coupling
  }
}

// ✅ GOOD: Use @Output events
@Component({ ... })
export class ParentComponent {
  onCustomerSaved(customer: Customer) {
    this.customerService.update(customer);
  }
}
```

## Template Best Practices

### Built-in Control Flow (Angular 17+)
```html
<!-- ✅ Use @for instead of *ngFor -->
@for (customer of customers(); track customer.id) {
  <app-customer-card [customer]="customer" />
}
@empty {
  <p>No customers found</p>
}

<!-- ✅ Use @if instead of *ngIf -->
@if (isLoading()) {
  <app-loading-spinner />
} @else if (error()) {
  <app-error-message [error]="error()" />
} @else {
  <app-customer-list [customers]="customers()" />
}
```

### Property Binding vs Interpolation
```html
<!-- ✅ Use property binding for attributes -->
<img [src]="customer.avatarUrl" [alt]="customer.name" />

<!-- ❌ Avoid interpolation for attributes -->
<img src="{{ customer.avatarUrl }}" alt="{{ customer.name }}" />

<!-- ✅ Use property binding for booleans -->
<button [disabled]="!form.valid">Save</button>

<!-- ❌ Avoid string values -->
<button [disabled]="'true'">Save</button>
```

### Event Binding
```html
<!-- ✅ Use parentheses for events -->
<button (click)="onSave()">Save</button>

<!-- ✅ Use target for event properties -->
<input (input)="onInputChange($event.target.value)" />

<!-- ✅ Handle form submission -->
<form (submit)="onSubmit($event)"> ... </form>
```

### Two-Way Binding
```html
<!-- ✅ Use [(ngModel)] for forms -->
<input [(ngModel)]="customer.name" name="name" />

<!-- ✅ Create two-way binding with @Input/@Output -->
<!-- Child component -->
@Input() value: string = '';
@Output() valueChange = new EventEmitter<string>();

<!-- Parent template -->
<app-input [(value)]="customer.name" />
```

## Change Detection

### OnPush Strategy
```typescript
// ✅ Always use OnPush for better performance
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ...
})
export class CustomerCardComponent {
  @Input() customer!: Customer;
}

// ✅ Trigger change detection with immutable updates
@Component({ ... })
export class CustomerListComponent {
  customers = signal<Customer[]>([]);

  // ✅ Good - creates new array reference
  addCustomer(customer: Customer) {
    this.customers.update(list => [...list, customer]);
  }

  // ❌ Bad - mutates existing array (won't trigger change detection!)
  addCustomerBad(customer: Customer) {
    this.customers().push(customer);
  }
}
```

### Using async Pipe
```typescript
// ✅ Use async pipe for observables
@Component({
  template: `<app-customer-card [customer]="customer$ | async" />`
})
export class CustomerComponent {
  customer$ = this.customerService.getCustomerById(123);
}
```

## Forms in Components

### Reactive Forms
```typescript
@Component({ ... })
export class CustomerFormComponent {
  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.pattern(/^[+]?[0-9]{10,15}$/)]]
  });

  get nameControl() { return this.form.controls.name; }
  get emailControl() { return this.form.controls.email; }

  onSubmit() {
    if (this.form.valid) {
      this.customerService.create(this.form.value).subscribe();
    }
  }
}
```

### Template in Component
```html
<form [formGroup]="form" (ngSubmit)="onSubmit()">
  <input formControlName="name" placeholder="Name" />
  @if (nameControl.invalid && nameControl.touched) {
    <small class="error">Name is required</small>
  }

  <input formControlName="email" placeholder="Email" />
  @if (emailControl.invalid && emailControl.touched) {
    <small class="error">Valid email required</small>
  }

  <button type="submit" [disabled]="form.invalid">Save</button>
</form>
```

## Custom Validators
```typescript
export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumbers = /\d/.test(value);
    const hasSpecialChar = /[!@#$%^&*]/.test(value);

    return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar
      ? null
      : { passwordStrength: true };
  };
}

// Usage
form = this.fb.group({
  password: ['', [Validators.required, passwordStrengthValidator()]]
});
```

## Component Styling

### Scoped Styles
```typescript
@Component({
  styleUrls: ['./customer-card.component.scss'],
  // Styles are scoped to this component by default
})
export class CustomerCardComponent { ... }
```

### ViewEncapsulation Options
```typescript
@Component({
  encapsulation: ViewEncapsulation.Emulated,  // Default - scoped styles
  // encapsulation: ViewEncapsulation.None,   // Global styles
  // encapsulation: ViewEncapsulation.ShadowDom,  // Shadow DOM
})
```

### Style Best Practices
- Use SCSS for better maintainability
- Use CSS variables for theming
- Keep component styles small (<100 lines)
- Extract common styles to shared mixins

```scss
// customer-card.component.scss
:host {
  display: block;
  padding: 16px;
}

.card {
  background: var(--color-background);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.title {
  @include typography-headline;
  color: var(--color-text-primary);
}
```

## Accessibility (a11y)

### Best Practices
- Use semantic HTML (`<button>` not `<div onclick>`)
- Add `aria-label` for icon buttons
- Ensure keyboard navigation works
- Use proper heading hierarchy
- Add alt text for images

```html
<!-- ✅ Accessible button -->
<button 
  type="button"
  aria-label="Delete customer"
  (click)="onDelete()">
  <app-icon name="trash" />
</button>

<!-- ✅ Accessible form -->
<label for="email">Email Address</label>
<input 
  id="email"
  type="email"
  formControlName="email"
  [aria-invalid]="emailControl.invalid"
  [aria-describedby]="emailControl.invalid ? 'email-error' : null"
/>
@if (emailControl.invalid) {
  <div id="email-error" role="alert">
    {{ getErrorMessage(emailControl) }}
  </div>
}
```

## Component Testing
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

  it('should display customer name', () => {
    component.customer = { id: 1, name: 'John Doe' };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.name').textContent).toContain('John Doe');
  });

  it('should emit deleted event when delete button clicked', () => {
    spyOn(component.deleted, 'emit');
    component.customer = { id: 1, name: 'John' };
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.delete-btn').click();

    expect(component.deleted.emit).toHaveBeenCalled();
  });
});
```
