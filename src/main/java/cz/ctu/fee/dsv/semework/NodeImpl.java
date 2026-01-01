package cz.ctu.fee.dsv.semework;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class NodeImpl extends UnicastRemoteObject implements Node {
    private static final int PING_TIMEOUT_MS = 3000;

    private final long nodeId;
    private final Map<Long, Node> knownNodes;
    private final Set<Long> repliesReceivedForMyRequest = new HashSet<>();
    private final PriorityQueue<Request> requestQueue;
    private final ExecutorService failureDetectionExecutor = Executors.newCachedThreadPool();  // Executor for handling async pings (avoids freezing the main thread)
    private final Logger logger;
    private final FileWriter logWriter;

    private int logicalClock;
    private int sharedVariable;
    private int messageDelayMs;
    private boolean inCriticalSection;
    private boolean wantCS = false;
    private boolean isDead = false;

    public NodeImpl(long nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.logicalClock = 0;
        this.knownNodes = new ConcurrentHashMap<>();
        this.sharedVariable = 0;
        this.inCriticalSection = false;
        this.messageDelayMs = 0;
        this.requestQueue = new PriorityQueue<>();

        FileWriter writer = null;
        try {
            writer = new FileWriter("node_" + nodeId + ".log", true);
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
        this.logWriter = writer;
        this.logger = new Logger(nodeId, logWriter);

        logger.logInfo("Node created with ID: " + nodeId, logicalClock);
    }

//    public static long generateId(String ip, int port) {
//        String idStr = ip + ":" + port;
//        return Math.abs(idStr.hashCode());
//    }

    public static long generateId(String address, int port) {
        String[] array = address.split("\\.");
        long id = 0;
        long shift = 0, temp = 0;
        for(int i = 0 ; i < array.length; i++){
            temp = Long.parseLong(array[i]);
            id = (long) (id * 1000);
            id += temp;
        }
        if (id == 0) {
            long fallbackId = Math.abs(address.hashCode());
            id = fallbackId + port * 1000000000000L;
        }
        id = id + port*1000000000000l;
        return id;
    }


    @Override
    public long getNodeId() throws RemoteException {
        return nodeId;
    }

    @Override
    public synchronized int getLogicalClock() throws RemoteException {
        return logicalClock;
    }

    @Override
    public Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException {
        ensureAlive();
        logger.logInfo("Node " + joiningNodeId + " is joining the network", logicalClock);

        Map<Long, Node> currentTopology = new HashMap<>(knownNodes);
        currentTopology.put(this.nodeId, this);

        this.addNode(joiningNodeId, joiningNodeRef);

        try {
            joiningNodeRef.updateSharedVariable(sharedVariable, logicalClock, nodeId);
        } catch (RemoteException e) {
            logger.logError("Error syncing var to new node: " + e.getMessage(), logicalClock);
        }

        try {
            List<Request> currentQueue;
            synchronized (requestQueue) { currentQueue = new ArrayList<>(requestQueue); }
            joiningNodeRef.syncQueue(currentQueue);
        } catch (RemoteException e) {
            logger.logError("Error syncing queue to new node: " + e.getMessage(), logicalClock);
        }

        for (Map.Entry<Long, Node> entry : currentTopology.entrySet()) {
            if (entry.getKey() != nodeId && entry.getKey() != joiningNodeId) {
                try {
                    entry.getValue().addNode(joiningNodeId, joiningNodeRef);
                } catch (RemoteException e) {
                    logger.logError("Error notifying node " + entry.getKey(), logicalClock);
                }
            }
        }
        return currentTopology;
    }

    public void joinNetwork(String ip, int port) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(ip, port);
            Node networkNode = (Node) registry.lookup(String.valueOf(port));

            logger.logInfo("Attempting to join network via " + ip + ":" + port, logicalClock);
            Map<Long, Node> networkTopology = networkNode.join(this.nodeId, this);

            for (Map.Entry<Long, Node> entry : networkTopology.entrySet())
                if (entry.getKey() != this.nodeId)
                    this.addNode(entry.getKey(), entry.getValue());

            logger.logInfo("Successfully joined network. Known nodes: " + knownNodes.keySet(), logicalClock);
        } catch (Exception e) {
            logger.logError("Failed to join network: " + e.getMessage(), logicalClock);
            throw new RemoteException("Join network failed", e);
        }
    }

    @Override
    public void addNode(long otherNodeId, Node nodeRef) throws RemoteException {
        ensureAlive();
        incrementClock();
        knownNodes.put(otherNodeId, nodeRef);
        logger.logInfo("Added node " + otherNodeId + " (Total: " + knownNodes.size() + ")", logicalClock);
    }

    @Override
    public void removeNode(long nodeId) throws RemoteException {
        knownNodes.remove(nodeId);
        repliesReceivedForMyRequest.remove(nodeId);

        synchronized (requestQueue) {
            boolean removed = requestQueue.removeIf(r -> r.nodeId == nodeId);
            if (removed)
                logger.logInfo("Cleaned up pending request from removed node " + nodeId, logicalClock);
        }
        logger.logInfo("Removed node " + nodeId + " from topology (Total nodes: " + knownNodes.size() + ")", logicalClock);
        synchronized (this) { notifyAll(); }
    }

    @Override
    public void leave() throws RemoteException {
        if (knownNodes.isEmpty()) return;
        logger.logInfo("LEAVE: Leaving network...", logicalClock);

        List<Node> nodesToNotify = new ArrayList<>(knownNodes.values());
        knownNodes.clear();
        synchronized (requestQueue) { requestQueue.clear(); }
        repliesReceivedForMyRequest.clear();

        for (Node node : nodesToNotify)
            try { node.removeNode(this.nodeId); } catch (RemoteException ignored) {}

        logger.logInfo("LEAVE: Complete.", logicalClock);
    }

    @Override
    public List<Long> getKnownNodes() throws RemoteException {
        return new ArrayList<>(knownNodes.keySet());
    }

    @Override
    public void enterCS() throws RemoteException {
        ensureAlive();

        incrementClock();
        wantCS = true;
        logger.logInfo("REQUESTING CRITICAL SECTION (My Timestamp: " + logicalClock + ")", logicalClock);

        Request myReq = new Request(nodeId, logicalClock);
        synchronized (requestQueue) {
            requestQueue.add(myReq);
            logger.logInfo(" Added self to queue: " + requestQueue, logicalClock);
        }

        repliesReceivedForMyRequest.clear();

        broadcast((id, node) -> {
            logger.logInfo(" -> Sending REQUEST to node " + id, logicalClock);
            node.requestCS(nodeId, logicalClock);
        });

        waitForPermission();

        synchronized (this) {
            inCriticalSection = true;
            logger.logInfo("ENTERED CRITICAL SECTION", logicalClock);
        }
    }

    @Override
    public void requestCS(long requestingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        logger.logInfo("Received REQUEST from " + requestingNodeId + " (ts=" + timestamp + ")", logicalClock);

        synchronized (requestQueue) { requestQueue.add(new Request(requestingNodeId, timestamp)); }

        incrementClock();
        Node requester = knownNodes.get(requestingNodeId);
        if (requester != null) {
            try { requester.replyCS(nodeId, logicalClock); }
            catch (RemoteException e) { logger.logError("  Failed to reply to " + requestingNodeId, logicalClock); }
        }
        synchronized (this) { notifyAll(); }
    }

    @Override
    public void replyCS(long replyingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        repliesReceivedForMyRequest.add(replyingNodeId);
        logger.logInfo("Received REPLY from " + replyingNodeId + " (ts=" + timestamp + ")", logicalClock);
        synchronized (this) { notifyAll(); }
    }

    @Override
    public void releaseCS(long releasingNodeId, int timestamp) throws RemoteException {
        ensureAlive();
        simulateDelay();
        updateClock(timestamp);

        logger.logInfo("Received RELEASE from " + releasingNodeId + " (ts=" + timestamp + ")", logicalClock);
        synchronized (requestQueue) { requestQueue.removeIf(r -> r.nodeId == releasingNodeId); }
        synchronized (this) { notifyAll(); }
    }

    @Override
    public void leaveCS() throws RemoteException {
        if (!inCriticalSection) {
            logger.logError("ERROR: Attempted to leave CS but was not in it.", logicalClock);
            return;
        }
        logger.logInfo("LEAVING CRITICAL SECTION", logicalClock);
        inCriticalSection = false;
        wantCS = false;
        incrementClock();

        synchronized (requestQueue) { requestQueue.removeIf(r -> r.nodeId == nodeId); }
        broadcast((id, node) -> node.releaseCS(nodeId, logicalClock));
        repliesReceivedForMyRequest.clear();
        logger.logInfo("LEFT CRITICAL SECTION", logicalClock);
    }

    @Override
    public synchronized int getSharedVariable() throws RemoteException {
        ensureAlive();
        return sharedVariable;
    }

    @Override
    public synchronized void setSharedVariable(int value) throws RemoteException {
        ensureAlive();
        if (!inCriticalSection)
            throw new RemoteException("Illegal Access: Must be in Critical Section to write variable!");

        incrementClock();
        this.sharedVariable = value;
        logger.logInfo("Writing Shared Variable: " + value, logicalClock);
        broadcast((id, node) -> node.updateSharedVariable(value, logicalClock, nodeId));
    }

    @Override
    public synchronized void updateSharedVariable(int value, int timestamp, long sourceNodeId) throws RemoteException {
        ensureAlive();
        updateClock(timestamp);
        this.sharedVariable = value;
        logger.logInfo("Updated Shared Variable from " + sourceNodeId + " to " + value, logicalClock);
    }

    @Override
    public void setMessageDelayMs(int delayMs) throws RemoteException { this.messageDelayMs = delayMs; }

    @Override
    public int getMessageDelayMs() throws RemoteException { return messageDelayMs; }

    @Override
    public void kill() throws RemoteException {
        logger.logInfo("SIMULATION: Node KILLED (Stopping communication)", logicalClock);
        this.isDead = true;
    }

    @Override
    public void revive() throws RemoteException {
        logger.logInfo("SIMULATION: Node REVIVING", logicalClock);

        this.isDead = false;
        this.inCriticalSection = false;
        this.wantCS = false;
        this.logicalClock = 0;
        synchronized (requestQueue) { this.requestQueue.clear(); }
        repliesReceivedForMyRequest.clear();

        List<Node> potentialNeighbors = new ArrayList<>(knownNodes.values());
        this.knownNodes.clear();

        for (Node neighbor : potentialNeighbors) {
            try {
                Map<Long, Node> freshTopology = neighbor.join(this.nodeId, this);
                this.knownNodes.putAll(freshTopology);
                this.knownNodes.remove(this.nodeId);
                logger.logInfo("Revive successful! Reconnected via neighbor.", logicalClock);
                return;
            } catch (RemoteException ignored) {}
        }
        logger.logError("Revive failed: No reachable neighbors found. I am isolated.", logicalClock);
    }

    @Override
    public void syncQueue(List<Request> queueState) throws RemoteException {
        ensureAlive();
        synchronized (requestQueue) {
            requestQueue.clear();
            requestQueue.addAll(queueState);
        }
        logger.logInfo("Synced request queue (Size: " + requestQueue.size() + ")", logicalClock);
    }

    @Override
    public boolean isInCriticalSection() throws RemoteException { return inCriticalSection; }

    @Override
    public String getQueueStatus() throws RemoteException { synchronized (requestQueue) { return requestQueue.toString(); } }

    @Override
    public void ensureAlive() throws RemoteException {
        if (isDead) {
            try { Thread.sleep(PING_TIMEOUT_MS * 2); } catch (InterruptedException ignored) {}
            throw new RemoteException("Node is dead");
        }
    }

    @Override
    public void detectDeadNodes() throws RemoteException {
        ensureAlive();
        logger.logInfo("Starting failure detection scan...", logicalClock);
        List<Long> deadNodes = new ArrayList<>();

        for (Long neighborId : new ArrayList<>(knownNodes.keySet())) {
            Node neighborRef = knownNodes.get(neighborId);
            if (neighborRef == null) continue;

            if (!isNodeReachable(neighborId, neighborRef)) {
                logger.logInfo("TIMEOUT/FAILURE: Node " + neighborId + " is unreachable.", logicalClock);
                deadNodes.add(neighborId);
            }
        }
        for (Long deadId : deadNodes) { handleDeadNode(deadId); }
    }

    @Override
    public void notifyNodeDead(long deadNodeId) throws RemoteException {
        ensureAlive();
        logger.logInfo("Received notification: Node " + deadNodeId + " is dead/gone.", logicalClock);
        removeNode(deadNodeId);
    }

    private synchronized void waitForPermission() {
        while (!canEnterCS()) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private synchronized boolean canEnterCS() {
        if (!wantCS || isDead) return false;
        synchronized (requestQueue) { return !requestQueue.isEmpty() && requestQueue.peek().nodeId == nodeId; }
    }

    private boolean isNodeReachable(long neighborId, Node neighbor) {
        Future<Boolean> future = failureDetectionExecutor.submit(() -> {
            try { neighbor.ensureAlive(); return true; }
            catch (RemoteException e) { return false; }
        });

        try { return future.get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS); }
        catch (TimeoutException e) {
            logger.logInfo("  -> Node " + neighborId + " did not reply within " + PING_TIMEOUT_MS + "ms.", logicalClock);
            future.cancel(true);
            return false;
        } catch (InterruptedException | ExecutionException e) { return false; }
    }

    private void handleDeadNode(long deadId) {
        try { removeNode(deadId); } catch (RemoteException ignored) {}
        broadcast((id, node) -> node.notifyNodeDead(deadId));
    }

    private synchronized void incrementClock() { logicalClock++; }

    private synchronized void updateClock(int receivedTimestamp) { logicalClock = Math.max(logicalClock, receivedTimestamp) + 1; }

    private void simulateDelay() {
        if (messageDelayMs > 0) {
            try { Thread.sleep(messageDelayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    protected void broadcast(NodeOperation operation) {
        for (Map.Entry<Long, Node> entry : knownNodes.entrySet()) {
            try { operation.execute(entry.getKey(), entry.getValue()); }
            catch (RemoteException e) { logger.logError("Broadcasting to " + entry.getKey() + " failed (might be dead).", logicalClock); }
        }
    }

    public void shutdown() { logger.close(); }

    @FunctionalInterface
    protected interface NodeOperation { void execute(long nodeId, Node node) throws RemoteException; }
}
