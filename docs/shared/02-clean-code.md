# Clean Code Principles (Robert C. Martin)

## Names That Matter

### Meaningful Names
- Use intention-revealing names: `daysSinceCreation` not `d`
- Avoid disinformation: don't use `accountList` for an `ArrayList`
- Make meaningful distinctions: `getActiveCustomers()` vs `getAllCustomers()`
- Use pronounceable names: `genymdhms` is BAD, `birthTimestamp` is GOOD
- Use searchable names: single letters like `i`, `j` are hard to find in code
- No magic numbers: `const MAX_RETRY_COUNT = 3` not `if (attempts < 3)`

### Function Names
- Verbs for functions: `calculateTotal()`, `findCustomer()`, `sendEmail()`
- Command-query separation: functions either DO something or RETURN something, not both
- Avoid side effects: `updateStatus()` should not also send an email unless that's obvious

## Functions

### Small is Beautiful
- Functions should be SMALL (ideally < 20 lines, never > 50)
- Do ONE thing only - if you can describe it with "and", it does too much
- Example: `validateAndSaveCustomer()` should be `validateCustomer()` + `saveCustomer()`

### One Level of Abstraction
- All statements in a function should be at the same level
- Don't mix HTTP handling with business logic in the same function
- Use "Extract Method" when you see comments or mixed concerns

### Function Arguments
- Prefer 0-2 arguments; avoid 3+ (use a parameter object instead)
- ❌ `createCustomer(name, email, phone, address, city, zip, country)`
- ✅ `createCustomer(name, email, phone, addressInfo)` where `addressInfo` is a class

### No Side Effects
- Functions should not secretly change external state
- Don't modify a parameter that wasn't meant to be modified
- Document mutations clearly if they're necessary

## Comments

### Comments Are Failures
- Prefer clean code over comments that explain bad code
- If you need a comment to explain "why", the code should be self-documenting
- ❌ `// increment counter` → `counter++` (obvious)
- ✅ `// Retry 3x because DB sometimes times out under load` (explains business context)

### Comment Types That ARE Useful
- Javadoc for public APIs (what it does, params, returns, exceptions)
- TODO with context and owner: `// TODO(@user): Optimize this query by July 2026`
- Legal notices, deprecation warnings, architectural notes
- Explain WHY something is done, not WHAT the code does

### Bad Comments
- Redundant comments: `// Get the customer by ID` above `getCustomerById()`
- Commented-out code: delete it, use version control
- Mumbling: vague comments that add no value
- Position markers: `// ========== BEGIN ==========` (use file structure instead)

## Formatting

### Vertical Organization
- Related functions should be close together (callers above callees)
- Declare variables near their first use, not at the top of the function
- Keep classes/files vertically manageable (< 500 lines per file ideally)

### Consistent Style
- Follow team conventions (braces, indentation, line length ~120 chars)
- Use your IDE's auto-formatter; don't argue about style in PRs

## Objects and Data Structures

### Data Abstraction
- Hide implementation details; expose behavior
- ❌ `customer.address.city` (exposes internal structure)
- ✅ `customer.getShippingCity()` (preserves abstraction)

### Law of Demeter ("Don't talk to strangers")
- A module should not know the inner workings of other modules
- Avoid chains: `order.getCustomer().getAddress().getCity()`
- Prefer: `order.getShippingCity()` or `order.getCustomer().getShippingCity()`

### Data-Oriented vs Object-Oriented
- Procedural code exposes data, OO hides data
- DTOs/Records are fine for data transfer; business objects should have behavior
- Don't create "anemic domain models" where every class is just getters/setters

## Error Handling

### Use Exceptions, Not Return Codes
- ❌ `int processOrder() { return -1; }`
- ✅ `void processOrder() { throw new OrderProcessingException(); }`

### Catch Specific Exceptions
- ❌ `catch (Exception e)` - catches everything, hides bugs
- ✅ `catch (SQLException e)` - handles what you expect

### Don't Return Null
- Return empty collections instead of `null`
- ❌ `List<Customer> getCustomers() { return null; }`
- ✅ `List<Customer> getCustomers() { return Collections.emptyList(); }`

### Write Try-Catch Blocks Cleanly
- The `try` block should be small and readable
- Handle errors gracefully; don't just log and ignore
- Use `finally` for cleanup (close resources, connections)

## Code Smells (Red Flags to Refactor)

### When You See These, FIX THEM (don't ignore)
- ❌ Long Method (>20 lines)
- ❌ Large Class (>500 lines, too many responsibilities)
- ❌ Duplicate Code (same pattern in 3+ places)
- ❌ Long Parameter List (>3 parameters)
- ❌ Feature Envy (method uses another class's data more than its own)
- ❌ Data Clumps (groups of variables that always appear together)
- ❌ Primitive Obsession (using primitives instead of small objects)
- ❌ Comments as Masking (comments hiding ugly/unclear code)
- ❌ Divergent Change (one class changed for different reasons each time)
- ❌ Shotgun Surgery (one change requires editing many files)

### Refactoring Rule
- Make the change, then refactor; don't refactor without a reason
- Small, incremental refactors are safer than big rewrites
- Test coverage is required before refactoring complex code
- **Only refactor code you directly touch** - if you're modifying a file, improve its structure as you go
- **Do NOT search for unrelated code to refactor** - don't go on "refactoring hunts" through the codebase
- If you notice a code smell while working on a feature, make a mental note but stay focused on the task
- Exception: If the code smell BLOCKS your change (e.g., method signature needs to change), then refactor that specific piece
