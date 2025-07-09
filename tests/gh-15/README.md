# Tests for Issue #15




## Test Files

This directory contains test cases specific to Issue #15. These tests will:
- Run automatically when the PR is opened/updated
- Be cleaned up automatically when the PR is merged
- Test both interpreter and VM modes

## Adding Tests

Create `.thorn` files in this directory. Each file should:
- Be a complete, runnable Thorn program
- Test a specific aspect of the fix
- Have descriptive names (e.g., `basic_functionality.thorn`, `edge_cases.thorn`)

## Test Categories

Consider creating tests for:
- ✅ Basic functionality
- ✅ Edge cases  
- ✅ Error conditions
- ✅ Integration with other features
- ✅ Both interpreter and VM modes

## Notes

- Tests run with a 30-second timeout
- Failed tests will show error output in CI
- Tests are automatically cleaned up after PR merge
