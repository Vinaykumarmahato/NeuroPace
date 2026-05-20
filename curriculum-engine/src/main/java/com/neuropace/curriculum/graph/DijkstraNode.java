package com.neuropace.curriculum.graph;

import java.util.Objects;

/**
 * Priority-queue node used during Dijkstra's algorithm traversal.
 *
 * <p>Natural ordering is by ascending {@code distance} so a min-heap
 * ({@link java.util.PriorityQueue}) always dequeues the nearest unvisited node.
 *
 * <p>Time complexity note: Each node is enqueued at most once per relaxation,
 * giving the overall O((V+E) log V) bound when combined with the PriorityQueue.
 */
public final class DijkstraNode implements Comparable<DijkstraNode> {

    /** Subject ID this node represents. */
    public final long subjectId;

    /** Shortest known distance from the source to this node. */
    public final double distance;

    /** Predecessor subject ID on the shortest path (-1 if none). */
    public final long predecessor;

    /**
     * Constructs a Dijkstra node.
     *
     * @param subjectId   the subject this node represents.
     * @param distance    accumulated cost from source.
     * @param predecessor the previous node in the shortest path (-1 for source).
     */
    public DijkstraNode(long subjectId, double distance, long predecessor) {
        this.subjectId   = subjectId;
        this.distance    = distance;
        this.predecessor = predecessor;
    }

    /** Min-heap ordering: smaller distance → higher priority. */
    @Override
    public int compareTo(DijkstraNode other) {
        return Double.compare(this.distance, other.distance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DijkstraNode that)) return false;
        return subjectId == that.subjectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId);
    }

    @Override
    public String toString() {
        return "DijkstraNode{id=" + subjectId + ", dist=" + distance + ", pred=" + predecessor + "}";
    }
}
