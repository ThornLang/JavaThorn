# Contributing to ThornLang

First off, thank you for considering contributing to ThornLang! It's people like you that make ThornLang such a great tool.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Process](#development-process)
- [Style Guidelines](#style-guidelines)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Project Structure](#project-structure)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please be respectful and constructive in all interactions.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/JavaThorn.git
   cd JavaThorn
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ThornLang/JavaThorn.git
   ```
4. **Build the project**:
   ```bash
   ./scripts/build.sh
   ```
5. **Run tests**:
   ```bash
   ./scripts/test.sh
   ```

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When you create a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the behavior
- **Expected behavior**
- **Actual behavior**
- **Code samples** (if applicable)
- **Environment details** (OS, Java version)

Example:
```markdown
**Title**: Parser fails on nested object literals in function calls

**Steps to reproduce**:
1. Create file with: `foo({"key": {"nested": "value"}})`
2. Run: `java com.thorn.Thorn test.thorn`

**Expected**: Should parse correctly
**Actual**: Parser error: unexpected token

**Environment**: macOS 13.0, Java 17
```

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title and description**
- **Use case** - why is this needed?
- **Proposed solution**
- **Alternative solutions** you've considered
- **Additional context**

### Your First Code Contribution

Unsure where to begin? Look for these labels:

- `good first issue` - Simple issues perfect for beginners
- `help wanted` - Issues where we need community help
- `documentation` - Documentation improvements

### Pull Requests

1. **Follow the style guidelines**
2. **Include tests** for new features
3. **Update documentation** as needed
4. **Ensure all tests pass**
5. **Write clear commit messages**

## Development Process

### 1. Setting Up Your Development Environment

```bash
# Required tools
- Java 11 or higher
- Git
- A text editor or IDE (IntelliJ IDEA recommended)

# Optional tools
- Maven (for future build system)
- VSCode with Java extensions
```

### 2. Making Changes

1. **Create a feature branch**:
   ```bash
   git checkout -b gh-ISSUE_NUMBER-brief-description
   # Example: git checkout -b gh-42-add-string-interpolation
   ```

2. **Make your changes**:
   - Write clean, readable code
   - Add tests for new functionality
   - Update relevant documentation

3. **Test your changes**:
   ```bash
   # Run all tests
   ./scripts/test.sh
   
   # Run specific test
   java com.thorn.Thorn tests/your-test.thorn
   
   # Test with VM mode
   java com.thorn.Thorn tests/your-test.thorn --vm
   ```

### 3. Before Submitting

- [ ] Run the full test suite
- [ ] Test both interpreter and VM modes
- [ ] Update CLAUDE.md if adding new syntax
- [ ] Add examples if introducing new features

## Style Guidelines

### Java Code Style

```java
// Package statement and imports
package com.thorn;

import java.util.*;

// Class documentation
/**
 * Brief description of the class.
 * 
 * Longer description if needed.
 */
public class Example {
    // Constants first
    private static final int MAX_SIZE = 100;
    
    // Instance variables
    private String name;
    private int value;
    
    // Constructor
    public Example(String name) {
        this.name = name;
        this.value = 0;
    }
    
    // Methods with clear names
    public void processData(List<String> data) {
        // Use meaningful variable names
        for (String item : data) {
            // Comment complex logic
            if (isValid(item)) {
                handleItem(item);
            }
        }
    }
    
    // Helper methods private
    private boolean isValid(String item) {
        return item != null && !item.isEmpty();
    }
}
```

### ThornLang Code Style

```thorn
// Use descriptive names
$ calculateTotal(items: Array[Item]): number {
    total = 0;
    
    // Clear logic flow
    for (item in items) {
        if (item.isActive) {
            total += item.price;
        }
    }
    
    return total;
}

// Class naming: PascalCase
class ShoppingCart {
    $ init() {
        items = [];
        discount = 0;
    }
    
    // Method naming: camelCase
    $ addItem(item: Item): void {
        this.items.push(item);
    }
}

// Constants: UPPER_CASE
@immut MAX_ITEMS = 100;
@immut DEFAULT_TAX_RATE = 0.08;
```

## Commit Guidelines

### Commit Message Format

```
gh-ISSUE_NUMBER: brief description

Longer explanation if needed. Wrap at 72 characters. Explain what
and why, not how.

- Bullet points for multiple changes
- Keep each point concise
```

### Examples

```bash
# Good
git commit -m "gh-15: fix object literal parsing in function calls"
git commit -m "gh-22: add property initialization without this in constructors"

# Bad
git commit -m "fix bug"
git commit -m "Updated Parser.java"
git commit -m "WIP"
```

### Commit Best Practices

1. **One logical change per commit**
2. **Test before committing**
3. **Reference issue numbers**
4. **Use present tense** ("add" not "added")
5. **Be concise** but descriptive

## Pull Request Process

### 1. Before Creating a PR

- [ ] Sync with upstream main:
  ```bash
  git fetch upstream
  git rebase upstream/main
  ```
- [ ] Resolve any conflicts
- [ ] Ensure all tests pass
- [ ] Review your own changes

### 2. Creating the PR

**Title**: `gh-ISSUE_NUMBER: Brief description`

**Description Template**:
```markdown
## Summary
Brief description of what this PR does.

## Changes
- List of specific changes
- Implementation details if complex

## Testing
- How you tested the changes
- Any edge cases considered

## Related Issues
Fixes #ISSUE_NUMBER
```

### 3. PR Review Process

1. **Automated checks** must pass
2. **Code review** from maintainers
3. **Address feedback** promptly
4. **Keep PR updated** with main branch
5. **Squash commits** if requested

### 4. After Merge

- Delete your feature branch
- Update your local main branch
- Celebrate your contribution! 🎉

## Project Structure

```
JavaThorn/
├── src/                      # Source code
│   └── com/thorn/
│       ├── Thorn.java       # Main entry point
│       ├── Scanner.java     # Lexical analysis
│       ├── Parser.java      # Syntax analysis
│       ├── Interpreter.java # Tree-walking interpreter
│       └── vm/              # Bytecode VM
├── tests/                    # Test files
│   ├── features/            # Feature tests
│   └── regression/          # Regression tests
├── examples/                 # Example programs
├── benchmarks/              # Performance tests
├── scripts/                 # Build and utility scripts
├── wiki/                    # Documentation
└── CLAUDE.md               # AI assistance guidelines
```

### Key Files to Understand

1. **Parser.java** - Recursive descent parser
2. **Interpreter.java** - Tree-walking interpreter
3. **ThornVM.java** - Bytecode virtual machine
4. **ThornType.java** - Type system implementation

## Testing

### Writing Tests

1. **Unit tests** for specific features:
   ```thorn
   // tests/features/test_arrays.thorn
   arr = [1, 2, 3];
   assert(arr.length == 3);
   arr.push(4);
   assert(arr.length == 4);
   ```

2. **Integration tests** for complex scenarios:
   ```thorn
   // tests/integration/test_class_system.thorn
   class Person {
       $ init(name: string) {
           name = name;
       }
   }
   
   p = Person("Alice");
   assert(p.name == "Alice");
   ```

3. **Error tests** for error handling:
   ```thorn
   // tests/errors/test_type_errors.thorn
   // Should fail with type error
   x: number = "string";
   ```

### Running Tests

```bash
# Run all tests
./scripts/test.sh

# Run specific category
./scripts/test.sh features
./scripts/test.sh regression

# Run with coverage (future)
./scripts/test.sh --coverage
```

## Documentation

### Where to Document

1. **Code comments** - Complex logic
2. **CLAUDE.md** - Language syntax changes
3. **Wiki pages** - User-facing documentation
4. **README.md** - Major feature additions
5. **Commit messages** - Change rationale

### Documentation Checklist

- [ ] Public methods have JavaDoc
- [ ] Complex algorithms are explained
- [ ] New syntax is documented
- [ ] Examples demonstrate usage
- [ ] Edge cases are mentioned

## Release Process

1. **Version bumping** (maintainers only)
2. **Changelog update**
3. **Testing on multiple platforms**
4. **Creating release artifacts**
5. **Publishing to GitHub releases**

## Getting Help

- **Discord**: [Join our community](https://discord.gg/thornlang)
- **GitHub Issues**: Technical questions
- **Wiki**: Documentation and guides
- **Email**: maintainers@thornlang.org

## Recognition

Contributors are recognized in:
- The CONTRIBUTORS.md file
- Release notes
- The project website

Thank you for contributing to ThornLang! Your efforts help make this language better for everyone.