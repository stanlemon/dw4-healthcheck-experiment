#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Building Maven project ==="
cd "$PROJECT_ROOT"
# Skip jacoco:check too — without tests it has no coverage data and would fail the threshold.
mvn clean package -DskipTests -Djacoco.skip=true

echo ""
echo "=== Configuring Docker to use minikube's daemon ==="
eval $(minikube docker-env)

echo ""
echo "=== Building dw5-app Docker image ==="
docker build -t dw5-app:latest -f dw5-app/Dockerfile dw5-app/

echo ""
echo "=== Building spring4-app Docker image ==="
docker build -t spring4-app:latest -f spring4-app/Dockerfile spring4-app/

echo ""
echo "=== Images built successfully ==="
docker images | grep -E "dw5-app|spring4-app"
