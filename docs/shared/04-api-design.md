# API Design Guidelines

## API Contract: OpenAPI-First

The API is spec-first, not code-first. `openapi/cookingchallenge-api.yaml` (repo root — a neutral contract, not nested under either project) is the single source of truth. Nobody hand-writes a controller interface or an HTTP client method that duplicates what the spec already describes — both sides generate from it.

- **Backend**: `org.openapitools.openapi-generator-gradle-plugin` (generator `spring`, `interfaceOnly=true`, `skipDefaultInterface=true`, `useTags=true`) generates one Java interface per tag (`ChallengesApi`, `AccountsApi`, ...) plus model classes into a build-only output directory that is never committed. `@RestController` classes implement the generated interface — they don't declare their own request/response record types.
- **Frontend**: `@openapitools/openapi-generator-cli` (generator `typescript-angular`) generates the Angular HTTP client from the same spec — same tool family as the backend, one codegen toolchain to learn instead of two.
- **Never hand-edit generated code** (workspace rule, see `CLAUDE.md`). If a generated shape is wrong, fix the spec and regenerate — don't patch the output.
- **The `{data, meta}` / error envelope is part of the spec, not bolted on after serialization.** OpenAPI 3.0 has no generics, so every response is its own named wrapper schema (`ChallengeResponse { data: Challenge, meta: ApiMeta }`, `ChallengeListResponse { data: Challenge[], meta: ApiMeta }`) — verbose to author, but it's the only way the generated client's types match the actual wire format.
- **Page-scoped DTOs are spec schemas, not just a convention** (see "Page-Scoped Query Endpoints" below) — `OrderDetailPageResponse`, `CancelOrderDialogResponse`, etc. are each their own named schema in the spec, tagged to the endpoint that returns them.

See [`docs/cookingChallenge/plans/openapi-first-api-plan.md`](../cookingChallenge/plans/openapi-first-api-plan.md) for the concrete rollout of this in the current codebase.

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

## Page-Scoped Query Endpoints

Every screen (page/route) gets **one primary GET endpoint** that returns exactly what's needed for its initial render — nothing the user hasn't asked to see yet. This is a BFF (Backend For Frontend)-style convention: endpoints and DTOs are shaped around screens, not generic reusable resources. Popups, dialogs, tabs, and expandable panels each get their **own** endpoint, called only when the user actually opens them (CQRS-lite: these are all read-side queries, kept separate from write-side commands).

### Rules

1. **One page = one primary query.** `GET /api/v1/orders/{id}` returns the order detail screen's data. It does NOT include the "cancel order" dialog's cancellation-reason options, or a side panel's full order history — those are separate calls.
2. **Popups/dialogs fetch on open, not on page load.** A dialog component calls its own endpoint when it opens, and never reuses data already held by its parent page — even if fields overlap. This avoids showing stale data if something changed between the page load and the popup open.
3. **DTOs are named after the screen/interaction they serve**, not the entity: `OrderDetailPageResponse`, `CancelOrderDialogResponse` — not one `OrderResponse` stretched to fit every context.
4. **No client-side aggregation across modules.** If a screen needs data from two backend modules (e.g. order + customer), the backend aggregates it server-side into one page DTO — see [`docs/backend/02-ddd-modulith.md#page-query-services`](../backend/02-ddd-modulith.md#page-query-services). The frontend never fans out to multiple endpoints and stitches results together itself.
5. **Cross-cutting data (roles, feature flags) is not page-scoped** — see Configuration Endpoint below.

### Example

```
✅ GET /api/v1/orders/{id}                 # Order detail page (main data)
✅ GET /api/v1/orders/{id}/cancel-options   # Cancel dialog, fetched on open
✅ GET /api/v1/orders/{id}/history          # "View history" popup, fetched on open

❌ GET /api/v1/orders/{id}                  # returns order + cancelOptions + fullHistory in one bloated DTO
```

### Configuration Endpoint

Data that isn't tied to one screen — the current user's roles/permissions, feature flags, shared lookup/enum values — is served by a single `GET /api/v1/config` endpoint. The frontend fetches it once at app bootstrap (see [`docs/frontend/03-services-state.md#config-service`](../frontend/03-services-state.md#config-service)), not per page. Roles and permissions are never derived or hardcoded on the frontend — the UI always reflects what this endpoint returned.

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

### Validation Duplication

Client-side validation (Angular reactive form validators) exists purely for UX responsiveness — instant feedback without a round trip. It is never trusted. The backend re-validates everything with Bean Validation (`@Valid`) and its own business rules regardless of what the frontend already checked, and must reject a request that skips the frontend entirely (Postman, another client, a malicious actor) exactly as it would reject one from the app. See [`docs/frontend/04-routing-forms-http.md#validation-is-ux-only`](../frontend/04-routing-forms-http.md#validation-is-ux-only).

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
