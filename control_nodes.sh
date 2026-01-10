#!/bin/bash
source bash_variables.sh

SLEEP_TIME=2
echo "   TEST SCENARIO (Using Remote IPs, 5 Nodes)"

echo "[STEP] Node 2 (${NODE_IP[2]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo "[STEP] Node 3 (${NODE_IP[3]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo "[STEP] Node 4 (${NODE_IP[4]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo "[STEP] Node 5 (${NODE_IP[5]}) joining Node 1 (${NODE_IP[1]})..."
curl -X POST http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/status
echo ""

echo -e "\n[STEP] Requesting CS on all nodes..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 1 writing variable 11..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/11
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes:"
echo "Node 5: "
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo -e "\n[STEP] Node 2 writing variable 22..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/22
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes:"
echo "Node 5: "
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo "Test Complete."