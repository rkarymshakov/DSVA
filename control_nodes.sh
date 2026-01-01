#!/bin/bash
source bash_variables.sh

SLEEP_TIME=2
echo "   TEST SCENARIO (Using Remote IPs, 3 Nodes)"

# 1. Topology
echo "[STEP] Node 2 (${NODE_IP[2]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo "[STEP] Node 3 (${NODE_IP[3]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

# 2. Status
echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status
echo ""
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status
echo ""
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/status
echo ""

# 3. Critical Section
echo -e "\n[STEP] Requesting CS on all nodes..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &
curl -s -X POST http://${NODE_IP[3]}:${NODE_API_PORT[3]}/enter-cs &

sleep ${SLEEP_TIME}

# 4. Write Variable (Node 1 writes)
echo -e "\n[STEP] Node 1 writing variable 555..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/555
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs

sleep ${SLEEP_TIME}

# 5. Verify shared variable on all nodes
echo -e "\n[STEP] Reading shared variable from all nodes:"
echo "Node 1: "
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var
echo ""
echo "Node 2: "
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var
echo ""
echo "Node 3: "
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/var
echo ""

echo "Test Complete."
