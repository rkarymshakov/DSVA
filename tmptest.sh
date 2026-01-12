#!/bin/bash
source bash_variables.sh

# Настройки
DELAY_MS=2000
SLEEP_TIME=1

echo "================================================"
echo "   RACE SIMULATION: Delayed Priority vs Fast Low-Priority"
echo "================================================"

# 1. Сброс задержек в 0 для чистоты
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0 > /dev/null

# 2. МАНИПУЛЯЦИЯ ВРЕМЕНЕМ (Ключевой момент!)
# Ставим Node 1 в начало времени (чтобы он выиграл по логике)
# Ставим Node 2 в будущее (чтобы он проиграл по логике, хотя он быстрый)
echo "[SETUP] Forcing clocks: Node 1 -> 1, Node 2 -> 10"
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/clock/1
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/clock/10

# 3. Включаем тормоза для Node 1
echo "[SETUP] Setting delay on Node 1 to ${DELAY_MS}ms..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/${DELAY_MS}

echo -e "\n[ACTION] Starting Race..."

# 4. Запускаем Node 1 (TS будет 2). Он "повиснет" на 2 секунды перед отправкой.
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/enter-cs &
PID1=$!
echo " -> Node 1 requested (TS=2, Delayed send)"

sleep 0.5

# 5. Запускаем Node 2 (TS будет 11). Он отправит запрос МГНОВЕННО.
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/enter-cs &
PID2=$!
echo " -> Node 2 requested (TS=11, Instant send)"

echo -e "\n[WAITING] Simulation in progress..."
sleep 5

echo -e "\n[CHECK] Verifying who entered CS (Expectation: Node 1)"
echo "------------------------------------------------"
echo "Node 1 Status (Should be TRUE or just finished):"
curl -s http://${NODE_IP[1]}:${NODE_API_PORT[1]}/status | grep -E "(inCriticalSection|logicalClock|Queue)"
echo "------------------------------------------------"
echo "Node 2 Status (Should be FALSE, waiting for Node 1):"
curl -s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status | grep -E "(inCriticalSection|logicalClock|Queue)"

# 6. Завершаем CS для Node 1, чтобы пустить Node 2
echo -e "\n[ACTION] Node 1 leaves CS..."
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave-cs
sleep 3

echo -e "\n[CHECK] Node 2 should be in CS now:"
curl -s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/status | grep "inCriticalSection"

# Финальная чистка
curl -s -X POST http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave-cs
curl -s -X POST http://${NODE_IP[1]}:${NODE_API_PORT[1]}/delay/0