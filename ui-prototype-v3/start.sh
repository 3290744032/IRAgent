#!/bin/bash
echo "========================================"
echo "  IRAgent v3 UI Prototype"
echo "========================================"
echo ""
echo "Starting local server..."
echo "Open http://localhost:3000 in browser"
echo "Press Ctrl+C to stop"
echo ""

cd "$(dirname "$0")"

if command -v npx &> /dev/null; then
    npx serve . -p 3000 --no-clipboard
elif command -v python3 &> /dev/null; then
    python3 -m http.server 8080
elif command -v python &> /dev/null; then
    python -m http.server 8080
else
    echo "ERROR: Please install Node.js (npx serve) or Python"
    exit 1
fi
