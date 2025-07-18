#!/bin/bash
for i in {1..110}; do
  curl -s http://localhost:8095/error > /dev/null
  echo "Generated error $i"
done
