#!/bin/bash
# Automated script to start 3 nodes and connect them
# Usage: ./start-system.sh

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/out"

# Node configuration
NODE1_PORT=2010
NODE2_PORT=2011
NODE3_PORT=2012
HOST="localhost"

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Lamport Distributed System - Auto Startup${NC}"
echo -e "${BLUE}=================================================${NC}"

# Check if compiled
if [ ! -d "$OUTPUT_DIR" ]; then
    echo -e "${RED}✗ Project not compiled! Run ./run.sh build first${NC}"
    exit 1
fi

# Kill any existing Java processes on these ports (cleanup)
echo -e "${YELLOW}Cleaning up old processes...${NC}"
for PORT in $NODE1_PORT $NODE2_PORT $NODE3_PORT; do
    PID=$(lsof -ti:$PORT 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "  Killed process on port $PORT"
    fi
done
sleep 1

# Start Node 1
echo -e "${GREEN}Starting Node 1 on port $NODE1_PORT...${NC}"
java -cp "$OUTPUT_DIR" cz.ctu.fee.dsv.semework.NodeRunner $NODE1_PORT > node1_console.log 2>&1 &
NODE1_PID=$!
sleep 2

# Start Node 2
echo -e "${GREEN}Starting Node 2 on port $NODE2_PORT...${NC}"
java -cp "$OUTPUT_DIR" cz.ctu.fee.dsv.semework.NodeRunner $NODE2_PORT > node2_console.log 2>&1 &
NODE2_PID=$!
sleep 2

# Start Node 3
echo -e "${GREEN}Starting Node 3 on port $NODE3_PORT...${NC}"
java -cp "$OUTPUT_DIR" cz.ctu.fee.dsv.semework.NodeRunner $NODE3_PORT > node3_console.log 2>&1 &
NODE3_PID=$!
sleep 2

echo -e "${BLUE}=================================================${NC}"
echo -e "${GREEN}✓ All nodes started!${NC}"
echo -e "${BLUE}=================================================${NC}"
echo "Node 1: PID=$NODE1_PID, Port=$NODE1_PORT"
echo "Node 2: PID=$NODE2_PID, Port=$NODE2_PORT"
echo "Node 3: PID=$NODE3_PID, Port=$NODE3_PORT"
echo -e "${BLUE}=================================================${NC}"

# Now connect them using NodeCLI commands
echo -e "${YELLOW}Connecting nodes together...${NC}"
sleep 1

# Create a temporary script for NodeCLI to execute
cat > /tmp/cli_commands.txt << EOF
connect $HOST $NODE1_PORT
addnode $HOST $NODE2_PORT
addnode $HOST $NODE3_PORT
list
status
exit
EOF

# Run CLI with the commands
java -cp "$OUTPUT_DIR" cz.ctu.fee.dsv.semework.NodeCLI < /tmp/cli_commands.txt

# Clean up temp file
rm /tmp/cli_commands.txt

echo -e "${BLUE}=================================================${NC}"
echo -e "${GREEN}✓ System is ready!${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""
echo "Nodes are running in background:"
echo "  - Node 1: $HOST:$NODE1_PORT (PID: $NODE1_PID)"
echo "  - Node 2: $HOST:$NODE2_PORT (PID: $NODE2_PID)"
echo "  - Node 3: $HOST:$NODE3_PORT (PID: $NODE3_PID)"
echo ""
echo "Log files:"
echo "  - node_<nodeID>.log (NodeImpl logs)"
echo "  - node<N>_console.log (console output for debugging)"
echo ""
echo "To interact with the system:"
echo "  ./run.sh client"
echo ""
echo "To stop all nodes:"
echo "  ./stop-system.sh"
echo ""
echo -e "${BLUE}=================================================${NC}"

# Save PIDs to file for cleanup script
echo "$NODE1_PID" > /tmp/lamport_nodes.pid
echo "$NODE2_PID" >> /tmp/lamport_nodes.pid
echo "$NODE3_PID" >> /tmp/lamport_nodes.pid