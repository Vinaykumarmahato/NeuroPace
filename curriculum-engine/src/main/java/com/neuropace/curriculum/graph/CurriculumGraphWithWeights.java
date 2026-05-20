package com.neuropace.curriculum.graph;

import com.neuropace.curriculum.dto.PathDTO;
import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;

import java.util.*;

/**
 * Extends {@link CurriculumGraph} with weighted edges for Dijkstra-based routing.
 *
 * <p>Edge weight = {@code target.difficultyLevel}, so Dijkstra naturally finds
 * the lowest-difficulty path through the curriculum for an overloaded student.
 *
 * <p><b>Dijkstra pseudocode:</b>
 * <pre>
 *   dist[source] = 0;  dist[all others] = ∞
 *   PQ = { (0, source) }
 *   while PQ not empty:
 *     (d, u) = PQ.poll()              // O(log V) — min-heap
 *     if u already settled: continue
 *     mark u settled
 *     for each neighbour v of u:
 *       newDist = d + weight(u, v)
 *       if newDist &lt; dist[v]:
 *         dist[v] = newDist
 *         pred[v] = u
 *         PQ.offer((newDist, v))      // O(log V)
 * </pre>
 *
 * <p><b>Time Complexity:</b> O((V + E) log V) using a binary-heap PriorityQueue.
 * For 500 subjects and 1000 edges: ~10,000 operations.
 *
 * <p><b>Why Dijkstra over BFS?</b> BFS treats all edges as equal weight, so it
 * might suggest a path through a harder subject if fewer hops exist. Dijkstra
 * guarantees the minimum-total-difficulty path.
 */
public class CurriculumGraphWithWeights extends CurriculumGraph {

    /**
     * Bidirectional weighted adjacency: nodeId → { neighbourId → edgeWeight }.
     * Weight = difficulty of the neighbour subject being transitioned to.
     */
    private final Map<Long, Map<Long, Integer>> weightedEdges;

    /**
     * Constructs the weighted curriculum graph from the same subjects and prerequisites
     * used by the parent topological-sort graph.
     *
     * @param subjects      all curriculum subjects.
     * @param prerequisites prerequisite edges (direction: requiresSubject → subject).
     */
    public CurriculumGraphWithWeights(List<Subject> subjects, List<Prerequisite> prerequisites) {
        super(subjects, prerequisites);

        // Initialise adjacency buckets for every known subject
        Map<Long, Map<Long, Integer>> edges = new HashMap<>();
        for (Long id : subjectsMap.keySet()) {
            edges.put(id, new HashMap<>());
        }

        for (Prerequisite p : prerequisites) {
            Long from   = p.getRequiresSubject().getId();   // easier prerequisite
            Long to     = p.getSubject().getId();            // harder dependent
            Subject toS  = subjectsMap.get(to);
            Subject frS  = subjectsMap.get(from);

            if (toS != null && frS != null) {
                int toWeight  = toS.getDifficultyLevel()  != null ? toS.getDifficultyLevel()  : 1;
                int frWeight  = frS.getDifficultyLevel()  != null ? frS.getDifficultyLevel()  : 1;

                // Forward edge (easier → harder): weight = harder subject's difficulty
                edges.computeIfAbsent(from, k -> new HashMap<>()).merge(to, toWeight, Math::min);
                // Reverse edge (harder → easier): weight = easier subject's difficulty
                // This lets Dijkstra route DOWN the curriculum graph
                edges.computeIfAbsent(to, k -> new HashMap<>()).merge(from, frWeight, Math::min);
            }
        }

        this.weightedEdges = Collections.unmodifiableMap(edges);
    }

    /**
     * Finds the lightest (lowest total difficulty cost) path from {@code sourceId}
     * to any subject in {@code targetIds} using Dijkstra's algorithm.
     *
     * <p>Time Complexity: O((V + E) log V).
     *
     * @param sourceId  subject the student is currently on.
     * @param targetIds set of acceptable destination subject IDs (e.g. all easier subjects).
     * @return PathDTO with ordered path, total cost, and routing reasoning.
     */
    public PathDTO findLightestPath(Long sourceId, Set<Long> targetIds) {
        // Already at a target?
        if (targetIds.contains(sourceId)) {
            Subject src = subjectsMap.get(sourceId);
            return (src != null) ? PathDTO.alreadyOptimal(src)
                                 : PathDTO.noPath(sourceId, sourceId);
        }

        if (!subjectsMap.containsKey(sourceId)) {
            return PathDTO.noPath(sourceId, -1L);
        }

        // ── Dijkstra initialisation ───────────────────────────────────────
        Map<Long, Double> dist    = new HashMap<>();
        Map<Long, Long>   pred    = new HashMap<>();
        Set<Long>         settled = new HashSet<>();
        PriorityQueue<DijkstraNode> pq = new PriorityQueue<>();

        for (Long id : subjectsMap.keySet()) dist.put(id, Double.MAX_VALUE);
        dist.put(sourceId, 0.0);
        pq.offer(new DijkstraNode(sourceId, 0.0, -1L));

        // ── Main loop ─────────────────────────────────────────────────────
        while (!pq.isEmpty()) {
            DijkstraNode node = pq.poll();
            long u = node.subjectId;

            if (settled.contains(u)) continue;      // stale entry — skip
            settled.add(u);

            if (targetIds.contains(u)) {
                return reconstructPath(sourceId, u, dist, pred);
            }

            for (Map.Entry<Long, Integer> e : weightedEdges.getOrDefault(u, Collections.emptyMap()).entrySet()) {
                long v        = e.getKey();
                double newD   = dist.get(u) + e.getValue();
                if (newD < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, newD);
                    pred.put(v, u);
                    pq.offer(new DijkstraNode(v, newD, u));
                }
            }
        }

        return PathDTO.noPath(sourceId, -1L);
    }

    // ── Package-visible accessors (for tests) ─────────────────────────────

    Map<Long, Map<Long, Integer>> getWeightedEdges() {
        return weightedEdges;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private PathDTO reconstructPath(Long sourceId, Long targetId,
                                    Map<Long, Double> dist,
                                    Map<Long, Long> pred) {
        LinkedList<Long> ids = new LinkedList<>();
        Long curr = targetId;
        // Walk predecessors back to source
        while (curr != null && !curr.equals(sourceId)) {
            ids.addFirst(curr);
            curr = pred.get(curr);
        }
        ids.addFirst(sourceId);

        List<Subject> path = new ArrayList<>();
        for (Long id : ids) {
            Subject s = subjectsMap.get(id);
            if (s != null) path.add(s);
        }

        double cost      = dist.getOrDefault(targetId, 0.0);
        Subject source   = subjectsMap.get(sourceId);
        Subject target   = subjectsMap.get(targetId);

        String pathStr = path.stream()
                .map(Subject::getName)
                .reduce((a, b) -> a + " → " + b)
                .orElse("?");

        String reasoning = (source != null && target != null)
                ? String.format("Routed from %s (diff=%d) to %s (diff=%d). Path: %s",
                        source.getName(), source.getDifficultyLevel(),
                        target.getName(), target.getDifficultyLevel(), pathStr)
                : "Path: " + pathStr + " | Total cost: " + cost;

        return new PathDTO(path, cost, reasoning);
    }
}
