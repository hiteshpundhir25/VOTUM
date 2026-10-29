#!/bin/bash
# VOTUM Compilation Script
set -e

# Change to project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "=========================================="
echo "Compiling VOTUM Java Application..."
echo "=========================================="

mkdir -p bin
javac -Xlint:all -d bin $(find src -name "*.java")

if [ $? -eq 0 ]; then
    echo "Compilation successful! Class files generated in bin/"
else
    echo "Compilation failed. Please check errors above."
    exit 1
fi

