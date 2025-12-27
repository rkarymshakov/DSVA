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
 * Implementation of Node interface with Lamport mutual exclusion foundation
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    // Node identification - numeric ID (like in example project)
    private final long nodeId;
    private int logicalClock;

    // Topology - complete graph (key = numeric node ID)
    private final Map<Long, Node> knownNodes;

    // Shared variable
    private int sharedVariable;

    // Critical section state
    private boolean inCriticalSection;

    // Message delay simulation
    private int messageDelayMs;

    // Request queue for Lamport algorithm
    private final PriorityQueue<Request> requestQueue;

    // File logging
    private FileWriter logWriter;

    // Time formatter for logging
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

        // Initialize log file
        try {
            this.logWriter = new FileWriter("node_" + nodeId + ".log", true);
        } catch (IOException e) {
            System.err.println("Failed to create log file for node " + nodeId + ": " + e.getMessage());
            this.logWriter = null;
        }

        log("Node created with ID: " + nodeId);
    }

    // === Generate numeric ID exactly like in example ===
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

    // === Helper methods ===

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String logLine = String.format("[%s][LC=%d][Node %d] %s",
                timestamp, logicalClock, nodeId, message);

        // Write to console
        System.out.println(logLine);

        // Write to file
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
        log("=== LEAVE: Starting graceful departure from network ===");
        log("Current topology size: " + knownNodes.size() + " nodes");

        // Get a copy of all known nodes before we start modifying the collection
        List<Map.Entry<Long, Node>> nodesCopy = new ArrayList<>(knownNodes.entrySet());

        // Step 1: Notify all other nodes to remove this node from their topology
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

        // Step 2: Clear this node's topology
        log("Step 2: Clearing local topology");
        int previousSize = knownNodes.size();
        knownNodes.clear();
        log("Cleared " + previousSize + " nodes from local topology");

        // Step 3: Clear any pending requests in the queue
        if (!requestQueue.isEmpty()) {
            log("Step 3: Clearing " + requestQueue.size() + " pending requests from queue");
            requestQueue.clear();
        }

        log("=== LEAVE: Node " + nodeId + " has successfully left the network ===");
        log("Final topology size: " + knownNodes.size() + " nodes");
    }

    @Override
    public List<Long> getKnownNodes() throws RemoteException {
        return new ArrayList<>(knownNodes.keySet());
    }

    @Override
    public Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException {
        incrementClock();
        log("Node " + joiningNodeId + " is joining the network");

        // Get a copy of current nodes to return to the joining node
        Map<Long, Node> existingNodes = new HashMap<>(knownNodes);

        // Add myself to the map so joining node knows about me too
        existingNodes.put(this.nodeId, this);

        // Add the joining node to my topology
        knownNodes.put(joiningNodeId, joiningNodeRef);
        log("Added joining node " + joiningNodeId + " to my topology");

        // Broadcast to ALL existing nodes: add the new node to their topology
        log("Broadcasting new node " + joiningNodeId + " to all existing nodes");
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            long existingNodeId = entry.getKey();
            Node existingNode = entry.getValue();

            // Don't send to the joining node itself
            if (existingNodeId != joiningNodeId) {
                try {
                    log("  Telling node " + existingNodeId + " to add node " + joiningNodeId);
                    existingNode.addNode(joiningNodeId, joiningNodeRef);
                } catch (RemoteException e) {
                    log("  ERROR: Failed to notify node " + existingNodeId + ": " + e.getMessage());
                }
            }
        }

        log("Join complete. Returning " + existingNodes.size() + " existing nodes to joining node");
        return existingNodes;
    }

    @Override
    public void requestCS(long requestingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS REQUEST from node " + requestingNodeId + " with timestamp " + timestamp);
        // TODO: Implement full Lamport logic here
    }

    @Override
    public void replyCS(long replyingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS REPLY from node " + replyingNodeId + " with timestamp " + timestamp);
        // TODO: Implement full Lamport logic here
    }

    @Override
    public void releaseCS(long releasingNodeId, int timestamp) throws RemoteException {
        simulateDelay();
        updateClock(timestamp);
        log("Received CS RELEASE from node " + releasingNodeId + " with timestamp " + timestamp);
        // TODO: Implement full Lamport logic here
    }

    @Override
    public synchronized int getSharedVariable() throws RemoteException {
        incrementClock();
        log("Reading shared variable: " + sharedVariable);
        return sharedVariable;
    }

    @Override
    public synchronized void setSharedVariable(int value) throws RemoteException {
        incrementClock();
        log("Writing shared variable: " + sharedVariable + " -> " + value);
        sharedVariable = value;
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
        log("Ping received");
    }

    // === Cleanup method ===
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

    // === Broadcast helper ===
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

    // === Request class for priority queue ===
    protected static class Request implements Comparable<Request> {
        final long nodeId;  // now long
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