# ThornLang v0.2.0 Release Notes

## 🎉 Overview
ThornLang v0.2.0 marks a significant milestone with the completion of all planned features for this release. This version introduces powerful new language features including the Result type for error handling, array slice notation, and numerous improvements to the type system and developer experience.

## ✨ New Features

### Result Type Error Handling (#38)
- Introduces `Result[T, E]` type with `Ok(value)` and `Error(error)` variants
- Pattern matching support: `match (result) { Ok(v) => ..., Error(e) => ... }`
- Helper methods: `is_ok()`, `is_error()`, `unwrap()`, `unwrap_or(default)`
- Special division by zero handling in Result context

### Array Slice Notation (#20)
- Python-style slice syntax: `array[start:end]`
- Negative indexing support: `array[-1]` for last element
- Slice assignment: `array[1:3] = [10, 20]`
- String slicing: `"hello"[1:4]` → `"ell"`

### Type System Enhancements
- **Function Types with Parameters** (#12): `Function[(string, number), boolean]`
- **Class Type Hints** (#14): `user: Person = Person("Alice", 25)`
- **Dictionary Methods** (#16): `.keys()`, `.values()`, `.has()`, `.get()`, `.set()`
- **Array Methods**: `.slice()` method with negative index support

### Developer Experience
- **Improved Error Messages** (#18): Clear, actionable error messages with available methods/properties
- **Global Functions in Modules** (#29): `print()` and `clock()` available in all module scopes
- **Type Checking**: Proper validation for function signatures and class types

## 🐛 Bug Fixes
- Fixed unchecked/unsafe operations warnings during build (#28)
- Fixed VM variable assignment issues with method calls (#17)
- Fixed VM failures with invalid index operations (#19)
- Fixed type hints in block lambdas (#21)
- Fixed NPE when importing non-existent functions (#6)
- Fixed exports of non-direct functions (#9)
- Fixed object literal syntax parsing (#15)

## 🚀 Performance
- Build process optimized with proper dependency ordering
- Both interpreter and VM fully support all new features
- Clean builds with no warnings

## 📋 Migration Guide
No breaking changes in this release. All existing ThornLang code will continue to work.

## 📦 Installation
Download `thornlang-v0.2.0.jar` and run:
```bash
java -jar thornlang-v0.2.0.jar script.thorn
```

## 🔮 What's Next
Planning for v0.3.0 is underway, focusing on:
- Standard library expansion
- Advanced type system features
- Performance optimizations
- Additional developer tools

## 👏 Contributors
Thanks to all contributors who made this release possible!

---
*Full changelog and documentation available at https://github.com/ThornLang/JavaThorn*