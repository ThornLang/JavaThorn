# Type Alias Tests for ThornLang (Issue #65)

This directory contains comprehensive tests for the type alias feature implemented in ThornLang. Type aliases allow you to create custom names for types using the `%` syntax.

## Test Files

### 1. `test_type_alias.thorn`
Basic smoke test that demonstrates type alias functionality:
- Simple type aliases for primitives
- Composite type aliases (Dict, Array)
- Function type aliases
- Usage in variable declarations

### 2. `type_alias_basic_test.thorn`
Comprehensive tests for basic type alias features:
- Primitive type aliases (number, string, boolean, null, Any)
- Function usage with type aliases
- Arrays with type aliases
- Nested type aliases
- Type alias shadowing in different scopes

### 3. `type_alias_generic_test.thorn`
Tests for generic type aliases:
- Generic collection aliases (Array[T], Dict[K,V])
- Function type aliases with parameters
- Complex nested type aliases
- Type aliases with classes
- Type alias chains

### 4. `type_alias_edge_cases_test.thorn`
Edge case and boundary condition tests:
- Recursive-like type aliases
- Very long type alias names
- Unicode characters in type aliases
- Special characters in names
- Empty collections with type aliases
- Multiple type aliases on one line
- Null and void type aliases
- Function composition with type aliases

### 5. `type_alias_stdlib_integration_test.thorn`
Integration with the test.thorn standard library:
- Using type aliases with test framework
- Test suites with type aliases
- Assertions with generic types
- Collection tests
- Function type tests

### 6. `type_alias_vm_test.thorn`
VM-specific tests (run with `--vm` flag):
- Basic types in VM mode
- Performance testing with type aliases
- Collections in VM
- Function types in VM
- Class integration in VM
- Recursive structures

### 7. `type_alias_errors_test.thorn`
Error condition tests:
- Wrong primitive type assignments
- Wrong collection element types
- Dictionary type mismatches
- Function type mismatches
- Null assignments
- Undefined type aliases
- Nested type mismatches
- Function parameter count errors

## Running the Tests

### Individual Tests
```bash
# Run basic tests
java com.thorn.Thorn tests/gh-65/type_alias_basic_test.thorn

# Run VM-specific tests
java com.thorn.Thorn tests/gh-65/type_alias_vm_test.thorn --vm

# Run integration tests
java com.thorn.Thorn tests/gh-65/type_alias_stdlib_integration_test.thorn
```

### All Tests
```bash
# Run all type alias tests
for test in tests/gh-65/*.thorn; do
    echo "Running $test..."
    java com.thorn.Thorn "$test"
done
```

## Type Alias Syntax

### Basic Syntax
```thorn
% TypeName = Type;
```

### Examples
```thorn
// Primitive types
% UserId = number;
% Username = string;
% IsActive = boolean;

// Collection types
% UserList = Array[User];
% ScoreMap = Dict[string, number];

// Function types
% Validator = Function[(string), boolean];
% Transformer = Function[(number), string];

// Nested types
% Matrix = Array[Array[number]];
% UserData = Dict[string, Any];
```

## Features Tested

1. **Type Definition**: Creating type aliases with `%` syntax
2. **Type Usage**: Using type aliases in variable declarations
3. **Type Checking**: Runtime type validation with aliases
4. **Scope Rules**: Local aliases can shadow global ones
5. **Generic Types**: Type aliases work with generic collections
6. **Function Types**: Full support for function type aliases
7. **VM Compatibility**: Type aliases work in both interpreter and VM modes
8. **Error Messages**: Clear error messages when type constraints are violated

## Known Limitations

1. Type aliases are resolved at runtime, not compile time
2. No support for union types (e.g., `string | null`)
3. Type parameters in aliases are not yet supported (e.g., `% Maybe[T] = T | null;`)
4. Circular type aliases are not detected at parse time

## Implementation Notes

- Type aliases are stored in the environment like variables
- They are immutable once defined
- The parser uses lookahead to distinguish `%` (modulo) from type alias syntax
- Both interpreter and VM treat type aliases as compile-time constructs
- Type checking happens at variable initialization and assignment