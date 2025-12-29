#!/bin/bash

ACTION=$1
NODE=$2
PORT=$3

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_JAR="$SCRIPT_DIR/target/SemExample-0.9-jar-with-dependencies.jar"

case $ACTION in
    build)
        echo -e "${BLUE}Building project with Maven...${NC}"
        mvn clean package

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Build successful! JAR created at $TARGET_JAR${NC}"
        else
            echo -e "${RED}Build failed! Check Maven output above${NC}"
            exit 1
        fi
        ;;

    server)
        if [ -z "$NODE" ] || [ -z "$PORT" ]; then
            echo "Usage: ./run.sh server <nodeName> <port>"
            exit 1
        fi
        if [ ! -f "$TARGET_JAR" ]; then
            echo -e "${RED}JAR not found. Run ./run.sh build first${NC}"
            exit 1
        fi
        echo -e "${BLUE}Starting node: $NODE on port $PORT${NC}"
        java -cp "$TARGET_JAR" cz.ctu.fee.dsv.semework.NodeRunner "$PORT" "$NODE"
        ;;

    client)
        if [ ! -f "$TARGET_JAR" ]; then
            echo -e "${RED}JAR not found. Run ./run.sh build first${NC}"
            exit 1
        fi
        echo -e "${BLUE}Starting interactive CLI client (NodeCLI)...${NC}"
        java -cp "$TARGET_JAR" cz.ctu.fee.dsv.semework.NodeCLI
        ;;

    clean)
        echo -e "${BLUE}Cleaning Maven target directory...${NC}"
        mvn clean
        echo -e "${GREEN}Clean complete!${NC}"
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
        echo "  ./run.sh build                    - Compile project and package with Maven"
        echo "  ./run.sh server <name> <port>     - Start one node (NodeRunner)"
        echo "  ./run.sh client                   - Start interactive CLI (NodeCLI)"
        echo "  ./run.sh clean                    - Clean Maven target directory"
        echo "  ./run.sh test                     - Show test instructions"
        ;;
esac
