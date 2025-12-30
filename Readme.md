```markdown
# Lamport Mutual Exclusion - Distributed System Project

---

**Algorithm:** Lamport's Mutual Exclusion  
**Problem Type:** Exclusive Access  
**Topology:** Complete Graph  
**Language:** Java  
**Communication:** RMI (Remote Method Invocation) + REST API  
**Functionality:** Shared Variable Synchronization  

---

## Project Overview

This project implements **Lamport's distributed mutual exclusion algorithm** to synchronize access to a shared variable across multiple nodes. It is designed to run in a distributed environment (multiple VMs) but can also simulate a cluster on a single machine.

Key features include:
* **Java RMI** for internal node-to-node communication (algorithm messages).
* **REST API** (Javalin) for external control and monitoring.
* **Automated Bash Scripts** for deployment, startup, and testing.
* **Interactive CLI** for manual debugging and control.
* **Fault Tolerance:** Detection of dead nodes and topology recovery.

## Project Structure

```text
/home/dsv/semwork/
├── code/                       # Source code repository
│   ├── src/main/java/.../      # Java Source Files
│   │   ├── Node.java           # RMI Interface
│   │   ├── NodeImpl.java       # Core Algorithm & Logic
│   │   ├── APIHandler.java     # REST API Controller
│   │   ├── NodeRunner.java     # Main Entry Point
│   │   └── NodeCLI.java        # Interactive Client
│   └── pom.xml                 # Maven Configuration
├── bash_variables.sh           # Configuration (IPs, Ports)
├── start_nodes.sh              # Deploys and starts nodes
├── control_nodes.sh            # Runs the automated test scenario
├── stop_system.sh              # Kills all running nodes
└── README.md

```

## Prerequisites

* **Java 21** (or compatible JDK)
* **Maven** (for building the project)
* **sshpass** (for automated deployment scripts)
* **curl** (for controlling nodes via REST API)

## Configuration

Edit `bash_variables.sh` to configure your environment:

```bash
# bash_variables.sh
NUM_NODES=2                     # Number of nodes to deploy (Min: 5 for final presentation)
NODE_IP[1]=192.168.56.106       # IP of Node 1 (Master)
NODE_IP[2]=192.168.56.107       # IP of Node 2 (Worker)
...

```

*Note: Ports are auto-calculated. RMI starts at 2010, REST API starts at 3010.*

## Deployment & Running

### 1. automated Deployment (Recommended)

This script will compile the code, create the JAR (with dependencies), copy it to all defined remote VMs, and start the nodes in background `tmux` sessions.

```bash
./start_nodes.sh

```

### 2. Manual Running (Local/Dev)

If you want to run a node manually without the scripts:

```bash
# Build the project
mvn clean package

# Run Node 1 (RMI Port 2010)
java -Djava.rmi.server.hostname=YOUR_IP -jar target/SemExample-0.9-jar-with-dependencies.jar 2010

```

## controlling the System

### Option A: Automated Test Scenario

This script executes a sequence of operations: Joining, Status Check, Mutual Exclusion Request, and Variable Write.

```bash
./control_nodes.sh

```

### Option B: Interactive CLI

You can connect to any running node using the provided CLI tool.

```bash
# Run the CLI
java -cp target/SemExample-0.9-jar-with-dependencies.jar cz.ctu.fee.dsv.semework.NodeCLI

```

**Common CLI Commands:**

* `connect <ip> <port>`: Connect to a running node (e.g., `connect 192.168.56.106 2010`).
* `status`: View logical clock, queue status, and known neighbors.
* `request`: Request entry to the Critical Section.
* `getvar` / `setvar <val>`: Read or write the shared variable.
* `kill` / `revive`: Simulate a node crash/recovery.
* `detect`: Run failure detection to clean up dead nodes.

### Option C: REST API (curl)

You can control nodes directly via HTTP requests.

* **Status:** `curl http://IP:3010/status`
* **Join:** `curl -X POST http://IP:3020/join/MASTER_IP/MASTER_PORT`
* **Enter CS:** `curl -X POST http://IP:3010/enter-cs`
* **Write Variable:** `curl -X POST http://IP:3010/var/100`

## Algorithm Implementation Details

### Lamport's Mutual Exclusion

1. **Requesting:** When a node wants to enter the Critical Section (CS), it increments its clock, adds a request to its local queue, and broadcasts a `REQUEST` message to all neighbors.
2. **Processing:** Upon receiving a request, neighbors update their clocks and add the request to their queues. They send a `REPLY` back.
3. **Entering:** A node enters the CS when:
* Its own request is at the head of the priority queue.
* It has received a message (REQUEST, REPLY, or RELEASE) from every other node with a timestamp larger than its request.


4. **Releasing:** Upon exiting, the node removes its request and broadcasts `RELEASE`.

### Failure Handling

* **Kill:** Simulates a crash by setting an internal flag (`isDead`). The node stops responding to RMI calls.
* **Detection:** The `detectDeadNodes()` method pings all known neighbors. If a neighbor times out, it is removed from the local topology and a `notifyNodeDead` message is broadcast to others.

## Logging

Logs are written to both the console (standard output) and file:

* `node_<id>.log`

Example Log Output:

```text
[HH:mm:ss.SSS][LC=5][Node 1706894745] === REQUESTING CRITICAL SECTION (My Timestamp: 5) ===
[HH:mm:ss.SSS][LC=6][Node 1706894745] Received REPLY from 1706894776 (ts=6)
[HH:mm:ss.SSS][LC=6][Node 1706894745] >>> ENTERED CRITICAL SECTION <<<
[HH:mm:ss.SSS][LC=7][Node 1706894745] Writing Shared Variable: 555
[HH:mm:ss.SSS][LC=8][Node 1706894745] >>> LEFT CRITICAL SECTION <<<

```

```

```