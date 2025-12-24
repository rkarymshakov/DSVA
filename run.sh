#!/bin/bash

# Build and Run Script for Lamport Distributed System

ACTION=$1
NODE=$2
PORT=$3

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

case $ACTION in
    build)
        echo -e "${BLUE}Building project...${NC}"

        # Create out directory if it doesn't exist
        mkdir -p "$SCRIPT_DIR/out"

        # Compile from src to out
        cd "$SCRIPT_DIR/src"
        javac -d "$SCRIPT_DIR/out" compute/*.java server/*.java client/*.java

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Build successful! Classes in $SCRIPT_DIR/out${NC}"
        else
            echo -e "${RED}✗ Build failed!${NC}"
            exit 1
        fi
        ;;

    server)
        if [ -z "$NODE" ] || [ -z "$PORT" ]; then
            echo "Usage: ./run.sh server <nodeName> <port>"
            echo "Example: ./run.sh server nodeA 1099"
            exit 1
        fi
        echo -e "${BLUE}Starting server: $NODE on port $PORT${NC}"

        # Run from out directory where classes are
        cd "$SCRIPT_DIR/out"
        java server.Server $PORT $NODE
        ;;

    client)
        echo -e "${BLUE}Starting interactive client...${NC}"

        # Run from out directory where classes are
        cd "$SCRIPT_DIR/out"
        java client.Client
        ;;

    clean)
        echo -e "${BLUE}Cleaning compiled files...${NC}"
        rm -rf "$SCRIPT_DIR/out"
        echo -e "${GREEN}✓ Clean complete!${NC}"
        ;;

    test)
        echo -e "${BLUE}Running test scenario...${NC}"
        echo "This will start 3 nodes. Open separate terminals and run:"
        echo "  Terminal 1: ./run.sh server nodeA 1099"
        echo "  Terminal 2: ./run.sh server nodeB 1100"
        echo "  Terminal 3: ./run.sh server nodeC 1101"
        echo "  Terminal 4: ./run.sh client"
        ;;

    *)
        echo "Lamport Distributed System - Build and Run Script"
        echo ""
        echo "Usage:"
        echo "  ./run.sh build                    - Compile the project"
        echo "  ./run.sh server <name> <port>     - Start a server node"
        echo "  ./run.sh client                   - Start interactive client"
        echo "  ./run.sh clean                    - Remove compiled files"
        echo "  ./run.sh test                     - Show test instructions"
        echo ""
        echo "Examples:"
        echo "  ./run.sh build"
        echo "  ./run.sh server nodeA 1099"
        echo "  ./run.sh server nodeB 1100"
        echo "  ./run.sh client"
        echo ""
        echo "Project structure:"
        echo "  ./src/          - Source files"
        echo "  ./out/          - Compiled classes (created by build)"
        echo "  ./run.sh        - This script"
        ;;
esac