#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-healthy-demo}"
SERVICE="${2:-dw4-app}"
PORT="${3:-8097}"

echo "=== Healthy Demo: Triggering failure on $SERVICE ==="

# Show initial state
echo ""
echo "--- Step 1: Current pod status (all should be Running, Ready) ---"
kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name="$SERVICE" -o wide

# Pick the first pod
POD=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name="$SERVICE" \
  -o jsonpath='{.items[0].metadata.name}')
echo ""
echo "--- Step 2: Targeting pod: $POD ---"

# Port-forward to the specific pod
echo ""
echo "--- Step 3: Port-forwarding to $POD on localhost:9999 ---"
kubectl port-forward -n "$NAMESPACE" "$POD" 9999:"$PORT" &
PF_PID=$!
sleep 2

cleanup() {
  kill "$PF_PID" 2>/dev/null || true
}
trap cleanup EXIT

# Show current health
echo ""
echo "--- Step 4: Current health status (should be 200 healthy) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:9999/health

# Hammer the error endpoint
echo ""
echo "--- Step 5: Triggering 150 errors to breach the 100-error threshold ---"
for i in $(seq 1 150); do
  curl -s http://localhost:9999/test-errors/trigger > /dev/null 2>&1
  if (( i % 25 == 0 )); then
    echo "  Sent $i errors..."
  fi
done

# Verify the pod is now unhealthy
echo ""
echo "--- Step 6: Health status after errors (should be 503 unhealthy) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:9999/health

# Stop port-forward before pod restarts
cleanup
trap - EXIT

# Watch the pod lifecycle
echo ""
echo "--- Step 7: Watching pod status (readiness fails ~10s, restart ~30s) ---"
echo "    Press Ctrl+C to stop watching"
echo ""
kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name="$SERVICE" -w
