#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "  Compiling Thapar University Dispensary Management System"
echo "=========================================================="

# Auto-detect JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
else
    JAVAC="javac"
fi

mkdir -p bin

echo "Using javac: $($JAVAC -version 2>&1)"
echo "Compiling Java source files..."

$JAVAC -d bin $(find src/main/java -name "*.java")

echo "Copying static web resources..."
mkdir -p bin/web
cp -r src/main/resources/web/* bin/web/ 2>/dev/null || true

echo "Compilation successful! Output located in ./bin"
