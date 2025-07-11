# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## VM Integration
- Don't remove our VM integration ever, we just need to fix it.

## Common Development Commands

### Build and Run
```bash
# Build the project
./scripts/build.sh

# Run a Thorn script
java com.thorn.Thorn script.thorn

# Run with VM (bytecode interpreter)
java com.thorn.Thorn script.thorn --vm

# View AST for debugging
java com.thorn.Thorn script.thorn --ast

# Interactive REPL
java com.thorn.Thorn
```

### Performance Testing
```bash
# Quick performance comparison with other languages
./benchmarks/quick_compare.sh

# Comprehensive benchmarks
./benchmarks/comprehensive/run_all.sh
```

## ThornLang Syntax Guide

### Variables and Constants

```thorn
// Regular variables (mutable)
name = "Alice";
age = 25;
score = 98.5;

// Typed variables
count: number = 0;
message: string = "Hello";
isActive: boolean = true;

// Immutable variables
@immut PI = 3.14159;
@immut MAX_SIZE: number = 100;

// Null values
value = null;
optional: string = null;
```

### Functions

```thorn
// Function definition with $ prefix
$ greet(name: string) {
    print("Hello, " + name);
}

// Function with return type
$ add(a: number, b: number): number {
    return a + b;
}

// Lambda functions
square = $(x) => x * x;
filter = $(arr, predicate) => {
    result = [];
    for (item in arr) {
        if (predicate(item)) {
            result.push(item);
        }
    }
    return result;
};

// Function types
validator: Function[(string), boolean] = $(s) => s.length > 0;
processor: Function[(number, number), number] = $(a, b) => a + b;
callback: Function[(), void] = $() => { print("Done!"); };

// Functions with Dict type parameters
$ getScore(scoreMap: Dict[string, number], name: string): number {
    return scoreMap[name] ?? 0;
}

// Functions returning Dict types
$ createConfig(): Dict[string, Any] {
    return {"host": "localhost", "port": 8080, "debug": true};
}
```

### Classes and Objects

```thorn
// Class definition
class Person {
    // Constructor (always named 'init')
    $ init(name: string, age: number) {
        // Property initialization (no 'this.' required in constructors)
        name: string = name;
        age: number = age;
        // Or with explicit this
        this.id = generate_id();
    }
    
    $ greet(): string {
        return "Hello, I'm " + this.name;
    }
    
    $ have_birthday(): void {
        this.age = this.age + 1;
    }
}

// Creating instances
person: Person = Person("Bob", 30);
alice = Person("Alice", 25);  // Type inferred

// Object literals
config = {
    "host": "localhost",
    "port": 8080,
    "debug": true
};

// Accessing properties
print(person.name);
print(config["port"]);
```

### Control Flow

```thorn
// If-else statements
if (score >= 90) {
    grade = "A";
} else if (score >= 80) {
    grade = "B";
} else {
    grade = "C";
}

// While loops
i = 0;
while (i < 10) {
    print(i);
    i = i + 1;
}

// For loops (C-style)
for (j = 0; j < 5; j = j + 1) {
    print(j);
}

// For-in loops
for (item in array) {
    print(item);
}

// Pattern matching
result = match value {
    0 => "zero",
    1 => "one",
    n if n > 0 => "positive",
    _ => "negative",
};
```

### Arrays and Collections

```thorn
// Array literals
numbers = [1, 2, 3, 4, 5];
mixed = ["hello", 42, true, null];

// Typed arrays
scores: Array[number] = [95, 87, 92];
names: Array[string] = ["Alice", "Bob", "Charlie"];

// Array methods
numbers.push(6);           // Add to end
first = numbers.shift();   // Remove from start
numbers.unshift(0);        // Add to start
last = numbers.pop();      // Remove from end
exists = numbers.includes(3);  // Check if contains
subset = numbers.slice(1, 3);  // Get subset

// Accessing elements
first_item = numbers[0];
numbers[1] = 100;

// Dictionary/Map operations
person = {
    "name": "Alice",
    "age": 25,
    "city": "Seattle"
};
person["age"] = 26;
city = person["city"];
```

### Type System

```thorn
// Primitive types
str: string = "hello";
num: number = 42;
bool: boolean = true;
nothing: null = null;
anything: Any = "can be anything";
empty: void = null;

// Class types
user: Person = Person("Alice", 25);
users: Array[Person] = [user];

// Function types
// Old syntax (still supported)
fn: Function[string] = $() => "result";

// New syntax with parameters
handler: Function[(string, number), boolean] = $(name, age) => age >= 18;

// Generic types
list: Array[number] = [1, 2, 3];
matrix: Array[Array[number]] = [[1, 2], [3, 4]];

// Dictionary types (Dict[K,V])
scores: Dict[string, number] = {"alice": 95, "bob": 87};
errors: Dict[string, string] = {"404": "Not Found", "500": "Server Error"};
config: Dict[string, Any] = {"port": 8080, "debug": true, "name": "MyApp"};

// Nested dictionary types
users: Dict[string, Dict[string, Any]] = {
    "alice": {"age": 25, "city": "Seattle"},
    "bob": {"age": 30, "city": "Portland"}
};
```

### Operators

```thorn
// Arithmetic
sum = a + b;
diff = a - b;
product = a * b;
quotient = a / b;
remainder = a % b;
power = a ** b;

// Compound assignment
x += 5;  // x = x + 5
y *= 2;  // y = y * 2

// Comparison
equals = a == b;
not_equals = a != b;
less = a < b;
greater = a > b;
less_equal = a <= b;
greater_equal = a >= b;

// Logical
and_result = a && b;
or_result = a || b;
not_result = !value;

// Null coalescing
default_value = nullable_value ?? "default";
```

### Module System

```thorn
// Import specific items
import { utils, helpers } from "./utilities";

// Import entire module
import "./config";

// Export variables
export name = "MyModule";
export @immut VERSION = "1.0.0";

// Export functions
export $ calculate(x: number): number {
    return x * 2;
}

// Export classes
export class MyClass {
    $ init() {
        // Constructor
    }
}

// Re-export
export imported_function;
```

### Built-in Functions

```thorn
// Output
print("Hello, World!");
print(value);

// Timing
start = clock();
// ... do work ...
elapsed = clock() - start;
```

### Error Handling (Current Pattern)

```thorn
// Currently using runtime errors
// Future: Result type pattern (Issue #38)

// Defensive programming
if (divisor == 0) {
    print("Error: Division by zero");
    return null;
}
result = dividend / divisor;
```

### Comments

```thorn
// Single-line comment

/*
 * Multi-line comment
 * Can span multiple lines
 */
```

### Special Features

```thorn
// Method chaining
result = array
    .filter($(x) => x > 0)
    .map($(x) => x * 2);

// Null coalescing
value = getUserInput() ?? "default";

// String interpolation (using concatenation)
message = "Hello, " + name + "! You are " + age + " years old.";

// Pattern matching with guards
category = match score {
    s if s >= 90 => "Excellent",
    s if s >= 70 => "Good",
    s if s >= 50 => "Pass",
    _ => "Fail",
};
```

### Advanced Syntax Details

#### Array Indexing
```thorn
// Positive indexing
first = array[0];
second = array[1];

// Negative indexing (pending Issue #20)
// last = array[-1];     // Not yet implemented
// second_last = array[-2];  // Not yet implemented

// Out of bounds behavior
// array[100] -> Runtime error: "List index out of bounds"
```

#### Dictionary/Object Methods (pending Issue #16)
```thorn
// Currently dictionaries/objects only support property access
dict = {"key": "value"};
value = dict["key"];

// Planned methods (not yet implemented):
// keys = dict.keys();      // Get all keys
// values = dict.values();  // Get all values
// has = dict.has("key");   // Check if key exists
// dict.remove("key");      // Remove key-value pair
// size = dict.size();      // Get number of entries
```

#### Type Inference Rules
```thorn
// Variables infer type from initialization
x = 42;              // Inferred as number
y = "hello";         // Inferred as string
z = true;            // Inferred as boolean

// Arrays infer element type
nums = [1, 2, 3];    // Inferred as Array[number]
mixed = [1, "two"];  // Inferred as Array[Any]

// Functions infer return type
$ double(x) {
    return x * 2;    // Return type inferred from usage
}
```

#### Operator Precedence (highest to lowest)
```thorn
// 1. Member access, calls
obj.prop, func(), array[index]

// 2. Unary operators
-x, !x

// 3. Exponentiation
x ** y

// 4. Multiplication, division, modulo
x * y, x / y, x % y

// 5. Addition, subtraction
x + y, x - y

// 6. Comparison
<, >, <=, >=

// 7. Equality
==, !=

// 8. Logical AND
&&

// 9. Logical OR
||

// 10. Null coalescing
??

// 11. Assignment
=, +=, -=, *=, /=, %=
```

### Language Limitations and Unsupported Features

#### Not Supported
```thorn
// No string interpolation syntax
// BAD: `Hello ${name}`
// GOOD: "Hello " + name

// No destructuring
// BAD: [a, b] = [1, 2];
// BAD: {name, age} = person;

// No spread operator
// BAD: newArray = [...oldArray, 4, 5];
// BAD: func(...args);

// No async/await
// BAD: async $ fetchData() { await fetch(url); }

// No generators
// BAD: $ *generator() { yield 1; }

// No decorators
// BAD: @decorator class MyClass {}

// No private fields
// BAD: class X { #private = 1; }

// No static methods
// BAD: class X { static $ method() {} }

// No interfaces or abstract classes
// BAD: interface Shape { area(): number; }

// No enums
// BAD: enum Color { RED, GREEN, BLUE }

// No tuples as a distinct type
// BAD: tuple: (string, number) = ("x", 1);

// No optional parameters
// BAD: $ func(required: string, optional?: number) {}

// No default parameters
// BAD: $ func(x: number = 0) {}

// No rest parameters
// BAD: $ func(...rest: Array[Any]) {}
```

#### Edge Cases and Gotchas

```thorn
// Division by zero produces Infinity
result = 1 / 0;  // Infinity
result = -1 / 0; // -Infinity

// Type coercion in operators
"5" + 3;   // "53" (string concatenation)
// "5" - 3;   // Error: Operands must be numbers

// Null handling
null + 5;  // Error: Operands must be numbers
null ?? 5; // 5 (null coalescing works)

// Array bounds
arr = [1, 2, 3];
arr[3];    // Error: List index out of bounds
arr[-1];   // Error: List index out of bounds (negative not supported yet)

// Property access on primitives
(42).toString;    // Error: Cannot access property on primitive type 'number'
true.valueOf;     // Error: Cannot access property on primitive type 'boolean'

// Function without explicit return
$ noReturn() {
    x = 5;
}
result = noReturn(); // result is null

// Immutable variable reassignment
@immut PI = 3.14;
// PI = 3.14159;  // Error: Cannot assign to immutable variable

// Class without constructor
class Empty {}
obj = Empty();  // Error: init method required

// Boolean truthiness
if (0) { }      // 0 is truthy (only null and false are falsy)
if ("") { }     // Empty string is truthy
if ([]) { }     // Empty array is truthy
if ({}) { }     // Empty object is truthy
```

### Type Checking Behavior

```thorn
// Type annotations are checked at runtime
x: number = "string";  // Runtime error when executed

// Generic type parameters are checked
arr: Array[number] = [1, 2, "three"];  // Runtime error

// Function types validate signature
fn: Function[(number), string] = $(x) => x;  // Error if returns number

// Class types check inheritance
class Animal {}
class Dog {}  // Note: No inheritance syntax yet
animal: Animal = Dog();  // Error: Type mismatch
```

### Performance Considerations

```thorn
// Use --vm flag for better performance on compute-heavy code
// Tree-walker (default): Better for I/O, simple scripts
// Bytecode VM (--vm): Better for loops, math, recursive functions

// Efficient patterns
sum = 0;
for (i = 0; i < 1000000; i += 1) {  // Use += instead of i = i + 1
    sum += i;  // Compound assignment is optimized
}

// String building (concatenation is optimized but still O(n))
result = "";
for (s in strings) {
    result = result + s;  // Consider accumulating in array then joining
}
```

## Architecture Overview

### Core Components
- **Scanner/Lexer**: Tokenizes Thorn source code with unique syntax support
- **Parser**: Recursive descent parser building AST with error recovery
- **Dual Execution Models**:
  - **Tree-walking Interpreter**: Direct AST evaluation (default)
  - **Bytecode VM**: Register-based virtual machine for performance

### Key Source Files
- `src/com/thorn/Thorn.java` - Main entry point and CLI handling
- `src/com/thorn/Scanner.java` - Lexical analysis
- `src/com/thorn/Parser.java` - Syntax analysis and AST building
- `src/com/thorn/Interpreter.java` - Tree-walking interpreter
- `src/com/thorn/vm/ThornVM.java` - Bytecode virtual machine
- `src/com/thorn/vm/SimpleCompiler.java` - AST to bytecode compiler

### Performance Optimizations
- Return value optimization (eliminated exception-based returns)
- Variable access caching
- Arithmetic fast paths
- String concatenation optimization
- Array method efficiency

## Project Structure
- `src/` - Java source code (core implementation)
- `examples/` - Example Thorn programs for testing and demonstration
- `benchmarks/` - Performance benchmarks comparing against Python, JavaScript, Ruby
- `tree-sitter-thorn/` - Tree-sitter grammar for VSCode syntax highlighting
- `arch/` - Architecture documentation for future development

## Current Status
- **Feature Complete**: Core language features implemented in both interpreter and VM
- **Performance Competitive**: 2.4x slower than Python (was 7.4x before optimization)
- **VM Feature Parity**: Both execution models support all language features
- **Standard Library**: Minimal (only collections.thorn stub exists)
- **Module System**: Infrastructure complete, needs library modules

## Development Workflow
1. Write Thorn code in `.thorn` files
2. Build with `./build.sh`
3. Test with example programs or benchmarks
4. Use `--ast` flag for debugging parser issues
5. Use `--vm` flag to test bytecode execution
6. Run benchmarks to verify performance impact of changes

## Future Architecture Plans
- Static type system migration (comprehensive 16-week plan documented)
- Standard library reconstruction (detailed module specifications)
- Advanced features like generics and compile-time type checking

## Git Commit Naming Convention
- Use the format "gh-issuenumber: one line description"
  - Example: `gh-42: add pattern matching support`
  - Ensures clear tracking of issue-related work
  - Helps in tracing branch purpose and corresponding GitHub issue
  