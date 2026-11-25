#!/usr/bin/env bash
# ==============================================================================
# Thapar University Dispensary Management System - Reset Simulation Data
# Resets all JSON data stores to the initial starting demonstration state.
# ==============================================================================

set -e

PORT="${1:-8080}"
echo "======================================================================="
echo "  Thapar University Dispensary Management System"
echo "  Resetting Simulation Data to Fresh Initial State..."
echo "======================================================================="

# If the server is currently running on the port, trigger the clean reset API
if curl -s -X POST "http://localhost:${PORT}/api/admin/reset-data" > /dev/null 2>&1; then
    echo "✅ Successfully called server reset API on port ${PORT}."
    echo "✅ Initial queue (#101 - #112), student directories, schedules, and stock restored!"
else
    echo "ℹ️ Server is not running on port ${PORT}. Clearing data directory directly..."
    rm -f data/*.json
    echo "✅ Removed data/*.json. Files will be freshly re-seeded on next server start."
fi

echo "======================================================================="
echo "  Simulation is ready to run from the very beginning!"
echo "======================================================================="
