#!/bin/bash
source bash_variables.sh

SLEEP_TIME=2
echo "   TEST SCENARIO (Using Remote IPs, 5 Nodes)"

echo -e "\n[STEP] Status Check:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status
echo ""
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status
echo ""
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/status
echo ""

echo -e "\n[STEP] Requesting CS on all nodes..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Node 1 writing variable 33..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/33
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs

sleep ${SLEEP_TIME}

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
echo "Node 4: "
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/var
echo ""
echo "Node 5: "
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo -e "\n[STEP] Node 2 writing variable 44..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/44
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs

sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes:"
echo "Node 5: "
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo "Test Complete."