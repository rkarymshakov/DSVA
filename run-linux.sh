#!/bin/bash
# Linux version of Windows run.sh - Simple & minimal

ACTION=$1
NODE=$2
PORT=$3

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/out"

case $ACTION in
    build)
        echo "Building project..."
        mkdir -p "$OUTPUT_DIR"

        # Compile ALL .java files directly in src/
        javac -d "$OUTPUT_DIR" "$SCRIPT_DIR/src"/*.java

        if [ $? -eq 0 ]; then
            echo "✓ Build successful! Classes in $OUTPUT_DIR"
        else
            echo "✗ Build failed! Check if all .java files are in src/"
            exit 1
        fi
        ;;

    server)
        if [ -z "$NODE" ] || [ -z "$PORT" ]; then
            echo "Usage: ./run-linux.sh server <nodeName> <port>"
            echo "Example: ./run-linux.sh server nodeA 2010"
            exit 1
        fi

        echo "Starting node: $NODE on port $PORT"

        # Get the VM's Host-only IP automatically
        HOST_IP=$(ip addr show enp0s8 | grep -oP 'inet \K[\d.]+' | head -1)

        if [ -z "$HOST_IP" ]; then
            echo "Warning: Could not auto-detect Host-only IP, using 127.0.0.1"
            HOST_IP="127.0.0.1"
        else
            echo "Auto-detected VM IP: $HOST_IP"
        fi

        # Run with RMI hostname set - CRITICAL FOR VMs
        java -Djava.rmi.server.hostname="$HOST_IP" -cp "$OUTPUT_DIR" NodeRunner "$NODE" "$PORT" "$HOST_IP"
        ;;

    client)
        echo "Starting interactive CLI client..."
        java -cp "$OUTPUT_DIR" NodeCLI
        ;;

    clean)
        echo "Cleaning compiled files..."
        rm -rf "$OUTPUT_DIR"
        echo "✓ Clean complete!"
        ;;

    test)
        echo "Test scenario:"
        echo "Open separate terminals:"
        echo "  1) ./run-linux.sh server nodeA 2010"
        echo "  2) ./run-linux.sh server nodeB 2011"
        echo "  3) ./run-linux.sh client"
        echo ""
        echo "In NodeCLI then:"
        echo "  connect 192.168.56.105 2010"
        ;;

    *)
        echo "Lamport Distributed System - Linux VM"
        echo ""
        echo "Usage:"
        echo "  ./run-linux.sh build                    - Compile all .java"
        echo "  ./run-linux.sh server <name> <port>     - Start node"
        echo "  ./run-linux.sh client                   - Start CLI"
        echo "  ./run-linux.sh clean                    - Delete out/"
        echo "  ./run-linux.sh test                     - Test instructions"
        ;;
esac