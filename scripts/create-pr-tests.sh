#!/bin/bash

# Script to create test directory structure for a new PR

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <issue-number> [description]"
    echo "Example: $0 29 'global functions in modules'"
    exit 1
fi

ISSUE_NUM=$1
DESCRIPTION=${2:-""}

TEST_DIR="tests/gh-$ISSUE_NUM"

if [ -d "$TEST_DIR" ]; then
    echo "❌ Test directory $TEST_DIR already exists"
    exit 1
fi

echo "🏗️  Creating test directory for Issue #$ISSUE_NUM"
mkdir -p "$TEST_DIR"

# Create a basic README for the test directory
cat > "$TEST_DIR/README.md" << EOF
# Tests for Issue #$ISSUE_NUM

${DESCRIPTION:+## Description}
${DESCRIPTION:+$DESCRIPTION}

## Test Files

This directory contains test cases specific to Issue #$ISSUE_NUM. These tests will:
- Run automatically when the PR is opened/updated
- Be cleaned up automatically when the PR is merged
- Test both interpreter and VM modes

## Adding Tests

Create \`.thorn\` files in this directory. Each file should:
- Be a complete, runnable Thorn program
- Test a specific aspect of the fix
- Have descriptive names (e.g., \`basic_functionality.thorn\`, \`edge_cases.thorn\`)

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
EOF

# Create a basic example test file
cat > "$TEST_DIR/basic_test.thorn" << EOF
// Basic test for Issue #$ISSUE_NUM
// TODO: Replace with actual test logic

print("Basic test for Issue #$ISSUE_NUM");
print("This is a placeholder - replace with real test");
EOF

echo "✅ Created test directory structure:"
echo "   📁 $TEST_DIR/"
echo "   📄 $TEST_DIR/README.md"
echo "   📄 $TEST_DIR/basic_test.thorn"
echo ""
echo "Next steps:"
echo "1. Replace basic_test.thorn with your actual test cases"
echo "2. Add more .thorn files as needed"
echo "3. Commit and push - tests will run automatically on PR"
echo "4. Tests will be cleaned up automatically when PR is merged"