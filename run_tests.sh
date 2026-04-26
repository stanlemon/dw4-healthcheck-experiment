#!/usr/bin/env bash
set -euo pipefail

echo "Running all tests..."
mvn clean test

echo ""
echo "Test summary:"
# Surefire writes one directory per module.
find . -path '*/target/surefire-reports/*.txt' -not -path '*/node_modules/*' \
  -exec grep -H "Tests run:" {} \; | grep -v "Tests run: 0,"
