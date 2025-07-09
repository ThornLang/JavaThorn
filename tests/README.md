# PR-Specific Tests

This directory contains test cases specific to individual PRs/issues. These tests provide focused validation for each fix or feature.

## How It Works

### 1. Creating Tests
When working on an issue, create a test directory:
```bash
# Using the helper script
./scripts/create-pr-tests.sh <issue-number> [description]

# Or manually
mkdir tests/gh-<issue-number>
```

### 2. Adding Test Cases
Add `.thorn` files to your test directory:
```
tests/gh-29/
├── basic_functionality.thorn
├── edge_cases.thorn
├── error_conditions.thorn
└── integration_test.thorn
```

### 3. Automatic Testing
- Tests run automatically when PR is opened/updated
- Both interpreter and VM modes are tested
- 30-second timeout per test
- Detailed error output on failures

### 4. Automatic Cleanup
- Test directories are automatically removed when PR is merged
- No manual cleanup needed
- Keeps repository clean

## Test Guidelines

### Good Test Practices
✅ **Descriptive names**: `basic_print_in_module.thorn`  
✅ **Complete programs**: Tests should be runnable standalone  
✅ **Focused scope**: One test per specific scenario  
✅ **Expected output**: Use `print()` to verify behavior  
✅ **Edge cases**: Test boundary conditions and error cases  

### Test Categories
- **Basic functionality**: Core feature working correctly
- **Edge cases**: Boundary conditions, unusual inputs
- **Error conditions**: Proper error handling and messages
- **Integration**: Works with other language features
- **Regression**: Prevents old bugs from returning

### Example Test Structure
```thorn
// Test: Basic functionality description
// Expected: What should happen

// Setup
testValue = 42;

// Action
result = someFunction(testValue);

// Verification
print("Result: " + result);
if (result == expectedValue) {
    print("✅ Test passed");
} else {
    print("❌ Test failed");
}
```

## Workflow Integration

### PR Tests Workflow (`.github/workflows/pr-tests.yml`)
- Triggered on PR open/update
- Extracts issue number from PR title (`gh-XX: description`)
- Runs all `.thorn` files in `tests/gh-XX/`
- Tests both interpreter and VM modes
- Reports results in PR checks

### Cleanup Workflow (`.github/workflows/cleanup-pr-tests.yml`)
- Triggered when PR is merged
- Removes `tests/gh-XX/` directory
- Commits cleanup automatically
- Keeps repository clean

## Directory Structure

```
tests/
├── README.md (this file)
├── gh-29/              # Issue #29 tests
│   ├── basic_test.thorn
│   ├── edge_cases.thorn
│   └── README.md
├── gh-30/              # Issue #30 tests
│   └── ...
└── scripts/
    └── create-pr-tests.sh
```

## Benefits

1. **PR Review**: Reviewers can see exactly what scenarios are tested
2. **Regression Prevention**: Specific tests prevent old bugs from returning
3. **Documentation**: Tests serve as examples of the fix in action
4. **Automation**: No manual test management needed
5. **Clean Repository**: Automatic cleanup prevents test accumulation

## Commands

```bash
# Create new test directory
./scripts/create-pr-tests.sh 29 "global functions in modules"

# Run tests locally (if desired)
cd tests/gh-29
java com.thorn.Thorn test_file.thorn
java com.thorn.Thorn test_file.thorn --vm

# Tests run automatically in CI - no manual intervention needed
```

This system provides comprehensive, automated testing for each PR while keeping the repository clean and organized.