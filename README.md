# Thorn Programming Language

A modern, dynamically-typed programming language built in Java, featuring clean syntax and powerful features.

## Features

- **Dynamic typing** with automatic type inference
- **Immutable variables** with `@immut` annotation
- **Functions** with `$` prefix for clear identification
- **Classes** with constructor and method support
- **Lambda expressions** with arrow syntax
- **Pattern matching** with match expressions
- **Null coalescing** operator (`??`)
- **List and dictionary** literals
- **Power operator** (`**`)
- **For-in loops** for easy iteration

## Quick Start

### Building
```bash
javac -d . src/com/thorn/*.java
```

### Running
```bash
# Run a file
java com.thorn.Thorn script.thorn

# Interactive REPL
java com.thorn.Thorn
```

## Syntax Examples

### Variables
```thorn
// Mutable variable
x = 42;

// Immutable variable
@immut PI = 3.14159;
```

### Functions
```thorn
$ greet(name) {
    return "Hello, " + name + "!";
}

// Lambda expression
multiply = $(x, y) => x * y;
```

### Classes
```thorn
class Person {
    $ init(name, age) {
        this.name = name;
        this.age = age;
    }
    
    $ greet() {
        return "Hi, I'm " + this.name;
    }
}

person = Person("Alice", 25);
print(person.greet());
```

### Control Flow
```thorn
// If-else
if (x > 10) {
    print("Large");
} else {
    print("Small");
}

// For-in loop
for (item in [1, 2, 3]) {
    print(item);
}

// While loop
while (x < 10) {
    x = x + 1;
}
```

### Data Structures
```thorn
// Lists
numbers = [1, 2, 3, 4, 5];
first = numbers[0];

// Dictionaries
person = {"name": "Alice", "age": "30"};
name = person["name"];
```

### Advanced Features
```thorn
// Null coalescing
value = maybe_null ?? "default";

// Power operator
squared = 3 ** 2;  // 9

// Pattern matching (planned)
result = match (value) {
    1 => "one",
    2 => "two",
    _ => "other"
};
```

## Implementation Status

✅ Lexer/Scanner
✅ Parser
✅ AST Generation
✅ Basic Interpreter
✅ Variables (mutable/immutable)
✅ Functions and Lambdas
✅ Classes
✅ Control Flow (if/else, while, for-in)
✅ Lists and Dictionaries
✅ Operators (arithmetic, logical, comparison)
✅ Null coalescing

🚧 Pattern matching (partial)
🚧 Module system (import/export)
🚧 Standard library

## Examples

See the `examples/` directory for demonstration scripts:
- `demo.thorn` - General language features
- `class_demo.thorn` - Object-oriented programming
- `hello.thorn` - Simple starter example

## Architecture

The interpreter follows the tree-walk interpreter pattern from "Crafting Interpreters":

1. **Scanner** - Tokenizes source code
2. **Parser** - Builds Abstract Syntax Tree (AST)
3. **Interpreter** - Evaluates the AST

## Future Plans

- Complete pattern matching implementation
- Module system with import/export
- Standard library (file I/O, networking, etc.)
- Performance optimizations
- Better error messages
- Debugger support