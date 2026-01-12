#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1
echo "   Test without topology, without delay (Using Remote IPs, 5 Nodes)"

echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/status
echo ""

echo -e "\n[STEP] Requesting CS on 1 and 2 nodes..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs
sleep ${SLEEP_TIME}
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 1 writing variable 3..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/3
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes: (Expect: 3):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 2 writing variable 4..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/4
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes: (Expect: 4):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo "Test Complete."