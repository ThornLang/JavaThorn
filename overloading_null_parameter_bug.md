# Bug: Function Overloading with Null Parameters

## Description
When using function overloading with a 2-parameter version that calls a 3-parameter version passing `null`, type checking incorrectly reports an error about null not being a valid string, even though the 3-parameter function properly handles null values.

## Example
```thorn
// In stdlib/test.thorn:
$ assert_equals(expected: Any, actual: Any, message: string): void {
    if (expected != actual) {
        errorMsg: string = "Expected " + _format_value(expected) + " but was " + _format_value(actual);
        if (message != null) {  // Properly handles null
            errorMsg = errorMsg + ": " + message;
        }
        _fail(errorMsg);
    }
}

$ assert_equals(expected: Any, actual: Any): void {
    assert_equals(expected, actual, null);  // Passes null for message
}

// In test file:
assert_equals(95, scores["Math"]);  // This fails with: Type error: expected string but got null for parameter 'message'
```

## Expected Behavior
The 2-parameter overload should be selected and work correctly, passing null to the 3-parameter version.

## Actual Behavior
Type error is thrown claiming null is not a valid string for the message parameter, even though:
1. The 2-parameter overload should be selected
2. The 3-parameter version properly handles null values

## Reproduction
Run: `java com.thorn.Thorn tests/gh-65/type_alias_stdlib_integration_test.thorn`

## Possible Causes
1. Overload resolution may not be working correctly with imported functions
2. Type checking may be happening before overload resolution
3. Null handling in overloaded functions may have a bug

## Workaround
Explicitly provide all parameters including the message:
```thorn
assert_equals(95, scores["Math"], "Math score should be 95");
```