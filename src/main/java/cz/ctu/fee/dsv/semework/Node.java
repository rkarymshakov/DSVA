package cz.ctu.fee.dsv.semework;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote interface for distributed node implementing Lamport's mutual exclusion algorithm
 * with shared variable functionality and failure detection.
 */
public interface Node extends Remote {

    // === Node identification ===
    long getNodeId() throws RemoteException;
    int getLogicalClock() throws RemoteException;

    // === Topology management ===
    Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException;
    void addNode(long nodeId, Node nodeRef) throws RemoteException;
    void removeNode(long nodeId) throws RemoteException;
    void leave() throws RemoteException;
    List<Long> getKnownNodes() throws RemoteException;

    // === Lamport's algorithm - Critical Section messages ===
    void requestCS(long requestingNodeId, int timestamp) throws RemoteException;
    void replyCS(long replyingNodeId, int timestamp) throws RemoteException;
    void releaseCS(long releasingNodeId, int timestamp) throws RemoteException;
    void enterCS() throws RemoteException;
    void leaveCS() throws RemoteException;

    // === Shared Variable operations ===
    int getSharedVariable() throws RemoteException;
    void setSharedVariable(int value) throws RemoteException;
    void updateSharedVariable(int value, int timestamp, long sourceNodeId) throws RemoteException;

    // === Testing and simulation features ===
    void setMessageDelayMs(int delayMs) throws RemoteException;
    int getMessageDelayMs() throws RemoteException;

    // === FAILURE SIMULATION (Required by Assignment) ===
    /**
     * Simulates a node crash (uncorrect disconnection).
     * The node should stop responding to algorithm messages.
     */
    void kill() throws RemoteException;

    /**
     * Revives a previously killed node.
     * The node resumes processing messages.
     */
    void revive() throws RemoteException;

    public void syncQueue(List<Request> queueState) throws RemoteException;

    // === Status and debugging ===
    boolean isInCriticalSection() throws RemoteException;
    String getQueueStatus() throws RemoteException;
    void checkSimulationStatus() throws RemoteException;

    // === FAILURE DETECTION ===
    /**
     * Check all known nodes for liveness. Remove dead nodes and notify others.
     */
    void detectDeadNodes() throws RemoteException;

    /**
     * Notification that a node is dead - receiver should remove it from topology
     * @param deadNodeId ID of the dead node
     */
    void notifyNodeDead(long deadNodeId) throws RemoteException;
}