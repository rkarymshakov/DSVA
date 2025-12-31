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

    long getNodeId() throws RemoteException;
    int getLogicalClock() throws RemoteException;

    Map<Long, Node> join(long joiningNodeId, Node joiningNodeRef) throws RemoteException;
    void addNode(long nodeId, Node nodeRef) throws RemoteException;
    void removeNode(long nodeId) throws RemoteException;
    void leave() throws RemoteException;
    List<Long> getKnownNodes() throws RemoteException;

    void enterCS() throws RemoteException;
    void requestCS(long requestingNodeId, int timestamp) throws RemoteException;
    void replyCS(long replyingNodeId, int timestamp) throws RemoteException;
    void releaseCS(long releasingNodeId, int timestamp) throws RemoteException;
    void leaveCS() throws RemoteException;

    int getSharedVariable() throws RemoteException;
    void setSharedVariable(int value) throws RemoteException;
    void updateSharedVariable(int value, int timestamp, long sourceNodeId) throws RemoteException;

    void setMessageDelayMs(int delayMs) throws RemoteException;
    int getMessageDelayMs() throws RemoteException;

    void kill() throws RemoteException;
    void revive() throws RemoteException;

    void syncQueue(List<Request> queueState) throws RemoteException;

    boolean isInCriticalSection() throws RemoteException;
    String getQueueStatus() throws RemoteException;
    void ensureAlive() throws RemoteException;

    void detectDeadNodes() throws RemoteException;
    void notifyNodeDead(long deadNodeId) throws RemoteException;
}