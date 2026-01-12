#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1
echo "   Test Race Condition: Delayed Node 1 (Low TS) vs Fast Node 2 (High TS)"

echo -e "\n[STEP] Forcing clocks: Node 1 -> 40, Node 2 -> 80..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/clock/40
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/clock/80

echo -e "\n[STEP] Setting 3s delay on Node 1..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/3000

echo -e "\n[STEP] Node 1 requests CS (Delayed, Low TS)..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
sleep 0.5

echo -e "\n[STEP] Node 2 requests CS (Instant, High TS)..."
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &

echo -e "\n[STEP] Waiting 5s for negotiation..."
sleep 5

echo -e "\n[STEP] Checking Node 1 is in CS (Expect: true):"
curl -s http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status | grep "inCriticalSection"

echo -e "\n[STEP] Node 1 writing variable 33..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/33
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes (Expect: 33):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo -e "\n[STEP] Node 1 leaves CS..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs
sleep 4

echo -e "\n[STEP] Checking Node 2 is in CS (Expect: true):"
curl -s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status | grep "inCriticalSection"

echo -e "\n[STEP] Node 2 writing variable 44..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/44
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Reading shared variable from 5. nodes (Expect: 44):"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""

echo -e "\n[STEP] Node 2 leaves CS..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP] Resetting Node 1 delay to 0..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0
echo ""

echo "Test Complete."