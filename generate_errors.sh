#!/bin/bash
for i in {1..10}; do
  curl -s http://localhost:8097/error > /dev/null
  echo "Generated error $i"
done
