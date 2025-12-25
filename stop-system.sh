#!/bin/bash
# Stop all running nodes
# Usage: ./stop-system.sh

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping all Lamport nodes...${NC}"

# Read PIDs from file
if [ -f /tmp/lamport_nodes.pid ]; then
    while read PID; do
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID 2>/dev/null
            echo "  Stopped process $PID"
        fi
    done < /tmp/lamport_nodes.pid
    rm /tmp/lamport_nodes.pid
fi

# Also kill any Java processes on our ports (backup method)
for PORT in 2010 2011 2012; do
    PID=$(lsof -ti:$PORT 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "  Killed process on port $PORT"
    fi
done

echo -e "${GREEN}✓ All nodes stopped${NC}"