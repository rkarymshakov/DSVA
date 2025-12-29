package cz.ctu.fee.dsv.semework;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a timestamped request for the critical section.
 * Implements Comparable to allow automatic sorting in the PriorityQueue.
 */
public class Request implements Serializable, Comparable<Request> {

    private static final long serialVersionUID = 1L;

    public final long nodeId;
    public final int timestamp;

    public Request(long nodeId, int timestamp) {
        this.nodeId = nodeId;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Request other) {
        // Primary sort: Timestamp (Logical Clock)
        if (this.timestamp != other.timestamp) {
            return Integer.compare(this.timestamp, other.timestamp);
        }
        // Secondary sort: Node ID (Tie-breaker for concurrent requests)
        return Long.compare(this.nodeId, other.nodeId);
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