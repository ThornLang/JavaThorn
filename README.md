<div align="center">

<img src="assets/thorn-logo-text.svg" alt="Thorn" width="300">

### A modern language blending functional elegance with practical simplicity

[![Release](https://img.shields.io/github/v/release/ThornLang/JavaThorn?style=flat-square)](https://github.com/ThornLang/JavaThorn/releases)
[![License](https://img.shields.io/github/license/ThornLang/JavaThorn?style=flat-square)](https://github.com/ThornLang/JavaThorn/blob/main/LICENSE)
[![Issues](https://img.shields.io/github/issues/ThornLang/JavaThorn?style=flat-square)](https://github.com/ThornLang/JavaThorn/issues)
[![Wiki](https://img.shields.io/badge/docs-wiki-blue?style=flat-square)](https://github.com/ThornLang/JavaThorn/wiki)

</div>

## Overview

Thorn is a modern, dynamically-typed programming language that combines functional programming expressiveness with imperative simplicity. It features a clean syntax with unique `$` function definitions, pattern matching, and dual execution modes (tree-walking interpreter and bytecode VM).

## Features

- Dynamic typing with optional type annotations
- Functions as first-class citizens with `$` syntax
- Object-oriented programming with classes
- Pattern matching and null coalescing (`??`)
- Immutable variables with `@immut`
- Dual execution modes: interpreter and VM

## Quick Start

```bash
# Clone and build
git clone https://github.com/ThornLang/JavaThorn.git
cd JavaThorn
./scripts/build.sh

# Run a script
java com.thorn.Thorn script.thorn

# Start REPL
java com.thorn.Thorn
```

## Documentation

Comprehensive documentation is available in the [Wiki](https://github.com/ThornLang/JavaThorn/wiki):

- [Getting Started](https://github.com/ThornLang/JavaThorn/wiki/Getting-Started) - Installation and first program
- [Language Reference](https://github.com/ThornLang/JavaThorn/wiki/Language-Reference) - Complete syntax guide
- [Examples](https://github.com/ThornLang/JavaThorn/wiki/Examples) - Code examples and tutorials
- [Type System](https://github.com/ThornLang/JavaThorn/wiki/Type-System) - Type annotations and checking
- [Performance Guide](https://github.com/ThornLang/JavaThorn/wiki/Performance-Guide) - Optimization tips

## Performance

Thorn offers competitive performance for a dynamic language:

- 2.4x slower than Python (improved from 7.4x)
- Optimized variable access and arithmetic operations
- Choice between interpreter (better for I/O) and VM (better for computation)

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Code style and standards
- Development workflow
- Issue reporting
- Pull request process

## License

Thorn is released under the MIT License. See [LICENSE](LICENSE) for details.