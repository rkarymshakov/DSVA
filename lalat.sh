#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1
echo "=========================================="
echo "   Test: Node1 delayed, Node2 fast"
echo "=========================================="

echo "[STEP 1] Setting 2s delay on Node 1"
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/2000
sleep 1

echo -e "\n[STEP 2] Node 1 requests CS (will be delayed)"
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
sleep 0.1

echo -e "\n[STEP 3] Node 2 requests CS immediately (no delay)"
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &

echo -e "\n[STEP 4] Waiting 3s for processing..."
sleep 3

echo -e "\n[STEP 5] Check who is in CS:"
echo "Node 1:"
curl -s http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status | grep -E "(Clock|In CS)"
echo "Node 2:"
curl -s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status | grep -E "(Clock|In CS)"

echo -e "\n[CLEANUP] Reset delay"
curl -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0

echo -e "\n=========================================="
echo "EXPECTED: Node 2 enters CS first"
echo "Node 1 timestamp > Node 2 timestamp"
echo "=========================================="