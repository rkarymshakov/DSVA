#!/bin/bash
# Build and Run Script for Lamport Distributed Mutual Exclusion Project
# Version for FLAT src/ structure + renamed NodeRunner & NodeCLI

ACTION=$1
NODE=$2
PORT=$3

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Output directory for compiled classes
OUTPUT_DIR="$SCRIPT_DIR/out"

case $ACTION in
    build)
        echo -e "${BLUE}Building project...${NC}"
        mkdir -p "$OUTPUT_DIR"

        # Compile ALL .java files directly in src/ (flat structure)
        javac -d "$OUTPUT_DIR" "$SCRIPT_DIR/src"/*.java

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Build successful! Classes in $OUTPUT_DIR${NC}"
        else
            echo -e "${RED}✗ Build failed! Check if all .java files are in src/${NC}"
            exit 1
        fi
        ;;

    server)
        if [ -z "$NODE" ] || [ -z "$PORT" ]; then
            echo "Usage: ./run.sh server <nodeName> <port>"
            echo "Example: ./run.sh server nodeA 1099"
            exit 1
        fi
        echo -e "${BLUE}Starting node: $NODE on port $PORT${NC}"

        # Run NodeRunner (main class is now NodeRunner in package server)
        java -cp "$OUTPUT_DIR" NodeRunner "$PORT" "$NODE"
        ;;

    client)
        echo -e "${BLUE}Starting interactive CLI client (NodeCLI)...${NC}"
        java -cp "$OUTPUT_DIR" NodeCLI
        ;;

    clean)
        echo -e "${BLUE}Cleaning compiled files...${NC}"
        rm -rf "$OUTPUT_DIR"
        echo -e "${GREEN}✓ Clean complete!${NC}"
        ;;

    test)
        echo -e "${BLUE}Test scenario instructions:${NC}"
        echo "Open separate terminals:"
        echo "  1) ./run.sh server nodeA 1099"
        echo "  2) ./run.sh server nodeB 1100"
        echo "  3) ./run.sh server nodeC 1101"
        echo "  4) ./run.sh client"
        echo ""
        echo "In NodeCLI then:"
        echo "  connect localhost 1099"
        echo "  addnode localhost 1100"
        echo "  addnode localhost 1101"
        echo "  list"
        ;;

    *)
        echo -e "${BLUE}Lamport Distributed Mutual Exclusion - Build & Run Script${NC}"
        echo ""
        echo "Usage:"
        echo "  ./run.sh build                    - Compile all .java from src/ to out/"
        echo "  ./run.sh server <name> <port>     - Start one node (NodeRunner)"
        echo "  ./run.sh client                   - Start interactive CLI (NodeCLI)"
        echo "  ./run.sh clean                    - Delete out/"
        echo "  ./run.sh test                     - Show test instructions"
        echo ""
        echo "Current assumptions:"
        echo "  - All .java files are directly in src/ (flat structure)"
        echo "  - Main node starter is NodeRunner"
        echo "  - CLI is NodeCLI"
        ;;
esac