#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1
echo "================================================"
echo "   Test WITH Message Delay (Node 1 delayed)"
echo "================================================"

echo -e "\n[STEP 1] Setting message delay on Node 1 to 2000ms..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/2000
sleep 1

echo -e "\n[STEP 3] Initial status check (Node 1):"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status
echo ""

echo -e "\n[STEP 4] Node 1 (WITH DELAY) requests CS"
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
NODE1_PID=$!
echo "Node 1 request initiated (background process: $NODE1_PID)"

# Small delay to ensure Node 1's request is sent first
sleep 0.5

echo -e "\n[STEP 5] Node 2 (NO DELAY) requests CS"
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &
NODE2_PID=$!
echo "Node 2 request initiated (background process: $NODE2_PID)"

echo -e "\n[STEP 6] Waiting for both nodes to process CS requests..."
echo "Note: Node 1 has 2000ms delay per message, Node 2 has no delay"
echo "Expected behavior: Node 1 should enter CS first (lower timestamp)"
sleep 5

echo -e "\n[STEP 7] Checking which node is in CS..."
echo "Node 1 status:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status | grep -E "(inCriticalSection|logicalClock)"
echo ""
echo "Node 2 status:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status | grep -E "(inCriticalSection|logicalClock)"
echo ""

echo -e "\n[STEP 8] Node 1 (should be in CS) writes variable to 111..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/var/111
sleep ${SLEEP_TIME}

echo -e "\n[STEP 9] Verifying shared variable from Node 5:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""
sleep ${SLEEP_TIME}

echo -e "\n[STEP 10] Node 1 leaves CS..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs
sleep 2

echo -e "\n[STEP 11] Now Node 2 should enter CS..."
sleep 3

echo -e "\n[STEP 12] Node 2 (should be in CS) writes variable to 222..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/var/222
sleep ${SLEEP_TIME}

echo -e "\n[STEP 13] Verifying shared variable from Node 5:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/var
echo ""
sleep ${SLEEP_TIME}

echo -e "\n[STEP 14] Node 2 leaves CS..."
curl -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs
sleep ${SLEEP_TIME}

echo -e "\n[STEP 15] Final status check:"
echo "Node 1:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status
echo ""
echo "Node 2:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status
echo ""

echo -e "\n[STEP 16] Resetting Node 1 delay to 0..."
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0
echo ""

echo -e "\n================================================"
echo "   Test Complete"
echo "================================================"
echo ""
echo "EXPECTED RESULTS:"
echo "1. Node 1 should enter CS FIRST despite 2000ms delay"
echo "2. Shared variable should be 111, then 222"
echo "================================================"