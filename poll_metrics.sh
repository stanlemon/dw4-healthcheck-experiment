#!/bin/bash

# Use the first script argument as sleep time, default to 5 seconds if not provided
SLEEP_TIME=${1:-5}

while true; do
  # Fetch metrics and prepend a timestamp to each line
  curl -s http://localhost:8097/metrics | sed "s/^/[$(date '+%Y-%m-%d %H:%M:%S')] /"
  echo
  # Wait for the configured sleep time before polling again
  sleep "$SLEEP_TIME"
done