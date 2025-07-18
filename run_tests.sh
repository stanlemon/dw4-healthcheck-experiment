#!/bin/zsh

echo "Running all tests..."
mvn clean test

echo "\nTest summary:"
find target/surefire-reports -name "*.txt" -exec grep "Tests run:" {} \;
