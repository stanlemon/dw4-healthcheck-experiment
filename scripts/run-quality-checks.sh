#!/bin/bash

# Run all quality checks sequentially
# Usage: ./scripts/run-quality-checks.sh [--skip-sonar] [--fix-formatting]

set -e  # Exit on error

echo ""
echo "=== Step 1: Code Formatting (Spotless) ==="
if [[ "$*" == *"--fix-formatting"* ]]; then
    mvn spotless:apply
else
    mvn spotless:check
fi

echo ""
echo "=== Step 2: Static Analysis (SpotBugs) ==="
mvn clean compile spotbugs:check

echo ""
echo "=== Step 3: Tests with Coverage ==="
mvn test jacoco:report

if [[ "$*" != *"--skip-sonar"* ]] && [ -n "$SONAR_TOKEN" ]; then
    echo ""
    echo "=== Step 4: SonarQube Analysis ==="
    mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN
else
    echo ""
    echo "=== Step 4: SonarQube Analysis (skipped) ==="
fi

echo ""
echo "✓ All checks passed!"
