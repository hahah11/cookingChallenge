# Frontend Guidelines (Angular)

## Documentation

| Document | Purpose |
|----------|---------|
| [`docs/frontend/01-architecture.md`](../docs/frontend/01-architecture.md) | **Frontend Architecture**: Layer responsibilities, project structure, component communication |
| [`docs/frontend/02-components.md`](../docs/frontend/02-components.md) | **Components & Templates**: Component design, @Input/@Output, templates, change detection, forms |
| [`docs/frontend/03-services-state.md`](../docs/frontend/03-services-state.md) | **Services & State**: Service patterns, signals, RxJS, HTTP interceptors, state management |
| [`docs/frontend/04-routing-forms-http.md`](../docs/frontend/04-routing-forms-http.md) | **Routing, Forms & HTTP**: Guards, resolvers, reactive forms, HTTP client setup, API services |
| [`docs/frontend/05-performance-testing.md`](../docs/frontend/05-performance-testing.md) | **Performance & Testing**: Optimization techniques, component/service testing, best practices |

## Quick Reference: When Starting a Task

1. Check existing feature modules for similar patterns
2. Create model/interfaces matching backend DTOs
3. Build API service extending base HTTP patterns
4. Create presentational components first
5. Build container components with state management
6. Add route configuration and guards if needed
7. Write component tests and service tests
