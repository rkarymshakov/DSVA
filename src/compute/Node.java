package compute;

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
     * Get unique identifier of this node (e.g., "nodeA@192.168.1.10:1099")
     */
    String getNodeId() throws RemoteException;

    /**
     * Get the current logical clock value (Lamport timestamp)
     */
    int getLogicalClock() throws RemoteException;

    // === Topology management ===
    /**
     * Register another node in this node's topology
     * @param nodeId Unique identifier of the node to add
     * @param nodeRef Remote reference to the node
     */
    void addNode(String nodeId, Node nodeRef) throws RemoteException;

    /**
     * Remove a node from topology
     * @param nodeId Identifier of node to remove
     */
    void removeNode(String nodeId) throws RemoteException;

    /**
     * Get list of all known nodes in the system
     */
    List<String> getKnownNodes() throws RemoteException;

    // === Lamport's algorithm - Critical Section messages ===
    /**
     * Request access to critical section
     * @param requestingNodeId ID of the requesting node
     * @param timestamp Lamport timestamp of the request
     */
    void requestCS(String requestingNodeId, int timestamp) throws RemoteException;

    /**
     * Reply to a critical section request
     * @param replyingNodeId ID of the node sending reply
     * @param timestamp Lamport timestamp of the reply
     */
    void replyCS(String replyingNodeId, int timestamp) throws RemoteException;

    /**
     * Release critical section (broadcast to all nodes)
     * @param releasingNodeId ID of the node releasing CS
     * @param timestamp Lamport timestamp of the release
     */
    void releaseCS(String releasingNodeId, int timestamp) throws RemoteException;

    // === Shared Variable operations (executed in Critical Section) ===
    /**
     * Get the current value of the shared variable
     * This should only be called when node is in critical section
     */
    int getSharedVariable() throws RemoteException;

    /**
     * Set the value of the shared variable
     * This should only be called when node is in critical section
     * @param value New value for the shared variable
     */
    void setSharedVariable(int value) throws RemoteException;

    // === Testing and simulation features ===
    /**
     * Set artificial delay for message sending (in milliseconds)
     * Used to simulate slow network and test concurrent situations
     * @param delayMs Delay in milliseconds (0 = no delay)
     */
    void setMessageDelayMs(int delayMs) throws RemoteException;

    /**
     * Get current message delay setting
     */
    int getMessageDelayMs() throws RemoteException;

    // === Status and debugging ===
    /**
     * Check if this node is currently in critical section
     */
    boolean isInCriticalSection() throws RemoteException;

    /**
     * Get information about pending requests in the queue
     */
    String getQueueStatus() throws RemoteException;

    /**
     * Ping - simple health check
     */
    void ping() throws RemoteException;
}