#!/usr/bin/env bash
set -euo pipefail

# This heuristic flags credential literals for review, not every kind of secret.
# Print only tracked source filenames so a finding cannot expose its value in CI.
pattern="(^|[^[:alnum:]_])(password|secret|key|token)[[:space:]]*[:=][[:space:]]*['\"][^'\"]{8,}"
if git grep -I -i -l -E "$pattern" -- ':(glob)**/src/main/**/*.java'; then
  echo "Potential hardcoded credential literals found in the files above." >&2
  exit 1
else
  status=$?
  if [[ "$status" -ne 1 ]]; then
    echo "Credential literal scan failed (git grep exit $status)." >&2
    exit "$status"
  fi
fi

echo "No potential hardcoded credential literals found in tracked main Java sources."
