#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Building Maven project ==="
cd "$PROJECT_ROOT"
mvn clean package -DskipTests

echo ""
echo "=== Configuring Docker to use minikube's daemon ==="
eval $(minikube docker-env)

echo ""
echo "=== Building dw4-app Docker image ==="
docker build -t dw4-app:latest -f dw4-app/Dockerfile dw4-app/

echo ""
echo "=== Building spring3-app Docker image ==="
docker build -t spring3-app:latest -f spring3-app/Dockerfile spring3-app/

echo ""
echo "=== Images built successfully ==="
docker images | grep -E "dw4-app|spring3-app"
