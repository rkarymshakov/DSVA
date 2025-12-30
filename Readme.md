# Lamport Mutual Exclusion – Distributed System Project

---

**Algorithm:** Lamport’s Mutual Exclusion  
**Problem Type:** Exclusive Access  
**Topology:** Complete Graph  
**Language:** Java  
**Communication:** RMI (Remote Method Invocation) + REST API  
**Functionality:** Shared Variable Synchronization

---

## Project Overview

This project implements **Lamport’s distributed mutual exclusion algorithm** to synchronize access to a shared variable across multiple nodes. It is designed to run in a distributed environment (multiple VMs) but can also simulate a cluster on a single machine.

### Key Features

- **Java RMI** for internal node-to-node communication (algorithm messages)
- **REST API (Javalin)** for external control and monitoring
- **Automated Bash scripts** for deployment, startup, and testing
- **Interactive CLI** for manual debugging and control
- **Failure handling**: detection of dead nodes and topology cleanup

---

## Project Structure

```text
/home/dsv/semwork/
├── code/                       # Source code repository
│   ├── src/main/java/.../      # Java source files
│   │   ├── Node.java           # RMI interface
│   │   ├── NodeImpl.java       # Core Lamport algorithm & logic
│   │   ├── APIHandler.java     # REST API controller
│   │   ├── NodeRunner.java     # Main entry point
│   │   └── NodeCLI.java        # Interactive client
│   └── pom.xml                 # Maven configuration
├── bash_variables.sh           # Environment config (IPs, ports)
├── start_nodes.sh              # Deploys and starts nodes
├── control_nodes.sh            # Automated test scenario
├── stop_system.sh              # Stops all running nodes
└── README.md
```

---

## Prerequisites

- **Java 21** (or compatible JDK)
- **Maven**
- **sshpass**
- **curl**

---

## Configuration

```bash
NUM_NODES=2
NODE_IP[1]=192.168.56.106
NODE_IP[2]=192.168.56.107
```

---

## Deployment & Running

```bash
./start_nodes.sh
```

```bash
mvn clean package
java -Djava.rmi.server.hostname=YOUR_IP -jar target/SemExample-0.9-jar-with-dependencies.jar 2010
```

---

## CLI Commands

- `connect <ip> <port>`
- `status`
- `request`
- `getvar`
- `setvar <value>`
- `kill`
- `revive`
- `detect`

---

## Logging

```text
node_<id>.log
```