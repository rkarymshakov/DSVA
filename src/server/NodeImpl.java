package server;

import compute.Node;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Basic implementation of Node interface
 * This is a foundation - Lamport algorithm logic will be added later
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    // Node identification
    private final String nodeId;
    private int logicalClock;

    // Topology - complete graph (all nodes know each other)
    private final Map<String, Node> knownNodes;

    // Shared variable (simulated as local copy - in reality each node will sync this)
    private int sharedVariable;

    // Critical section state
    private boolean inCriticalSection;

    // Message delay simulation
    private int messageDelayMs;

    // Request queue for Lamport algorithm (will be implemented later)
    private final PriorityQueue<Request> requestQueue;

    // Time formatter for logging
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Constructor
     * @param nodeId Unique identifier (e.g., "nodeA@192.168.1.10:1099")
     */
    public NodeImpl(String nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.logicalClock = 0;
        this.knownNodes = new ConcurrentHashMap<>();
        this.sharedVariable = 0;
        this.inCriticalSection = false;
        this.messageDelayMs = 0;
        this.requestQueue = new PriorityQueue<>();

        log("Node created: " + nodeId);
    }

    // === Helper methods ===

    /**
     * Log message with timestamp
     */
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        System.out.println(String.format("[%s][LC=%d][%s] %s",
                timestamp, logicalClock, nodeId, message));
    }

    /**
     * Increment logical clock (Lamport rule)
     */
    private synchronized void incrementClock() {
        logicalClock++;
    }

    /**
     * Update logical clock on message receive (Lamport rule: max(local, received) + 1)
     */
    private synchronized void updateClock(int receivedTimestamp) {
        logicalClock = Math.max(logicalClock, receivedTimestamp) + 1;
    }

    /**
     * Simulate network delay if configured
     */
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
    public String getNodeId() throws RemoteException {
        return nodeId;
    }

    @Override
    public synchronized int getLogicalClock() throws RemoteException {
        return logicalClock;
    }

    @Override
    public void addNode(String nodeId, Node nodeRef) throws RemoteException {
        knownNodes.put(nodeId, nodeRef);
        log("Added node to topology: " + nodeId + " (Total nodes: " + knownNodes.size() + ")");
    }

    @Override
    public void removeNode(String nodeId) throws RemoteException {
        knownNodes.remove(nodeId);
        log("Removed node from topology: " + nodeId + " (Total nodes: " + knownNodes.size() + ")");
    }

    @Override
    public List<String> getKnownNodes() throws RemoteException {
        return new ArrayList<>(knownNodes.keySet());
    }

    @Override
    public void requestCS(String requestingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS REQUEST from " + requestingNodeId + " with timestamp " + timestamp);

        // TODO: Implement Lamport algorithm logic here
        // For now, just acknowledge we received the request
    }

    @Override
    public void replyCS(String replyingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS REPLY from " + replyingNodeId + " with timestamp " + timestamp);

        // TODO: Implement Lamport algorithm logic here
    }

    @Override
    public void releaseCS(String releasingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS RELEASE from " + releasingNodeId + " with timestamp " + timestamp);

        // TODO: Implement Lamport algorithm logic here
    }

    @Override
    public synchronized int getSharedVariable() throws RemoteException {
        log("Reading shared variable: " + sharedVariable);
        return sharedVariable;
    }

    @Override
    public synchronized void setSharedVariable(int value) throws RemoteException {
        log("Writing shared variable: " + sharedVariable + " -> " + value);
        sharedVariable = value;
    }

    @Override
    public void setMessageDelayMs(int delayMs) throws RemoteException {
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
        log("Ping received");
    }

    /**
     * Helper method to broadcast a message to all known nodes
     */
    protected void broadcast(NodeOperation operation) {
        for (Map.Entry<String, Node> entry : knownNodes.entrySet()) {
            try {
                operation.execute(entry.getKey(), entry.getValue());
            } catch (RemoteException e) {
                log("ERROR: Failed to send message to " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Functional interface for broadcast operations
     */
    @FunctionalInterface
    protected interface NodeOperation {
        void execute(String nodeId, Node node) throws RemoteException;
    }

    /**
     * Inner class to represent a CS request (for priority queue)
     */
    protected static class Request implements Comparable<Request> {
        final String nodeId;
        final int timestamp;

        Request(String nodeId, int timestamp) {
            this.nodeId = nodeId;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Request other) {
            if (this.timestamp != other.timestamp) {
                return Integer.compare(this.timestamp, other.timestamp);
            }
            // If timestamps equal, use node ID for deterministic ordering
            return this.nodeId.compareTo(other.nodeId);
        }

        @Override
        public String toString() {
            return String.format("Request{node=%s, ts=%d}", nodeId, timestamp);
        }
    }
}