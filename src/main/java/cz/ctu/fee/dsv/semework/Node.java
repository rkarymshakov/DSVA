package cz.ctu.fee.dsv.semework;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for distributed node implementing Lamport's mutual exclusion algorithm
 * with shared variable functionality
 */
public interface Node extends Remote {

    // === Node identification ===
    /**
     * Get unique numeric identifier of this node (generated from IP + port)
     */
    long getNodeId() throws RemoteException;

    /**
     * Get the current logical clock value (Lamport timestamp)
     */
    int getLogicalClock() throws RemoteException;

    // === Topology management ===
    /**
     * Register another node in this node's topology
     * @param nodeId Unique numeric identifier of the node to add
     * @param nodeRef Remote reference to the node
     */
    void addNode(long nodeId, Node nodeRef) throws RemoteException;

    /**
     * Remove a node from topology
     * @param nodeId Numeric identifier of node to remove
     */
    void removeNode(long nodeId) throws RemoteException;

    /**
     * Get list of all known nodes in the system (their numeric IDs)
     */
    List<Long> getKnownNodes() throws RemoteException;

    // === Lamport's algorithm - Critical Section messages ===
    /**
     * Request access to critical section
     * @param requestingNodeId ID of the requesting node
     * @param timestamp Lamport timestamp of the request
     */
    void requestCS(long requestingNodeId, int timestamp) throws RemoteException;

    /**
     * Reply to a critical section request
     * @param replyingNodeId ID of the node sending reply
     * @param timestamp Lamport timestamp of the reply
     */
    void replyCS(long replyingNodeId, int timestamp) throws RemoteException;

    /**
     * Release critical section (broadcast to all nodes)
     * @param releasingNodeId ID of the node releasing CS
     * @param timestamp Lamport timestamp of the release
     */
    void releaseCS(long releasingNodeId, int timestamp) throws RemoteException;

    // === Shared Variable operations (executed in Critical Section) ===
    /**
     * Get the current value of the shared variable
     */
    int getSharedVariable() throws RemoteException;

    /**
     * Set the value of the shared variable
     * @param value New value for the shared variable
     */
    void setSharedVariable(int value) throws RemoteException;

    // === Testing and simulation features ===
    /**
     * Set artificial delay for message sending/receiving (in milliseconds)
     */
    void setMessageDelayMs(int delayMs) throws RemoteException;

    /**
     * Get current message delay setting
     */
    int getMessageDelayMs() throws RemoteException;

    // === Status and debugging ===
    boolean isInCriticalSection() throws RemoteException;

    String getQueueStatus() throws RemoteException;

    void ping() throws RemoteException;
}