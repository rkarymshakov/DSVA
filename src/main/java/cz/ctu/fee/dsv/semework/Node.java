package cz.ctu.fee.dsv.semework;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote interface for a distributed node implementing Lamport's mutual exclusion algorithm.
 * Provides methods for joining/leaving a network, managing critical section access,
 * shared variable synchronization, message delays, and failure detection.
 */
public interface Node extends Remote {

    /** Returns the unique identifier of this node. */
    long getNodeId() throws RemoteException;

    /** Returns the current logical clock of this node. */
    int getLogicalClock() throws RemoteException;

    /**
     * Allows a new node to join the network and synchronizes shared variables and request queue.
     * @param joiningNodeId The ID of the node that is joining.
     * @param joiningNodeRef The reference to the joining node's remote object.
     * @return A map of all known nodes including this node and their references.
     */
    Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException;

    /**
     * Adds a known node to this node's topology.
     * @param nodeId The ID of the node to add.
     * @param nodeRef The reference to the node's remote object.
     */
    void addNode(long nodeId, Node nodeRef) throws RemoteException;

    /**
     * Removes a node from this node's topology.
     * @param nodeId The ID of the node to remove.
     */
    void removeNode(long nodeId) throws RemoteException;

    /** Leaves the network, notifying all known nodes and cleaning up state. */
    void leave() throws RemoteException;

    /** Returns a list of IDs for all currently known nodes. */
    List<Long> getKnownNodes() throws RemoteException;

    /** Requests entry into the critical section (Lamport mutual exclusion). */
    void enterCS() throws RemoteException;

    /**
     * Handles a request from another node to enter the critical section.
     * @param requestingNodeId The ID of the node requesting access.
     * @param timestamp The logical clock timestamp of the request.
     */
    void requestCS(long requestingNodeId, int timestamp) throws RemoteException;

    /**
     * Handles a reply from another node granting permission to enter the critical section.
     * @param replyingNodeId The ID of the node sending the reply.
     * @param timestamp The logical clock timestamp of the reply.
     */
    void replyCS(long replyingNodeId, int timestamp) throws RemoteException;

    /**
     * Handles notification from another node that it has released the critical section.
     * @param releasingNodeId The ID of the node releasing the critical section.
     * @param timestamp The logical clock timestamp of the release.
     */
    void releaseCS(long releasingNodeId, int timestamp) throws RemoteException;

    /** Leaves the critical section after execution is finished. */
    void leaveCS() throws RemoteException;

    /** Returns the current value of the shared variable. */
    int getSharedVariable() throws RemoteException;

    /**
     * Updates the shared variable; must be in the critical section to write.
     * @param value The new value to set the shared variable to.
     */
    void setSharedVariable(int value) throws RemoteException;

    /**
     * Updates the shared variable based on another node's value and timestamp.
     * @param value The value to update to.
     * @param timestamp The logical clock timestamp of the update.
     * @param sourceNodeId The ID of the node sending the update.
     */
    void updateSharedVariable(int value, int timestamp, long sourceNodeId) throws RemoteException;

    /**
     * Sets a message delay (simulated network latency) in milliseconds.
     * @param delayMs The delay in milliseconds to simulate for message passing.
     */
    void setMessageDelayMs(int delayMs) throws RemoteException;

    /** Returns the currently configured message delay. */
    int getMessageDelayMs() throws RemoteException;

    /** Simulates a node crash; the node will stop responding to requests. */
    void kill() throws RemoteException;

    /** Revives a previously killed node and reconnects it to the network. */
    void revive() throws RemoteException;

    /**
     * Synchronizes this node's request queue with a given list of requests from another node.
     * @param queueState The list of requests to sync.
     */
    void syncQueue(List<Request> queueState) throws RemoteException;

    /** Returns true if the node is currently in the critical section. */
    boolean isInCriticalSection() throws RemoteException;

    /** Returns a string representation of the current request queue. */
    String getQueueStatus() throws RemoteException;

    /** Checks if the node is alive; throws RemoteException if it is killed. */
    void ensureAlive() throws RemoteException;

    /** Detects dead/unresponsive nodes in the network and handles their removal. */
    void detectDeadNodes() throws RemoteException;

    /**
     * Notifies this node that a specific node is dead.
     * @param deadNodeId The ID of the node that is dead/unresponsive.
     */
    void notifyNodeDead(long deadNodeId) throws RemoteException;
}
