#!/bin/bash

# Script to generate errors for testing the health check system
# Usage: ./generate_errors.sh [number_of_errors]
# Default: 15 errors

# Set default number of errors to 15, or use the first argument if provided
NUM_ERRORS=${1:-15}

# Validate that the parameter is a positive integer
if ! [[ "$NUM_ERRORS" =~ ^[1-9][0-9]*$ ]]; then
    echo "Error: Please provide a positive integer for the number of errors"
    echo "Usage: $0 [number_of_errors]"
    echo "Example: $0 25"
    exit 1
fi

# Check if server is running before attempting to generate errors
echo "Checking if server is running..."
if ! curl -s --connect-timeout 5 http://localhost:8097/hello > /dev/null 2>&1; then
    echo "Error: Server is not running on localhost:8097"
    echo "Please start the server first:"
    echo "  mvn clean compile exec:java"
    echo "  OR"
    echo "  java -jar target/dw-test2-1.0-SNAPSHOT.jar server config.yml"
    exit 1
fi
echo "Server is running ✓"

echo "Generating $NUM_ERRORS errors..."

# Generate the specified number of errors
for ((i=1; i<=NUM_ERRORS; i++)); do
    curl -s http://localhost:8097/error > /dev/null
    echo "Generated error $i/$NUM_ERRORS"
done

echo "Completed generating $NUM_ERRORS errors"
echo "Check metrics at: http://localhost:8097/metrics"
echo "Check health at: http://localhost:8098/healthcheck"
