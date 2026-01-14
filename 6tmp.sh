#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1
echo "   Test without topology, with delay (Using Remote IPs, 5 Nodes)"

echo "[STEP] Setting 2s message delay on Node 2..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/delay/1000
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status
echo ""

echo -e "\n[STEP] Requesting CS on 1, 2 nodes..."
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs
sleep ${SLEEP_TIME}
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 2 writing variable 100..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/100
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes: (Expect: 100):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 1 writing variable 90..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/90
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes: (Expect: 90):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Resetting Node 1 delay to 0..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0
echo ""

echo "Test Complete."