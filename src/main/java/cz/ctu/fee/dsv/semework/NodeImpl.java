package cz.ctu.fee.dsv.semework;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class NodeImpl extends UnicastRemoteObject implements Node {

    private final long nodeId;
    private int logicalClock;

    private final Map<Long, Node> knownNodes;

    // Lamport specific: Track the latest timestamp received from each neighbor
    private final Map<Long, Integer> latestKnownTimestamps;

    private final Set<Long> repliesReceivedForMyRequest = new HashSet<>();

    private int sharedVariable;
    private boolean inCriticalSection;
    private int messageDelayMs;

    // Queue sorted by timestamp, then by nodeId
    private final PriorityQueue<Request> requestQueue;

    private FileWriter logWriter;
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private boolean wantCS = false;
    private int myRequestTimestamp = -1;

    private boolean isDead = false; // If true, node simulates a crash

    private static final int PING_TIMEOUT_MS = 3000;
    // Executor for handling async pings (avoids freezing the main thread)
    private final ExecutorService failureDetectionExecutor = Executors.newCachedThreadPool();

    public NodeImpl(long nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.logicalClock = 0;
        this.knownNodes = new ConcurrentHashMap<>();
        this.latestKnownTimestamps = new ConcurrentHashMap<>();
        this.sharedVariable = 0;
        this.inCriticalSection = false;
        this.messageDelayMs = 0;
        this.requestQueue = new PriorityQueue<>();

        try {
            this.logWriter = new FileWriter("node_" + nodeId + ".log", true);
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
            this.logWriter = null;
        }

        log("Node created with ID: " + nodeId);
    }

    public static long generateId(String ip, int port) {
        String idStr = ip + ":" + port;
        return Math.abs(idStr.hashCode());
    }

    // === FAILURE SIMULATION ===

    @Override
    public void kill() throws RemoteException {
        log("SIMULATION: Node KILLED (Stopping communication)");
        this.isDead = true;
    }

    @Override
    public void revive() throws RemoteException {
        log("SIMULATION: Node REVIVING");

        this.isDead = false;
        this.inCriticalSection = false;
        this.wantCS = false;
        this.logicalClock = 0;
        synchronized (requestQueue) {
            this.requestQueue.clear();
        }
        repliesReceivedForMyRequest.clear();

        List<Node> potentialNeighbors = new ArrayList<>(knownNodes.values());
        this.knownNodes.clear();

        for (Node neighbor : potentialNeighbors) {
            try {
                Map<Long, Node> freshTopology = neighbor.join(this.nodeId, this);

                this.knownNodes.putAll(freshTopology);
                this.knownNodes.remove(this.nodeId);

                this.latestKnownTimestamps.clear();
                for (Long id : knownNodes.keySet()) {
                    latestKnownTimestamps.put(id, 0);
                }

                log("Revive successful! Reconnected via neighbor.");
                return;

            } catch (RemoteException e) { }
        }

        log("Revive failed: No reachable neighbors found. I am isolated.");
    }

    @Override
    public void ensureAlive() throws RemoteException {
        if (isDead) {
            try {
                Thread.sleep(PING_TIMEOUT_MS * 2);
            } catch (InterruptedException ignored) {}
            throw new RemoteException("Node is dead");
        }
    }

    // === LAMPORT'S MUTUAL EXCLUSION ALGORITHM ===

    @Override
    public void enterCS() throws RemoteException {
        if (isDead) return;

        incrementClock();
        wantCS = true;
        myRequestTimestamp = logicalClock;
        log("=== REQUESTING CRITICAL SECTION (My Timestamp: " + myRequestTimestamp + ") ===");

        Request myReq = new Request(nodeId, myRequestTimestamp);
        synchronized (requestQueue) {
            requestQueue.add(myReq);
            log(" Added self to queue: " + requestQueue);
        }

        repliesReceivedForMyRequest.clear();

        broadcast((id, node) -> {
            log(" -> Sending REQUEST to node " + id);
            node.requestCS(nodeId, myRequestTimestamp);
        });

        waitForPermission();

        synchronized (this) {
            inCriticalSection = true;
            log(">>> ENTERED CRITICAL SECTION <<<");
        }
    }

    private synchronized void waitForPermission() {
        while (!canEnterCS()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized boolean canEnterCS() {
        if (!wantCS) return false;
        if (isDead) return false;

        synchronized (requestQueue) {
            if (requestQueue.isEmpty() || requestQueue.peek().nodeId != nodeId) {
                return false;
            }
        }

        return repliesReceivedForMyRequest.size() == knownNodes.size();
    }

    @Override
    public void requestCS(long requestingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        latestKnownTimestamps.put(requestingNodeId, timestamp);

        log("Received REQUEST from " + requestingNodeId + " (ts=" + timestamp + ")");

        synchronized (requestQueue) {
            requestQueue.add(new Request(requestingNodeId, timestamp));
        }

        incrementClock();
        Node requester = knownNodes.get(requestingNodeId);
        if (requester != null) {
            try {
                requester.replyCS(nodeId, logicalClock);
            } catch (RemoteException e) {
                log("  Failed to reply to " + requestingNodeId);
            }
        }

        checkQueueState();
    }

    @Override
    public void replyCS(long replyingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        repliesReceivedForMyRequest.add(replyingNodeId);
        latestKnownTimestamps.put(replyingNodeId, timestamp);

        log("Received REPLY from " + replyingNodeId + " (ts=" + timestamp + ")");

        checkQueueState();
    }

    @Override
    public void releaseCS(long releasingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        latestKnownTimestamps.put(releasingNodeId, timestamp);

        log("Received RELEASE from " + releasingNodeId + " (ts=" + timestamp + ")");

        synchronized (requestQueue) {
            requestQueue.removeIf(r -> r.nodeId == releasingNodeId);
        }

        checkQueueState();
    }

    @Override
    public void leaveCS() throws RemoteException {
        if (!inCriticalSection) {
            log("ERROR: Attempted to leave CS but was not in it.");
            return;
        }

        log("=== LEAVING CRITICAL SECTION ===");
        inCriticalSection = false;
        wantCS = false;

        incrementClock();

        synchronized (requestQueue) {
            requestQueue.removeIf(r -> r.nodeId == nodeId);
        }

        broadcast((id, node) -> {
            node.releaseCS(nodeId, logicalClock);
        });

        repliesReceivedForMyRequest.clear();
        log(">>> LEFT CRITICAL SECTION <<<");
    }

    private synchronized void checkQueueState() {
        notifyAll();
    }

    // === SHARED VARIABLE & TOPOLOGY ===

    @Override
    public synchronized void setSharedVariable(int value) throws RemoteException {
        ensureAlive();
        if (!inCriticalSection) {
            throw new RemoteException("Illegal Access: Must be in Critical Section to write variable!");
        }

        incrementClock();
        this.sharedVariable = value;
        log("Writing Shared Variable: " + value);

        broadcast((id, node) -> node.updateSharedVariable(value, logicalClock, nodeId));
    }

    @Override
    public synchronized void updateSharedVariable(int value, int timestamp, long sourceNodeId) throws RemoteException {
        if (isDead) return;
        updateClock(timestamp);
        this.sharedVariable = value;
        log("Updated Shared Variable from " + sourceNodeId + " to " + value);
    }

    @Override
    public synchronized int getSharedVariable() throws RemoteException {
        ensureAlive();
        return sharedVariable;
    }

    @Override
    public void addNode(long otherNodeId, Node nodeRef) throws RemoteException {
        ensureAlive();
        incrementClock();
        knownNodes.put(otherNodeId, nodeRef);
        latestKnownTimestamps.put(otherNodeId, 0);
        log("Added node " + otherNodeId + " (Total: " + knownNodes.size() + ")");
    }

    @Override
    public void removeNode(long nodeId) throws RemoteException {
        knownNodes.remove(nodeId);
        latestKnownTimestamps.remove(nodeId);
        repliesReceivedForMyRequest.remove(nodeId);

        synchronized (requestQueue) {
            boolean removed = requestQueue.removeIf(r -> r.nodeId == nodeId);
            if (removed) {
                log("Cleaned up pending request from removed node " + nodeId);
            }
        }

        log("Removed node " + nodeId + " from topology (Total nodes: " + knownNodes.size() + ")");

        checkQueueState();
    }

    @Override
    public Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException {
        ensureAlive();
        log("Node " + joiningNodeId + " is joining the network");

        Map<Long, Node> currentTopology = new HashMap<>(knownNodes);
        currentTopology.put(this.nodeId, this);

        this.addNode(joiningNodeId, joiningNodeRef);

        try {
            joiningNodeRef.updateSharedVariable(sharedVariable, logicalClock, nodeId);
        } catch (RemoteException e) {
            log("Error syncing var to new node: " + e.getMessage());
        }

        try {
            List<Request> currentQueue;
            synchronized (requestQueue) {
                currentQueue = new ArrayList<>(requestQueue);
            }
            joiningNodeRef.syncQueue(currentQueue);
        } catch (RemoteException e) {
            log("Error syncing queue to new node: " + e.getMessage());
        }

        for (Map.Entry<Long, Node> entry : currentTopology.entrySet()) {
            if (entry.getKey() != nodeId && entry.getKey() != joiningNodeId) {
                try {
                    entry.getValue().addNode(joiningNodeId, joiningNodeRef);
                } catch (RemoteException e) {
                    log("Error notifying node " + entry.getKey());
                }
            }
        }
        return currentTopology;
    }

    public void joinNetwork(String ip, int port) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(ip, port);
            Node networkNode = (Node) registry.lookup(String.valueOf(port));

            log("Attempting to join network via " + ip + ":" + port);

            Map<Long, Node> networkTopology = networkNode.join(this.nodeId, this);

            for (Map.Entry<Long, Node> entry : networkTopology.entrySet()) {
                if (entry.getKey() != this.nodeId) {
                    this.addNode(entry.getKey(), entry.getValue());
                }
            }

            log("Successfully joined network. Known nodes: " + knownNodes.keySet());

        } catch (Exception e) {
            log("Failed to join network: " + e.getMessage());
            throw new RemoteException("Join network failed", e);
        }
    }

    @Override
    public void syncQueue(List<Request> queueState) throws RemoteException {
        ensureAlive();
        synchronized (requestQueue) {
            requestQueue.clear();
            requestQueue.addAll(queueState);
        }
        log("Synced request queue (Size: " + requestQueue.size() + ")");
    }

    @Override
    public void leave() throws RemoteException {
        if (knownNodes.isEmpty()) return;

        log("LEAVE: Leaving network...");
        for (Node node : knownNodes.values()) {
            try {
                node.removeNode(this.nodeId);
            } catch (RemoteException e) { }
        }
        knownNodes.clear();
        latestKnownTimestamps.clear();
        requestQueue.clear();
        repliesReceivedForMyRequest.clear();
        log("LEAVE: Complete.");
    }

    // === FAILURE DETECTION & HELPERS ===

    @Override
    public void detectDeadNodes() throws RemoteException {
        ensureAlive();
        log("Starting failure detection scan...");

        List<Long> deadNodes = new ArrayList<>();

        // We iterate over a copy of the keys to avoid concurrent modification
        for(Long neighborId : new ArrayList<>(knownNodes.keySet())) {
            Node neighborRef = knownNodes.get(neighborId);
            if(neighborRef == null) continue;

            if (!isNodeReachable(neighborId, neighborRef)) {
                log("TIMEOUT/FAILURE: Node " + neighborId + " is unreachable.");
                deadNodes.add(neighborId);
            }
        }

        for(Long deadId : deadNodes) {
            handleDeadNode(deadId);
        }
    }

    private boolean isNodeReachable(long neighborId, Node neighbor) {
        Future<Boolean> future = failureDetectionExecutor.submit(() -> {
            try {
                neighbor.ensureAlive();
                return true;
            } catch (RemoteException e) {
                return false;
            }
        });

        try {
            return future.get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log("  -> Node " + neighborId + " did not reply within " + PING_TIMEOUT_MS + "ms.");
            future.cancel(true);
            return false;
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    private void handleDeadNode(long deadId) {
        try {
            removeNode(deadId);
        } catch (RemoteException e) { }

        broadcast((id, node) -> node.notifyNodeDead(deadId));
    }

    @Override
    public void notifyNodeDead(long deadNodeId) throws RemoteException {
        if(isDead) return;
        log("Received notification: Node " + deadNodeId + " is dead/gone.");
        removeNode(deadNodeId);
    }

    // === UTILS ===

    private synchronized void incrementClock() {
        logicalClock++;
    }

    private synchronized void updateClock(int receivedTimestamp) {
        logicalClock = Math.max(logicalClock, receivedTimestamp) + 1;
    }

    private void simulateDelay() {
        if (messageDelayMs > 0) {
            try {
                Thread.sleep(messageDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String logLine = String.format("[%s][LC=%d][Node %d] %s",
                timestamp, logicalClock, nodeId, message);
        System.out.println(logLine);
        if (logWriter != null) {
            try {
                logWriter.write(logLine + "\n");
                logWriter.flush();
            } catch (IOException e) { }
        }
    }

    public void shutdown() {
        try { if(logWriter!=null) logWriter.close(); } catch(Exception e){}
    }

    protected void broadcast(NodeOperation operation) {
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            try {
                operation.execute(entry.getKey(), entry.getValue());
            } catch (RemoteException e) {
                log("Broadcasting to " + entry.getKey() + " failed (might be dead).");
            }
        }
    }

    @Override
    public void setMessageDelayMs(int delayMs) throws RemoteException {
        this.messageDelayMs = delayMs;
    }

    @Override
    public int getMessageDelayMs() throws RemoteException {
        return messageDelayMs;
    }

    @Override
    public boolean isInCriticalSection() throws RemoteException {
        return inCriticalSection;
    }

    @Override
    public String getQueueStatus() throws RemoteException {
        return requestQueue.toString();
    }

    @Override
    public List<Long> getKnownNodes() throws RemoteException {
        return new ArrayList<>(knownNodes.keySet());
    }

    @Override
    public long getNodeId() throws RemoteException {
        return nodeId;
    }

    @Override
    public synchronized int getLogicalClock() throws RemoteException {
        return logicalClock;
    }

    @FunctionalInterface
    protected interface NodeOperation {
        void execute(long nodeId, Node node) throws RemoteException;
    }
}