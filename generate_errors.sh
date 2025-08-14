#!/bin/bash

#!/bin/bash

# Script to generate errors and latency for testing the health check system
# Usage: ./generate_errors.sh [number_of_errors] [latency_percentage]
# Default: 15 errors, 30% of requests will have latency
# Latency percentage can exceed 100% for aggressive testing
# Examples:
#   ./generate_errors.sh 25        # 25 errors, 30% latency
#   ./generate_errors.sh 25 50     # 25 errors, 50% latency
#   ./generate_errors.sh 25 200    # 25 errors, 200% latency (2x latency requests)
#   ./generate_errors.sh 10 500    # 10 errors, 500% latency (5x latency requests)
#   ./generate_errors.sh 25 50 true   # 25 errors, 50% EGREGIOUS latency (high delays)
#   ./generate_errors.sh 20 100 egregious # 20 errors, 100% egregious latency

# Set default number of errors to 15, or use the first argument if provided
NUM_ERRORS=${1:-15}

# Set default latency percentage to 30%, or use the second argument if provided
LATENCY_PERCENTAGE=${2:-30}

# Set egregious latency mode - can be "true", "egregious", "yes", or any non-empty value
EGREGIOUS_MODE=${3:-false}

# Validate that the first parameter is a positive integer
if ! [[ "$NUM_ERRORS" =~ ^[1-9][0-9]*$ ]]; then
    echo "Error: Please provide a positive integer for the number of errors"
    echo "Usage: $0 [number_of_errors] [latency_percentage]"
    echo "Example: $0 25 50"
    exit 1
fi

# Validate that the latency percentage is a non-negative integer (can exceed 100 for aggressive testing)
if ! [[ "$LATENCY_PERCENTAGE" =~ ^[0-9]+$ ]] || [ "$LATENCY_PERCENTAGE" -lt 0 ]; then
    echo "Error: Latency percentage must be a non-negative integer"
    echo "Usage: $0 [number_of_errors] [latency_percentage]"
    echo "Example: $0 25 50   (normal: 50% of errors get latency)"
    echo "Example: $0 25 200  (aggressive: 200% means 2x latency requests per error)"
    echo "Example: $0 25 500  (egregious: 500% means 5x latency requests per error)"
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

echo "Generating $NUM_ERRORS errors with $LATENCY_PERCENTAGE% latency requests..."

# Calculate how many requests should have latency
NUM_LATENCY_REQUESTS=$(( (NUM_ERRORS * LATENCY_PERCENTAGE) / 100 ))

# Arrays of possible slow delays (in milliseconds)
# Includes aggressive delays to easily trigger latency thresholds
SLOW_DELAYS=(600 800 1000 1200 1500 2000 3000 4000 5000 7000 10000)

# Generate the specified number of errors
for ((i=1; i<=NUM_ERRORS; i++)); do
    # Determine if this request should have latency
    if [ $i -le $NUM_LATENCY_REQUESTS ]; then
        # Generate a slow request that will also cause an error
        # Pick a random delay from the array
        DELAY=${SLOW_DELAYS[$RANDOM % ${#SLOW_DELAYS[@]}]}
        echo "Generating slow error $i/$NUM_ERRORS (${DELAY}ms delay)"

        # First make a slow request to add latency
        curl -s http://localhost:8097/slow/$DELAY > /dev/null

        # Then make an error request
        curl -s http://localhost:8097/error > /dev/null
    else
        # Generate a regular error without latency
        curl -s http://localhost:8097/error > /dev/null
        echo "Generated error $i/$NUM_ERRORS"
    fi
done

echo "Completed generating $NUM_ERRORS errors"
if [ $NUM_LATENCY_REQUESTS -gt 0 ]; then
    echo "Generated $NUM_LATENCY_REQUESTS slow requests (${LATENCY_PERCENTAGE}%)"
fi
echo "Check metrics at: http://localhost:8097/metrics"
echo "Check health at: http://localhost:8098/healthcheck"
