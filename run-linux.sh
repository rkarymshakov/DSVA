#!/bin/bash
# ==============================================================
# Linux VM Run Script for Lamport Distributed Mutual Exclusion
# Optimized for Ubuntu VMs with Host-only networking
# ==============================================================

ACTION=$1
NODE_NAME=$2
PORT=$3
HOST_IP=$4

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/out"
SRC_DIR="$SCRIPT_DIR/src"
JAR_NAME="lamport-system.jar"

# Helper function to get VM IP automatically
get_vm_ip() {
    # Try to get Host-only adapter IP (enp0s8 or eth1)
    IP=$(ip addr show 2>/dev/null | grep -oP 'inet \K192\.168\.56\.\d+' | head -1)

    if [ -z "$IP" ]; then
        # Fallback: try other common host-only ranges
        IP=$(ip addr show 2>/dev/null | grep -oP 'inet \K(192\.168\.\d+\.\d+|10\.0\.\d+\.\d+)' | head -1)
    fi

    if [ -z "$IP" ]; then
        echo "127.0.0.1"
    else
        echo "$IP"
    fi
}

# Display current VM info
show_vm_info() {
    echo -e "${YELLOW}=== VM Information ==="
    echo "Hostname: $(hostname)"
    echo "IP Addresses:"
    ip addr show | grep "inet " | grep -v 127.0.0.1
    echo -e "=====================${NC}"
}

case $ACTION in
    build)
        echo -e "${BLUE}Building project for Linux VM...${NC}"
        show_vm_info

        mkdir -p "$OUTPUT_DIR"

        # Find all Java files and compile
        echo "Compiling Java files from $SRC_DIR..."
        find "$SRC_DIR" -name "*.java" > sources.txt
        javac -d "$OUTPUT_DIR" @sources.txt

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Build successful! Classes in $OUTPUT_DIR${NC}"
            # Create JAR for easy transfer between VMs
            echo "Creating JAR file..."
            jar cfe "$JAR_NAME" NodeRunner -C "$OUTPUT_DIR" .
            echo -e "${GREEN}✓ JAR created: $JAR_NAME${NC}"
        else
            echo -e "${RED}✗ Build failed! Check Java files in $SRC_DIR${NC}"
            exit 1
        fi
        rm -f sources.txt
        ;;

    server)
        if [ -z "$NODE_NAME" ] || [ -z "$PORT" ]; then
            echo -e "${RED}Usage: $0 server <nodeName> <port> [hostIP]${NC}"
            echo "Example: $0 server NodeA 2010"
            echo "Example: $0 server NodeA 2010 192.168.56.105"
            echo ""
            echo "If hostIP is not provided, script will try to detect automatically."
            exit 1
        fi

        # Auto-detect IP if not provided
        if [ -z "$HOST_IP" ]; then
            HOST_IP=$(get_vm_ip)
            echo -e "${YELLOW}Auto-detected VM IP: $HOST_IP${NC}"
        fi

        show_vm_info
        echo -e "${BLUE}Starting node: $NODE_NAME on port $PORT (REST: $((PORT + 5000)))${NC}"
        echo -e "${YELLOW}RMI Hostname set to: $HOST_IP${NC}"

        # Kill any existing process on this port
        echo "Checking for existing processes on port $PORT..."
        PID=$(lsof -ti:$PORT 2>/dev/null)
        if [ ! -z "$PID" ]; then
            echo "Killing existing process (PID: $PID) on port $PORT..."
            kill -9 $PID 2>/dev/null
            sleep 1
        fi

        # Start the node with RMI configuration
        echo "Starting NodeRunner..."
        java -Djava.rmi.server.hostname="$HOST_IP" \
             -cp "$OUTPUT_DIR" NodeRunner "$NODE_NAME" "$PORT" "$HOST_IP" &

        NODE_PID=$!
        echo -e "${GREEN}✓ Node started with PID: $NODE_PID${NC}"

        # Wait a moment and check if it's running
        sleep 2
        if ps -p $NODE_PID > /dev/null; then
            echo -e "${GREEN}✓ Node is running${NC}"
            echo "RMI Port: $PORT"
            echo "REST API: http://$HOST_IP:$((PORT + 5000))"
            echo "Logs are being written to console"
        else
            echo -e "${RED}✗ Node failed to start! Check console output.${NC}"
        fi
        ;;

    jar-server)
        if [ -z "$NODE_NAME" ] || [ -z "$PORT" ]; then
            echo "Usage: $0 jar-server <nodeName> <port> [hostIP]"
            echo "Starts from JAR file (after running 'build')"
            exit 1
        fi

        if [ ! -f "$JAR_NAME" ]; then
            echo -e "${RED}JAR file not found. Run './run-linux.sh build' first.${NC}"
            exit 1
        fi

        if [ -z "$HOST_IP" ]; then
            HOST_IP=$(get_vm_ip)
        fi

        echo -e "${BLUE}Starting from JAR: $NODE_NAME on port $PORT${NC}"
        java -Djava.rmi.server.hostname="$HOST_IP" \
             -jar "$JAR_NAME" "$NODE_NAME" "$PORT" "$HOST_IP" &
        ;;

    client)
        echo -e "${BLUE}Starting interactive CLI client...${NC}"
        show_vm_info
        java -cp "$OUTPUT_DIR" NodeCLI
        ;;

    status)
        echo -e "${YELLOW}=== System Status ==="
        echo "Java processes:"
        ps aux | grep java | grep -v grep

        echo -e "\nNetwork ports in use:"
        for port in 2010 2011 2012 2013 2014 1099; do
            if sudo lsof -i :$port > /dev/null 2>&1; then
                echo -e "  Port $port: ${GREEN}LISTENING${NC}"
            else
                echo -e "  Port $port: ${RED}CLOSED${NC}"
            fi
        done

        echo -e "\nVM Network:"
        ip addr show | grep "inet " | grep -v 127.0.0.1
        echo -e "==================${NC}"
        ;;

    connect)
        # Connect to another node (useful for testing)
        if [ -z "$2" ] || [ -z "$3" ]; then
            echo "Usage: $0 connect <targetIP> <targetPort>"
            echo "Example: $0 connect 192.168.56.106 2011"
            exit 1
        fi

        TARGET_IP=$2
        TARGET_PORT=$3

        echo -e "${BLUE}Testing connection to $TARGET_IP:$TARGET_PORT...${NC}"

        # Test network connectivity
        if ping -c 1 -W 1 "$TARGET_IP" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Network reachable${NC}"
        else
            echo -e "${RED}✗ Network unreachable${NC}"
        fi

        # Test port connectivity
        if nc -zv "$TARGET_IP" "$TARGET_PORT" 2>&1 | grep -q "succeeded"; then
            echo -e "${GREEN}✓ Port $TARGET_PORT is open${NC}"
        else
            echo -e "${RED}✗ Port $TARGET_PORT is closed${NC}"
        fi
        ;;

    firewall)
        echo -e "${YELLOW}=== Firewall Configuration ==="
        echo "Current status:"
        sudo ufw status

        echo -e "\nTo disable firewall (for testing):"
        echo "  sudo ufw disable"
        echo ""
        echo "To allow RMI ports:"
        echo "  sudo ufw allow 2010:2015/tcp"
        echo "  sudo ufw allow 7010:7015/tcp"
        echo -e "================================${NC}"
        ;;

    clean)
        echo -e "${BLUE}Cleaning project...${NC}"
        rm -rf "$OUTPUT_DIR"
        rm -f "$JAR_NAME"
        echo -e "${GREEN}✓ Clean complete!${NC}"
        ;;

    deploy)
        # Copy JAR to another VM (requires SSH setup)
        if [ -z "$2" ] || [ -z "$3" ]; then
            echo "Usage: $0 deploy <targetVM_IP> <targetVM_User>"
            echo "Example: $0 deploy 192.168.56.106 dsv"
            exit 1
        fi

        TARGET_IP=$2
        TARGET_USER=$3

        if [ ! -f "$JAR_NAME" ]; then
            echo "Building JAR first..."
            $0 build
        fi

        echo "Deploying to $TARGET_USER@$TARGET_IP..."
        scp "$JAR_NAME" "$TARGET_USER@$TARGET_IP:~/"
        scp "$0" "$TARGET_USER@$TARGET_IP:~/run-linux.sh"
        ssh "$TARGET_USER@$TARGET_IP" "chmod +x ~/run-linux.sh"

        echo -e "${GREEN}✓ Deployment complete!${NC}"
        echo "On target VM, run: ./run-linux.sh jar-server NodeB 2011"
        ;;

    test-vm)
        echo -e "${YELLOW}=== VM-to-VM Test Scenario ==="
        show_vm_info

        echo -e "\n${BLUE}Instructions for 2-VM test:${NC}"
        echo ""
        echo "${GREEN}On VM1 (first terminal):${NC}"
        echo "  1. Build:        ./run-linux.sh build"
        echo "  2. Start NodeA:  ./run-linux.sh server NodeA 2010"
        echo ""
        echo "${GREEN}On VM2 (second terminal):${NC}"
        echo "  1. Build:        ./run-linux.sh build"
        echo "  2. Start NodeB:  ./run-linux.sh server NodeB 2011"
        echo ""
        echo "${GREEN}On VM2 (third terminal):${NC}"
        echo "  1. Connect CLI:  ./run-linux.sh client"
        echo "  2. In CLI:       connect <VM1_IP> 2010"
        echo "  3. In CLI:       addnode <VM1_IP> 2010"
        echo ""
        echo "${GREEN}Test REST API (from any VM):${NC}"
        echo "  curl http://<VM1_IP>:7010/status"
        echo "  curl http://<VM2_IP>:7011/status"
        echo -e "==========================================${NC}"
        ;;

    *)
        echo -e "${BLUE}Lamport Distributed System - Linux VM Manager${NC}"
        echo ""
        echo -e "${YELLOW}VM Network Info:${NC}"
        show_vm_info
        echo ""
        echo -e "${GREEN}Available commands:${NC}"
        echo "  build                     - Compile project and create JAR"
        echo "  server <name> <port> [ip] - Start a node (auto-detects IP)"
        echo "  jar-server <name> <port>  - Start from existing JAR"
        echo "  client                    - Start interactive CLI"
        echo "  status                    - Show system status"
        echo "  connect <ip> <port>       - Test connection to another node"
        echo "  firewall                  - Show firewall status and commands"
        echo "  deploy <ip> <user>        - Deploy to another VM (SSH)"
        echo "  test-vm                   - Show 2-VM test instructions"
        echo "  clean                     - Clean build outputs"
        echo ""
        echo -e "${YELLOW}Examples:${NC}"
        echo "  ./run-linux.sh server NodeA 2010"
        echo "  ./run-linux.sh server NodeB 2011 192.168.56.106"
        echo "  ./run-linux.sh connect 192.168.56.106 2011"
        echo "  ./run-linux.sh status"
        echo ""
        echo -e "${YELLOW}Note:${NC} Make script executable with: chmod +x run-linux.sh"
        ;;
esac