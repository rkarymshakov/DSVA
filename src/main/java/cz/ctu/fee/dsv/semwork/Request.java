package cz.ctu.fee.dsv.semwork;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a timestamped request for the critical section.
 * Implements Comparable to allow automatic sorting in the PriorityQueue.
 */
public class Request implements Serializable, Comparable<Request> {

    @Serial
    private static final long serialVersionUID = 1L;

    public final long nodeId;
    public final int timestamp;

    public Request(long nodeId, int timestamp) {
        this.nodeId = nodeId;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Request other) {
        if (this.timestamp != other.timestamp) // Primary sort: Timestamp (Logical Clock)
            return Integer.compare(this.timestamp, other.timestamp); // Earlier timestamp has higher priority
        return Long.compare(this.nodeId, other.nodeId); // Secondary sort: Lower node ID has higher priority
    }

    @Override
    public String toString() {
        return String.format("{N:%d, T:%d}", nodeId, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return nodeId == request.nodeId && timestamp == request.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, timestamp);
    }
}