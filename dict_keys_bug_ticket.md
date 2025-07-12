# Bug Report: Dictionary Literal Syntax Only Accepts String Keys

## Issue Summary
Dictionary literals in ThornLang are hardcoded to only accept string literals as keys, despite the type system properly supporting `Dict[K, V]` with any key type `K`.

## Problem Description
The parser restricts dictionary literal syntax `{key: value}` to only accept string literals as keys, even though:
1. The type system supports `Dict[number, V]`, `Dict[boolean, V]`, etc.
2. Type checking properly validates any key type at runtime
3. The underlying Java `HashMap` supports any object as keys

## Current Behavior
```thorn
// ✅ Works - string keys
stringDict: Dict[string, number] = {"key1": 1, "key2": 2};

// ❌ Fails - parser error "Expected string key in dictionary"
numberDict: Dict[number, string] = {1: "one", 2: "two"};  
boolDict: Dict[boolean, string] = {true: "yes", false: "no"};
```

## Expected Behavior
All of the above should work, allowing any expression that evaluates to the correct key type.

## Root Cause
In `src/com/thorn/Parser.java` lines 698-701:
```java
if (match(STRING)) {
    keys.add(new Expr.Literal(previous().literal));
} else {
    throw error(peek(), "Expected string key in dictionary.");
}
```

The parser explicitly checks for `STRING` tokens and rejects anything else.

## Technical Details

### Location
- **File**: `src/com/thorn/Parser.java`
- **Method**: `primary()`
- **Lines**: 698-701

### Impact
- **Severity**: Medium
- **Area**: Parser, Dictionary literals
- **Affects**: Both interpreter and VM modes

### Type System Support
The type checking in `ThornType.java` already properly supports this:
```java
case "Dict":
    // ... 
    ThornType keyType = (ThornType) typeArgs.get(0);
    ThornType valueType = (ThornType) typeArgs.get(1);
    
    for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
        if (!keyType.matches(entry.getKey())) return false;
        if (!valueType.matches(entry.getValue())) return false;
    }
```

## Suggested Fix
Replace the hardcoded string check with expression parsing:
```java
// Instead of:
if (match(STRING)) {
    keys.add(new Expr.Literal(previous().literal));
} else {
    throw error(peek(), "Expected string key in dictionary.");
}

// Use:
keys.add(expression());  // Allow any expression as key
```

## Test Cases
Once fixed, these should all work:
```thorn
% UserId = number;
% UserData = Dict[string, Any];

// Number keys
scores: Dict[number, string] = {1: "first", 2: "second"};

// Boolean keys  
flags: Dict[boolean, string] = {true: "enabled", false: "disabled"};

// Variable keys
userId: UserId = 1001;
userMap: Dict[UserId, UserData] = {userId: {"name": "Alice"}};

// Expression keys
calculated: Dict[number, string] = {(5 * 2): "ten", (3 + 4): "seven"};
```

## Workaround
Currently, the only workaround is to use string keys and convert:
```thorn
// Current workaround - less type-safe
numberDict: Dict[string, string] = {"1": "one", "2": "two"};
```

## Related Issues
- Type aliases with `Dict[K, V]` where `K != string` cannot be used with literal syntax
- Inconsistency between type system capabilities and parser restrictions
- Reduced expressiveness of the language

## Priority
Medium - This limits the expressiveness of dictionaries and creates inconsistency between the type system and syntax.