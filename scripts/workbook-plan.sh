#!/bin/sh

set -eu

if [ "$#" -lt 1 ]; then
  printf '%s\n' "Usage: mise run back-workbook-plan -- <path-to-workbook.xlsx> [--planner greedy|brute-force] [--max-states N] [--max-seconds S] [--top N] [--format json|text]" >&2
  exit 1
fi

stdout_file="$(mktemp /tmp/opencode/workbook-plan-stdout.XXXXXX)"
stderr_file="$(mktemp /tmp/opencode/workbook-plan-stderr.XXXXXX)"

cleanup() {
  rm -f "$stdout_file" "$stderr_file"
}

trap cleanup EXIT INT TERM

if sh "./carpool-back/mvnw" -f "./carpool-back/pom.xml" -q exec:java \
  -Dexec.mainClass=com.carpool.workbook.normalization.WorkbookStatsCli \
  -Dexec.args="$* --include-planning-score --include-planning-output" \
  >"$stdout_file" 2>"$stderr_file"
then
  if ! grep -v 'Log4j API could not find a logging provider\|Skipped invalid entry /xl/theme/theme1.xml' "$stdout_file"
  then
    cat "$stdout_file" >&2
    exit 1
  fi
else
  cat "$stderr_file" >&2
  exit 1
fi
