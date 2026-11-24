#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
fixture=$(mktemp -d)
trap 'rm -rf -- "$fixture"' EXIT
cd "$fixture"
git init -q

expect_success() {
  if ! bash "$script_dir/check-hardcoded-secrets.sh" > scan.log 2>&1; then
    echo "Expected a successful scan: $1" >&2
    exit 1
  fi
}

expect_success 'no matching source files'
mkdir -p dw5-app/src/main/java/example dw5-app/src/test/java/example
printf '%s\n' 'class Example { String title = "ordinary text"; }' > dw5-app/src/main/java/example/Example.java
printf '%s\n' 'class ExampleTest { String password = "fixture-password"; }' > dw5-app/src/test/java/example/ExampleTest.java
git add dw5-app/src/main/java/example/Example.java dw5-app/src/test/java/example/ExampleTest.java
expect_success 'safe main sources and excluded test fixtures'

printf '%s\n' 'class Untracked { String token = "untracked-token"; }' > dw5-app/src/main/java/example/Untracked.java
expect_success 'untracked source files'

for quote in '"' "'"; do
  printf 'class Example { String password = %sdo-not-print-this-secret%s; }\n' "$quote" "$quote" > dw5-app/src/main/java/example/Example.java
  if bash "$script_dir/check-hardcoded-secrets.sh" > scan.log 2>&1; then
    echo 'Expected a credential finding to fail.' >&2
    exit 1
  else
    status=$?
    [[ "$status" -eq 1 ]]
  fi
  grep -Fq 'dw5-app/src/main/java/example/Example.java' scan.log
  if grep -Fq 'do-not-print-this-secret' scan.log; then
    echo 'Credential value was exposed in scan output.' >&2
    exit 1
  fi
done

mkdir outside-repository
mv .git outside-repository/git-directory
if bash "$script_dir/check-hardcoded-secrets.sh" > scan.log 2>&1; then
  echo 'Expected a git error to fail the scan.' >&2
  exit 1
else
  status=$?
  [[ "$status" -ne 1 ]]
fi
grep -Fq 'Credential literal scan failed' scan.log
printf '%s\n' 'Credential scan fixtures passed.'
