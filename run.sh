#!/usr/bin/env bash
set -e

PORT="${1:-8080}"

# Auto-detect JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

# Compile if bin directory is missing or empty
if [ ! -d "bin" ] || [ -z "$(ls -A bin 2>/dev/null)" ]; then
    echo "Compiled classes not found. Running compile.sh first..."
    ./compile.sh
fi

echo "=========================================================="
echo "  Starting Thapar University Dispensary Management Server"
echo "  URL: http://localhost:$PORT"
echo "=========================================================="

exec "$JAVA" -cp bin com.college.dispensary.Main "$PORT"
