package cz.ctu.fee.dsv.semework;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Implementation of Node interface with Lamport mutual exclusion and failure detection
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    private final long nodeId;
    private int logicalClock;
    private final Map<Long, Node> knownNodes;
    private int sharedVariable;
    private boolean inCriticalSection;
    private int messageDelayMs;
    private final PriorityQueue<Request> requestQueue;
    private FileWriter logWriter;
    private final Set<Long> replySet = new HashSet<>();
    private boolean wantCS = false;
    private int myRequestTimestamp = -1;

    // Failure detection settings
    private static final int PING_TIMEOUT_MS = 3000;

    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public NodeImpl(long nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.logicalClock = 0;
        this.knownNodes = new ConcurrentHashMap<>();
        this.sharedVariable = 0;
        this.inCriticalSection = false;
        this.messageDelayMs = 0;
        this.requestQueue = new PriorityQueue<>();

        try {
            this.logWriter = new FileWriter("node_" + nodeId + ".log", true);
        } catch (IOException e) {
            System.err.println("Failed to create log file for node " + nodeId + ": " + e.getMessage());
            this.logWriter = null;
        }

        log("Node created with ID: " + nodeId);
    }

    public static long generateId(String ip, int port) {
        String[] array = ip.split("\\.");
        long id = 0;
        for (String part : array) {
            long temp = Long.parseLong(part);
            id = (id * 1000) + temp;
        }
        id = id + (long) port * 1000000000000L;
        return id;
    }

    @Override
    public void detectDeadNodes() throws RemoteException {
        incrementClock();
        log("=== Starting failure detection for " + knownNodes.size() + " nodes ===");

        List<Long> deadNodes = new ArrayList<>();

        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            long nodeId = entry.getKey();
            Node node = entry.getValue();

            if (!isNodeAlive(nodeId, node)) {
                log("DETECTED: Node " + nodeId + " is DEAD (no response within " + PING_TIMEOUT_MS + "ms)");
                deadNodes.add(nodeId);
            } else {
                log("OK: Node " + nodeId + " is alive");
            }
        }

        for (long deadNodeId : deadNodes) {
            handleDeadNode(deadNodeId);
        }

        if (deadNodes.isEmpty()) {
            log("=== Failure detection complete: All " + knownNodes.size() + " nodes alive ===");
        } else {
            log("=== Failure detection complete: Removed " + deadNodes.size() + " dead nodes ===");
            log("Remaining nodes in topology: " + knownNodes.size());
        }
    }

    /**
     * Check if a specific node is alive by pinging it with timeout
     */
    private boolean isNodeAlive(long nodeId, Node node) {
        try {
            PingTask pingTask = new PingTask(node);
            Thread pingThread = new Thread(pingTask);
            pingThread.start();

            pingThread.join(PING_TIMEOUT_MS);

            if (pingThread.isAlive()) {
                pingThread.interrupt();
                return false;
            }

            return pingTask.isSuccess();

        } catch (InterruptedException e) {
            log("ERROR: Ping interrupted for node " + nodeId);
            return false;
        }
    }

    /**
     * Handle a dead node: remove locally and broadcast to all other nodes
     */
    private void handleDeadNode(long deadNodeId) {
        log("=== HANDLING DEAD NODE " + deadNodeId + " ===");

        knownNodes.remove(deadNodeId);
        log("Removed node " + deadNodeId + " from local topology");

        log("Broadcasting death of node " + deadNodeId + " to " + knownNodes.size() + " remaining nodes");

        int successCount = 0;
        int failureCount = 0;

        // Make a copy to avoid concurrent modification
        List<Map.Entry<Long, Node>> nodesCopy = new ArrayList<>(knownNodes.entrySet());

        for (Map.Entry<Long, Node> entry : nodesCopy) {
            long otherNodeId = entry.getKey();
            Node otherNode = entry.getValue();

            try {
                log("  → Notifying node " + otherNodeId + " about dead node " + deadNodeId);
                otherNode.notifyNodeDead(deadNodeId);
                successCount++;
            } catch (RemoteException e) {
                log("  ✗ WARNING: Failed to notify node " + otherNodeId + ": " + e.getMessage());
                failureCount++;
            }
        }

        log("Broadcast complete: " + successCount + " notified, " + failureCount + " failed");
        log("=== DEAD NODE " + deadNodeId + " HANDLING COMPLETE ===");
    }

    @Override
    public void notifyNodeDead(long deadNodeId) throws RemoteException {
        incrementClock();
        log("NOTIFICATION: Received death notice for node " + deadNodeId);

        if (knownNodes.containsKey(deadNodeId)) {
            knownNodes.remove(deadNodeId);
            log("✓ Removed dead node " + deadNodeId + " from topology (now " + knownNodes.size() + " nodes)");
        } else {
            log("⚠ Node " + deadNodeId + " was not in my topology (already removed or never existed)");
        }
    }

    /**
     * Helper class to ping a node in a separate thread with timeout support
     */
    private static class PingTask implements Runnable {
        private final Node node;
        private boolean success = false;

        PingTask(Node node) {
            this.node = node;
        }

        @Override
        public void run() {
            try {
                node.ping();
                success = true;
            } catch (RemoteException e) {
                success = false;
            }
        }

        boolean isSuccess() {
            return success;
        }
    }

    // === Helper methods ===

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String logLine = String.format("[%s][LC=%d][Node %d] %s",
                timestamp, logicalClock, nodeId, message);
        System.out.println(logLine);
        if (logWriter != null) {
            try {
                logWriter.write(logLine + "\n");
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("Failed to write to log file: " + e.getMessage());
            }
        }
    }

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
                log("Message delayed by " + messageDelayMs + "ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // === Node interface implementation ===

    @Override
    public long getNodeId() throws RemoteException {
        return nodeId;
    }

    @Override
    public synchronized int getLogicalClock() throws RemoteException {
        return logicalClock;
    }

    @Override
    public void addNode(long otherNodeId, Node nodeRef) throws RemoteException {
        incrementClock();
        knownNodes.put(otherNodeId, nodeRef);
        log("Added node " + otherNodeId + " (Total: " + knownNodes.size() + ")");
    }

    @Override
    public void removeNode(long nodeId) throws RemoteException {
        incrementClock();
        knownNodes.remove(nodeId);
        log("Removed node " + nodeId + " (Total nodes: " + knownNodes.size() + ")");
    }

    @Override
    public void leave() throws RemoteException {
        incrementClock();

        if (knownNodes.isEmpty()) {
            log("LEAVE: Node " + nodeId + " is already isolated. Nothing to do.");
            return;
        }

        log("=== LEAVE: Starting graceful departure from network ===");
        log("Current topology size: " + knownNodes.size() + " nodes");

        List<Map.Entry<Long, Node>> nodesCopy = new ArrayList<>(knownNodes.entrySet());

        log("Step 1: Notifying all nodes to remove node " + nodeId);
        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<Long, Node> entry : nodesCopy) {
            long otherNodeId = entry.getKey();
            Node otherNode = entry.getValue();

            try {
                log("  Notifying node " + otherNodeId + " to remove node " + nodeId);
                otherNode.removeNode(this.nodeId);
                successCount++;
            } catch (RemoteException e) {
                log("  WARNING: Failed to notify node " + otherNodeId + ": " + e.getMessage());
                failureCount++;
            }
        }

        log("Notification complete: " + successCount + " successful, " + failureCount + " failed");

        log("Step 2: Clearing local topology");
        int previousSize = knownNodes.size();
        knownNodes.clear();
        log("Cleared " + previousSize + " nodes from local topology");

        if (!requestQueue.isEmpty()) {
            log("Step 3: Clearing " + requestQueue.size() + " pending requests from queue");
            requestQueue.clear();
        }

        log("=== LEAVE: Node " + nodeId + " has successfully left the network ===");
    }

    @Override
    public List<Long> getKnownNodes() throws RemoteException {
        return new ArrayList<>(knownNodes.keySet());
    }

    @Override
    public Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException {
        incrementClock();
        log("Node " + joiningNodeId + " is joining the network");

        Map<Long, Node> existingNodes = new HashMap<>(knownNodes);
        existingNodes.put(this.nodeId, this);

        knownNodes.put(joiningNodeId, joiningNodeRef);
        log("Added joining node " + joiningNodeId + " to my topology");

        try {
            log("Synchronizing shared variable to joining node " + joiningNodeId +
                    " (value=" + sharedVariable + ")");
            joiningNodeRef.updateSharedVariable(sharedVariable, logicalClock, nodeId);
        } catch (RemoteException e) {
            log("ERROR: Failed to sync shared variable to node " + joiningNodeId + ": " + e.getMessage());
        }

        // 4) Inform all existing nodes about the new node
        log("Broadcasting new node " + joiningNodeId + " to all existing nodes");
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            long existingNodeId = entry.getKey();
            Node existingNode = entry.getValue();

            if (existingNodeId != joiningNodeId) {
                try {
                    log("  Telling node " + existingNodeId + " to add node " + joiningNodeId);
                    existingNode.addNode(joiningNodeId, joiningNodeRef);
                } catch (RemoteException e) {
                    log("  ERROR: Failed to notify node " + existingNodeId + ": " + e.getMessage());
                }
            }
        }

        log("Join complete. Returning " + existingNodes.size() + " existing nodes");
        return existingNodes;
    }

    public void enterCS() throws RemoteException {
        incrementClock();
        wantCS = true;
        myRequestTimestamp = logicalClock;

        log("=== REQUESTING CRITICAL SECTION (timestamp=" + myRequestTimestamp + ") ===");

        // Add my own request to queue
        requestQueue.add(new Request(nodeId, myRequestTimestamp));

        // Clear old replies
        replySet.clear();

        // Broadcast REQUEST to all nodes
        broadcast((id, node) -> {
            log("  → Sending REQUEST to node " + id);
            node.requestCS(nodeId, myRequestTimestamp);
        });

        // Wait for permission
        waitForPermission();

        // Enter CS
        inCriticalSection = true;
        log(">>> ENTERED CRITICAL SECTION <<<");
    }

    private synchronized void waitForPermission() {
        while (!canEnterCS()) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized boolean canEnterCS() {
        if (!wantCS) return false;

        // Check: my request is first in queue
        Request first = requestQueue.peek();
        if (first == null || first.nodeId != nodeId) {
            return false;
        }

        // Check: have replies from ALL other nodes
        return replySet.size() == knownNodes.size();
    }

    @Override
    public void requestCS(long requestingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);

        log("Received REQUEST from node " + requestingNodeId + " (ts=" + timestamp + ")");

        // Add to queue
        synchronized(requestQueue) {
            requestQueue.add(new Request(requestingNodeId, timestamp));
            log("  Queue now: " + requestQueue);
        }

        // Send REPLY immediately
        incrementClock();
        Node requester = knownNodes.get(requestingNodeId);
        if (requester != null) {
            log("  → Sending REPLY to node " + requestingNodeId);
            requester.replyCS(nodeId, logicalClock);
        }
    }

    @Override
    public void replyCS(long replyingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);

        log("Received REPLY from node " + replyingNodeId + " (ts=" + timestamp + ")");

        synchronized(this) {
            replySet.add(replyingNodeId);
            log("  Reply count: " + replySet.size() + "/" + knownNodes.size());
            notifyAll(); // Wake up waiting thread
        }
    }

    @Override
    public void releaseCS(long releasingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);

        log("Received RELEASE from node " + releasingNodeId + " (ts=" + timestamp + ")");

        synchronized(requestQueue) {
            requestQueue.removeIf(r -> r.nodeId == releasingNodeId);
            log("  Queue after removal: " + requestQueue);
        }
    }

    public void leaveCS() throws RemoteException {
        if (!inCriticalSection) {
            log("ERROR: Not in critical section!");
            return;
        }

        incrementClock();
        log("=== LEAVING CRITICAL SECTION ===");

        inCriticalSection = false;
        wantCS = false;

        // Remove my request from queue
        synchronized(requestQueue) {
            requestQueue.removeIf(r -> r.nodeId == nodeId);
        }

        // Broadcast RELEASE
        broadcast((id, node) -> {
            log("  → Sending RELEASE to node " + id);
            node.releaseCS(nodeId, logicalClock);
        });

        log(">>> LEFT CRITICAL SECTION <<<");
    }

    @Override
    public synchronized int getSharedVariable() throws RemoteException {
        incrementClock();
        log("Reading shared variable: " + sharedVariable);
        return sharedVariable;
    }

    @Override
    public synchronized void setSharedVariable(int value) throws RemoteException {
        if (!inCriticalSection) {
            log("ERROR: setSharedVariable called outside CS");
            return;
        }

        incrementClock();
        log("Writing shared variable: " + sharedVariable + " -> " + value);
        sharedVariable = value;

        // BROADCAST UPDATE
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            try {
                entry.getValue().updateSharedVariable(value, logicalClock, nodeId);
            } catch (RemoteException e) {
                log("Failed to propagate value to node " + entry.getKey());
            }
        }
    }


    @Override
    public synchronized void updateSharedVariable(int value, int timestamp, long sourceNodeId)
            throws RemoteException {
        updateClock(timestamp);
        log("UPDATE from node " + sourceNodeId + ": sharedVariable = " + value);
        this.sharedVariable = value;
    }


    @Override
    public void setMessageDelayMs(int delayMs) throws RemoteException {
        incrementClock();
        this.messageDelayMs = delayMs;
        log("Message delay set to: " + delayMs + "ms");
    }

    @Override
    public int getMessageDelayMs() throws RemoteException {
        return messageDelayMs;
    }

    @Override
    public synchronized boolean isInCriticalSection() throws RemoteException {
        return inCriticalSection;
    }

    @Override
    public String getQueueStatus() throws RemoteException {
        return "Queue size: " + requestQueue.size() + ", In CS: " + inCriticalSection;
    }

    @Override
    public void ping() throws RemoteException {
        incrementClock();
    }

    public void shutdown() throws RemoteException {
        log("Node shutting down");
        if (logWriter != null) {
            try {
                logWriter.close();
                logWriter = null;
            } catch (IOException e) {
                System.err.println("Error closing log file: " + e.getMessage());
            }
        }
    }

    protected void broadcast(NodeOperation operation) {
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            try {
                operation.execute(entry.getKey(), entry.getValue());
            } catch (RemoteException e) {
                log("ERROR: Failed to send to node " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    protected interface NodeOperation {
        void execute(long nodeId, Node node) throws RemoteException;
    }

    protected static class Request implements Comparable<Request> {
        final long nodeId;
        final int timestamp;

        Request(long nodeId, int timestamp) {
            this.nodeId = nodeId;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Request other) {
            if (this.timestamp != other.timestamp) {
                return Integer.compare(this.timestamp, other.timestamp);
            }
            return Long.compare(this.nodeId, other.nodeId);
        }

        @Override
        public String toString() {
            return String.format("Request{node=%d, ts=%d}", nodeId, timestamp);
        }
    }
}