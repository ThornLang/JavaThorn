# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## VM Integration
- Don't remove our VM integration ever, we just need to fix it.

## Common Development Commands

### Build and Run
```bash
# Build the project
./build.sh

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

## Language Syntax Notes
- Uses `$` for function definitions
- Supports immutable variables with `@immut`
- Has built-in pattern matching
- Includes null coalescing operator `??`
- Supports lambda functions with concise arrow syntax
- Provides rich control flow mechanisms (if-else, for, while loops)
- Offers class-based object-oriented programming
- Includes comprehensive operator support
- Supports module imports and exports

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
  