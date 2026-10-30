#!/bin/bash
# VOTUM Application Launcher Script
set -e

# Change to project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "=========================================="
echo "Starting VOTUM — Campus Election System..."
echo "=========================================="

mkdir -p bin
find src -name "*.class" -delete 2>/dev/null || true
javac -d bin $(find src -name "*.java")

if [ $? -eq 0 ]; then
    java -cp bin com.votum.Main "$@"
else
    echo "Compilation failed. Aborting run."
    exit 1
fi

