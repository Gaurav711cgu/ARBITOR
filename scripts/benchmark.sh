#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_OUT="$ROOT_DIR/out/classes"
TEST_OUT="$ROOT_DIR/out/test-classes"

"$ROOT_DIR/scripts/test.sh"

javac --release 21 -cp "$MAIN_OUT" -d "$TEST_OUT" "$ROOT_DIR/src/test/java/com/arbiter/BenchmarkRunner.java"
java -cp "$MAIN_OUT:$TEST_OUT" com.arbiter.BenchmarkRunner

