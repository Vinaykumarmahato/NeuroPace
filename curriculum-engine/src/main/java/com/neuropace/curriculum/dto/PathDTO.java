package com.neuropace.curriculum.dto;

import com.neuropace.curriculum.entity.Subject;

import java.util.List;

/**
 * Immutable DTO representing the result of a Dijkstra shortest-path computation
 * across the weighted curriculum graph.
 *
 * @param path       ordered list of subjects from source to destination.
 * @param totalCost  sum of edge weights along the path.
 * @param reasoning  human-readable explanation of the routing decision.
 */
public record PathDTO(List<Subject> path, double totalCost, String reasoning) {

    /** Sentinel: student is already at the optimal subject. */
    public static PathDTO alreadyOptimal(Subject current) {
        return new PathDTO(
                List.of(current),
                0.0,
                "Already on optimal path: " + current.getName()
        );
    }

    /** Sentinel: no path could be found. */
    public static PathDTO noPath(Long fromId, Long toId) {
        return new PathDTO(
                List.of(),
                Double.MAX_VALUE,
                "No path found from subject " + fromId + " to " + toId
        );
    }
}
