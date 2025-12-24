# Lamport Mutual Exclusion - Distributed System Project

## Project Overview
This project implements Lamport's distributed mutual exclusion algorithm with shared variable functionality using Java RMI.

**Current Status:** Basic RMI infrastructure (without Lamport algorithm implementation yet)

## Project Structure
```
lamport-project/
├── src/
│   ├── compute/
│   │   └── Node.java          # Remote interface for distributed nodes
│   ├── server/
│   │   ├── NodeImpl.java      # Node implementation (basic version)
│   │   └── Server.java        # Server to start and register nodes
│   └── client/
│       └── Client.java        # Interactive client for testing
└── README.md
```

## Requirements
- Java 8 or higher
- Multiple terminal windows or machines for testing

## Building the Project

### Option 1: Using javac directly
```bash
cd lamport-project/src
javac compute/*.java server/*.java client/*.java
```

### Option 2: Create a build script
Save this as `build.sh`:
```bash
#!/bin/bash
cd src
javac -d ../bin compute/*.java server/*.java client/*.java
echo "Build complete!"
```

Then run:
```bash
chmod +x build.sh
./build.sh
```

## Running the System

### Step 1: Start Multiple Nodes

Open **separate terminal windows** for each node.

**Terminal 1 - Start Node A on port 1099:**
```bash
cd lamport-project/src
java server.Server 1099 nodeA
```

**Terminal 2 - Start Node B on port 1100:**
```bash
cd lamport-project/src
java server.Server 1100 nodeB
```

**Terminal 3 - Start Node C on port 1101:**
```bash
cd lamport-project/src
java server.Server 1101 nodeC
```

Each node will display:
```
=================================================
Starting Distributed Node
=================================================
Node ID: nodeA@192.168.1.10:1099
RMI Port: 1099
=================================================
Node successfully registered in RMI registry
Node is ready and waiting for requests...
=================================================
```

### Step 2: Connect Nodes and Build Topology

Open another terminal for the **interactive client**:

```bash
cd lamport-project/src
java client.Client
```

You'll see the interactive prompt:
```
=================================================
Distributed System - Interactive Client
=================================================

Available commands:
  connect <host> <port> <n>  - Connect to a node
  addnode <host> <port> <n>  - Add a node to topology
  ...

>
```

**Now build the complete graph topology:**

```bash
# Connect to nodeA
> connect localhost 1099 nodeA
✓ Connected to node: nodeA@192.168.1.10:1099

# Add nodeB to nodeA's topology
> addnode localhost 1100 nodeB
✓ Node added to topology: nodeB@192.168.1.10:1100
  (Bidirectional connection established)

# Add nodeC to nodeA's topology
> addnode localhost 1101 nodeC
✓ Node added to topology: nodeC@192.168.1.10:1101
  (Bidirectional connection established)

# List all nodes known to nodeA
> list
Known nodes in topology:
  - nodeB@192.168.1.10:1100
  - nodeC@192.168.1.10:1101

# Now connect to nodeB and add nodeC to complete the graph
> connect localhost 1100 nodeB
✓ Connected to node: nodeB@192.168.1.10:1100

> addnode localhost 1101 nodeC
✓ Node added to topology: nodeC@192.168.1.10:1101
  (Bidirectional connection established)
```

## Testing Basic Functionality

### Test 1: Logical Clock
```bash
> connect localhost 1099 nodeA
> clock
Logical Clock: 2

> ping
✓ Ping successful

> clock
Logical Clock: 3
```

### Test 2: Shared Variable
```bash
> getvar
Shared Variable = 0

> setvar 42
✓ Shared Variable set to 42

> getvar
Shared Variable = 42
```

### Test 3: Message Delay (for testing concurrent situations)
```bash
> delay 1000
✓ Message delay set to 1000ms

> ping
✓ Ping successful
# (Notice the delay in the server logs)

> delay 0
✓ Message delay set to 0ms
```

### Test 4: Node Status
```bash
> status
Node Status:
  Node ID: nodeA@192.168.1.10:1099
  In Critical Section: false
  Queue: Queue size: 0, In CS: false
  Message Delay: 0ms
```

## What Works Now

✅ RMI infrastructure
✅ Node registration and lookup
✅ Complete graph topology building (manually via client)
✅ Logical clock incrementation
✅ Message delay simulation
✅ Basic shared variable read/write
✅ Interactive CLI for testing

## What's NOT Implemented Yet (TODO)

❌ Lamport's mutual exclusion algorithm logic
❌ Request queue management
❌ Automatic CS request/reply/release
❌ True distributed synchronization
❌ REST API
❌ Automatic topology recovery
❌ Persistent logging to file

## Next Steps

1. **Implement Lamport Algorithm:**
    - Complete the `requestCS()` logic in NodeImpl
    - Implement request queue management
    - Add reply tracking
    - Implement CS entry/exit conditions

2. **Add REST API:**
    - Embed HTTP server (e.g., using Spark Java or Spring Boot)
    - Implement endpoints: `/join`, `/leave`, `/kill`, `/enterCS`, `/leaveCS`, `/setDelay`

3. **Add File Logging:**
    - Log all operations with Lamport timestamps
    - Create separate log file per node

4. **Test Scenarios:**
    - Multiple concurrent CS requests
    - Node failure during CS
    - Message delays creating race conditions

## Typical Test Scenario

```bash
# Terminal 1: nodeA
java server.Server 1099 nodeA

# Terminal 2: nodeB  
java server.Server 1100 nodeB

# Terminal 3: nodeC
java server.Server 1101 nodeC

# Terminal 4: Client - build topology and test
java client.Client
> connect localhost 1099 nodeA
> addnode localhost 1100 nodeB
> addnode localhost 1101 nodeC
> connect localhost 1100 nodeB
> addnode localhost 1101 nodeC
> list
> status
> getvar
> setvar 100
```

## Notes

- Each node has a unique ID: `nodeName@ip:port`
- Logical clocks increment on internal events and message sends
- Logical clocks update on message receives: `max(local, received) + 1`
- Complete graph topology means every node knows every other node
- Message delays simulate slow networks for testing race conditions

## Assignment Requirements Checklist

- [x] Java + RMI
- [x] Complete graph topology
- [x] Shared variable functionality
- [x] Logical clock timestamps
- [x] Unique node identification
- [x] Message delay capability
- [x] Interactive CLI
- [ ] Lamport algorithm (TODO)
- [ ] REST API (TODO)
- [ ] File logging (TODO)
- [ ] Batch mode (TODO)

---

**Author:** Your Name  
**Course:** Distributed Systems  
**Algorithm:** Lamport's Mutual Exclusion  
**Problem Type:** Exclusive Access  
**Topology:** Complete Graph  
**Language:** Java  
**Communication:** RMI  
**Functionality:** Shared Variable