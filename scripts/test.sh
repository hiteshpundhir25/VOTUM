#!/bin/bash
# VOTUM Automated Test Suite Runner
set -e

# Change to project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "=========================================="
echo "Compiling VOTUM and Verification Suites..."
echo "=========================================="

mkdir -p bin
javac --release 17 -Xlint:all -d bin $(find src -name "*.java")

echo "Running Domain Logic Verification Suite..."
java -cp bin com.votum.test.VotumTestSuite

echo "Running UI & Interaction Smoke Test Suite..."
java -cp bin com.votum.test.UISmokeTestSuite

echo "=========================================="
echo "ALL TESTS PASSED SUCCESSFULLY!"
echo "=========================================="

