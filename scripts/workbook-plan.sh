#!/bin/sh

set -eu

if [ "$#" -lt 1 ]; then
  printf '%s\n' "Usage: mise run back-workbook-plan -- <path-to-workbook.xlsx> [--planner greedy|brute-force] [--seed N] [--restarts N] [--max-states N|unlimited] [--max-seconds S|unlimited] [--top N] [--exhaustive] [--format json|text|xlsx] [--output <path.xlsx>]" >&2
  exit 1
fi

stdout_file="$(mktemp /tmp/opencode/workbook-plan-stdout.XXXXXX)"
stderr_file="$(mktemp /tmp/opencode/workbook-plan-stderr.XXXXXX)"
tail_pid=""

cleanup() {
  if [ -n "$tail_pid" ] && kill -0 "$tail_pid" 2>/dev/null; then
    kill "$tail_pid" 2>/dev/null || true
    wait "$tail_pid" 2>/dev/null || true
  fi
  rm -f "$stdout_file" "$stderr_file"
}

trap cleanup EXIT INT TERM

touch "$stderr_file"
tail -f "$stderr_file" >&2 &
tail_pid=$!

exec_args=""
for arg in "$@"; do
  escaped_arg=$(printf "%s" "$arg" | sed "s/'/'\\''/g")
  exec_args="$exec_args '$escaped_arg'"
done
exec_args="$exec_args '--include-planning-score' '--include-planning-output'"

if sh "./carpool-back/mvnw" -f "./carpool-back/pom.xml" -q exec:java \
  -Dexec.mainClass=com.carpool.workbook.normalization.WorkbookStatsCli \
  -Dexec.args="$exec_args" \
  >"$stdout_file" 2>>"$stderr_file"
then
  if [ -n "$tail_pid" ] && kill -0 "$tail_pid" 2>/dev/null; then
    kill "$tail_pid" 2>/dev/null || true
    wait "$tail_pid" 2>/dev/null || true
    tail_pid=""
  fi
  if ! grep -v 'Log4j API could not find a logging provider\|Skipped invalid entry /xl/theme/theme1.xml' "$stdout_file"
  then
    cat "$stdout_file" >&2
    exit 1
  fi
else
  if [ -n "$tail_pid" ] && kill -0 "$tail_pid" 2>/dev/null; then
    kill "$tail_pid" 2>/dev/null || true
    wait "$tail_pid" 2>/dev/null || true
    tail_pid=""
  fi
  if [ -f "$stderr_file" ]; then
    cat "$stderr_file" >&2
  fi
  exit 1
fi
