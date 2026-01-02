# Lamport Mutual Exclusion – Distributed System

Implementation of **Lamport's distributed mutual exclusion algorithm** for coordinating access to shared resources across multiple nodes.

---

## Overview

- **Algorithm:** Lamport's Mutual Exclusion (1978)
- **Topology:** Complete Graph
- **Language:** Java 21
- **Communication:** Java RMI + REST API (Javalin)
- **Features:** Dynamic topology, failure detection, message delay simulation

---

## Prerequisites

- Java JDK 21+
- Maven 3.6+
- sshpass, curl, tmux (for deployment)
- Network access between VMs (RMI and REST ports)

---

## Installation

```bash
# Clone and build
git clone https://github.com/rkarymshakov/DSVA.git
cd DSVA
mvn clean package

# JAR location: target/SemExample-0.9-jar-with-dependencies.jar
```

---

## Configuration

Edit `bash_variables.sh`:

```bash
NUM_NODES=5
BASE_PORT=2010

NODE_IP[1]=192.168.56.106
NODE_IP[2]=192.168.56.107
NODE_IP[3]=192.168.56.108
NODE_IP[4]=192.168.56.109
NODE_IP[5]=192.168.56.110

SSH_USER=dsv
SSH_PASS=dsv
```

**Port Assignment:** Node N uses RMI port `2010+(N-1)*10` and REST port `3010+(N-1)*10`

---

## Deployment

### Multi-VM Deployment (Automated)

```bash
# Make scripts executable (first time only)
chmod +x *.sh

# Deploy and start all nodes
./start_nodes.sh

# Run test scenario
./control_nodes.sh
```

### Attach to Running Node

```bash
# Attach to existing node in tmux
tmux attach -t NODE_1
tmux attach -t NODE_2
# etc.
```

### Single Node (Manual)

```bash
# Start new node
java -jar semwork.jar 2010

# Or with custom hostname
java -Djava.rmi.server.hostname=<YOUR_IP> -jar semwork.jar 2010
```

### Local Testing (Multiple Nodes)

```bash
# Terminal 1
java -jar semwork.jar 2010

# Terminal 2
java -jar semwork.jar 2020
```

---

## Usage

### REST API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/join/{ip}/{port}` | POST | Join network via node |
| `/leave` | POST | Leave network |
| `/status` | GET | Get node status |
| `/enter-cs` | POST | Request critical section |
| `/leave-cs` | POST | Release critical section |
| `/var` | GET | Get shared variable |
| `/var/{value}` | POST | Set shared variable |
| `/kill` | POST | Simulate crash |
| `/revive` | POST | Revive node |
| `/delay/{ms}` | POST | Set message delay |
| `/detect` | POST | Trigger failure detection |

**Example:**
```bash
curl -X POST http://localhost:3010/join/192.168.56.107/2020
curl -X POST http://localhost:3010/enter-cs
curl -X POST http://localhost:3010/var/100
curl -X POST http://localhost:3010/leave-cs
```

### Console Commands

| Command | Description |
|---------|-------------|
| `j <ip> <port>` | Join network |
| `l` | Leave network |
| `req` | Request critical section |
| `rel` | Release critical section |
| `v <value>` | Set shared variable |
| `s` | Show status |
| `d <ms>` | Set message delay |
| `k` | Kill node |
| `rev` | Revive node |
| `det` | Detect dead nodes |
| `?` | Show help |

---

## Testing

### Automated Test

```bash
./control_nodes.sh
```

### Manual Test Example

```bash
# 1. Request CS from multiple nodes
curl -X POST http://192.168.56.106:3010/enter-cs &
curl -X POST http://192.168.56.107:3020/enter-cs &

# 2. Write shared variable (Node 1)
curl -X POST http://192.168.56.106:3010/var/999
curl -X POST http://192.168.56.106:3010/leave-cs

# 3. Verify synchronization
curl http://192.168.56.107:3020/var
curl http://192.168.56.108:3030/var
```

---

## Logging

Each node creates: `node_<NODE_ID>.log`

**Format:** `[HH:MM:SS.mmm][LC=<clock>][Node <id>] <message>`

**Example:**
```
[14:32:15.123][LC=0][Node 192168056106002010] Node created
[14:33:01.789][LC=12][Node 192168056106002010] REQUESTING CRITICAL SECTION
[14:33:02.456][LC=14][Node 192168056106002010] ENTERED CRITICAL SECTION
[14:33:03.789][LC=15][Node 192168056106002010] Wrote shared variable: 555
```

---

## Troubleshooting

**"RemoteException: Node is dead"**
```bash
curl -X POST http://<node_ip>:<api_port>/revive
```

**"Connection refused"**
```bash
# Check if node is running
ps aux | grep java
netstat -tuln | grep 2010
```

**"Illegal Access: Must be in Critical Section"**
```bash
# Enter CS before writing variable
curl -X POST http://localhost:3010/enter-cs
curl -X POST http://localhost:3010/var/100
curl -X POST http://localhost:3010/leave-cs
```

**Nodes stuck waiting for CS**
```bash
# Trigger failure detection
curl -X POST http://localhost:3010/detect
```

---

## Algorithm Details

**Lamport's Mutual Exclusion** uses logical clocks and message ordering to coordinate CS access.

**Entry Conditions:**
1. Request is at front of local queue
2. Received REPLY from all other nodes
3. Node wants to enter CS

**Message Types:** REQUEST, REPLY, RELEASE

**Properties:**
- Safety: ≤1 node in CS at any time
- Fairness: Requests granted in timestamp order
- Message Complexity: 3(N-1) per CS entry

---

## Project Structure

```
DSVA/
├── src/main/java/ctu/dsv/semwork/
│   ├── Node.java                # RMI interface
│   ├── NodeImpl.java            # Core algorithm
│   ├── NodeRunner.java          # Main entry point
│   ├── APIHandler.java          # REST API
│   ├── ConsoleHandler.java      # CLI
│   ├── Logger.java              # Logging
│   └── Request.java             # CS request
├── pom.xml
├── bash_variables.sh            # Deployment config
├── start_nodes.sh               # Deployment script
└── control_nodes.sh             # Test script
```