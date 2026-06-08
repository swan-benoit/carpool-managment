#!/bin/sh

set -eu

if [ "$#" -lt 1 ]; then
  printf '%s\n' "Usage: mise run back-workbook-stats -- <path-to-workbook.xlsx> [--format json|text]" >&2
  exit 1
fi

stdout_file="$(mktemp /tmp/opencode/workbook-stats-stdout.XXXXXX)"
stderr_file="$(mktemp /tmp/opencode/workbook-stats-stderr.XXXXXX)"

cleanup() {
  rm -f "$stdout_file" "$stderr_file"
}

trap cleanup EXIT INT TERM

exec_args=""
for arg in "$@"; do
  escaped_arg=$(printf "%s" "$arg" | sed "s/'/'\\''/g")
  exec_args="$exec_args '$escaped_arg'"
done

if sh "./carpool-back/mvnw" -f "./carpool-back/pom.xml" -q exec:java \
  -Dexec.mainClass=com.carpool.workbook.normalization.WorkbookStatsCli \
  -Dexec.args="$exec_args" \
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
