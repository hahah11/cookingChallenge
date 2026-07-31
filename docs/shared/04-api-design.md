# API Design Guidelines

## REST Conventions

### HTTP Methods

| Method | Purpose | Idempotent | Safe |
|--------|---------|------------|------|
| `GET` | Read resources | Yes | Yes |
| `POST` | Create resources | No | No |
| `PUT` | Full update (replace) | Yes | No |
| `PATCH` | Partial update | No | No |
| `DELETE` | Remove resources | Yes | No |

### Status Codes

**Success (2xx)**
- `200 OK` - Successful GET, PUT, PATCH
- `201 Created` - Successful POST (resource created)
- `204 No Content` - Successful DELETE (or PUT/PATCH with no response body)

**Client Errors (4xx)**
- `400 Bad Request` - Invalid request format or validation failed
- `401 Unauthorized` - Authentication required or failed
- `403 Forbidden` - Authenticated but not authorized
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Resource conflict (e.g., duplicate)
- `422 Unprocessable Entity` - Semantic validation error
- `429 Too Many Requests` - Rate limiting

**Server Errors (5xx)**
- `500 Internal Server Error` - Unexpected server error
- `502 Bad Gateway` - Upstream service error
- `503 Service Unavailable` - Service temporarily unavailable

### URL Structure

**General Rules**
- Use **plural nouns** for resources: `/api/customers` (not `/api/customer`)
- Use **lowercase** with **hyphens**: `/api/order-items` (not `/api/orderItems`)
- Use **nested paths** for relationships: `/api/customers/{id}/orders`
- Use **query parameters** for filtering, sorting, pagination

**Examples**
```
✅ GET    /api/customers              # List all customers
✅ GET    /api/customers/123          # Get customer by ID
✅ POST   /api/customers              # Create new customer
✅ PUT    /api/customers/123          # Replace customer
✅ PATCH  /api/customers/123          # Partial update customer
✅ DELETE /api/customers/123          # Delete customer

✅ GET    /api/customers/123/orders   # List orders for customer
✅ POST   /api/customers/123/orders   # Create order for customer

❌ GET    /api/getCustomers           # Don't use verbs in URLs
❌ GET    /api/customer/123           # Use plural: /customers
❌ GET    /api/Customer/123           # Use lowercase
❌ POST   /api/customers/create       # POST to /customers
```

## Response Format

### Success Response Envelope

```json
{
  "data": { ... },
  "meta": {
    "requestId": "abc-123-def",
    "timestamp": "2026-07-29T10:30:00Z"
  }
}
```

### List Response with Pagination

```json
{
  "data": [
    { "id": 1, "name": "John Doe" },
    { "id": 2, "name": "Jane Smith" }
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "meta": {
    "requestId": "abc-123-def",
    "timestamp": "2026-07-29T10:30:00Z"
  }
}
```

### Error Response Envelope

```json
{
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "Customer with ID 123 was not found",
    "details": [
      {
        "field": "id",
        "message": "Customer does not exist"
      }
    ],
    "requestId": "abc-123-def",
    "timestamp": "2026-07-29T10:30:00Z"
  }
}
```

## Versioning Strategy

### URI Versioning (Recommended)

```
/api/v1/customers
/api/v2/customers
```

**Implementation:**
```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/customers")
public class CustomerControllerV2 { ... }
```

### Deprecation Policy

**When deprecating a version:**
1. Add `Deprecation: true` header to responses
2. Add `Sunset: <date>` header with removal date
3. Document migration guide for users
4. Keep old version for at least 6 months

## Query Parameters

### Filtering
```
GET /api/customers?status=ACTIVE&country=USA
GET /api/customers?createdAt>=2026-01-01&createdAt<=2026-12-31
```

### Sorting
```
GET /api/customers?sort=name
GET /api/customers?sort=name:asc,createdAt:desc
```

### Pagination
```
GET /api/customers?page=0&size=20
```
**Defaults:** `page`: 0 (zero-indexed), `size`: 20, `maxSize`: 100

## Request/Response Validation

### Bean Validation (Backend)
```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email
) {}
```

### Controller Validation
```java
@PostMapping("/customers")
public ResponseEntity<CustomerResponse> create(
    @Valid @RequestBody CreateCustomerRequest request
) {
    Customer customer = customerService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
}
```

## Best Practices

### DO
- ✅ Use noun-based resource names
- ✅ Return consistent JSON structure
- ✅ Use proper HTTP status codes
- ✅ Document all endpoints (OpenAPI/Swagger)
- ✅ Implement rate limiting
- ✅ Use HTTPS in production

### DON'T
- ❌ Use verbs in URLs (`/api/getCustomers`)
- ❌ Return 200 for errors
- ❌ Expose internal error messages
- ❌ Return sensitive data (passwords, tokens)
- ❌ Forget to handle pagination for lists
