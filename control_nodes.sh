#!/bin/bash
source bash_variables.sh

SLEEP_TIME=2
echo "   TEST SCENARIO (Using Remote IPs)"

# 1. Topology
echo "[STEP] Node 2 (${NODE_IP[2]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

# 2. Status
echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status
echo ""
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status
echo ""

# 3. Critical Section
echo -e "\n[STEP] Requesting CS on both nodes..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &

sleep ${SLEEP_TIME}

# 4. Write Variable
echo -e "\n[STEP] Writing Variable..."
# Assuming Node 1 is inside
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/555
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs

sleep ${SLEEP_TIME}
echo -e "\n[STEP] Verifying sync on Node 2:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var
echo ""

echo "Test Complete."