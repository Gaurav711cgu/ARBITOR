#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_OUT="$ROOT_DIR/out/classes"
TEST_OUT="$ROOT_DIR/out/test-classes"

"$ROOT_DIR/scripts/build.sh"

rm -rf "$TEST_OUT"
mkdir -p "$TEST_OUT"

javac --release 21 -cp "$MAIN_OUT" -d "$TEST_OUT" $(find "$ROOT_DIR/src/test/java" -name '*.java' | sort)
java -cp "$MAIN_OUT:$TEST_OUT" com.arbiter.ArbiterTestRunner

