package com.neuropace.curriculum.benchmark;

import com.neuropace.curriculum.dto.PathDTO;
import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.graph.CurriculumGraphWithWeights;

import java.util.*;

/**
 * Performance benchmark for Dijkstra routing over a synthetic 500-subject curriculum graph.
 *
 * <p>Compares:
 * <ul>
 *   <li>Dijkstra: O((V+E)logV) — respects difficulty weights.</li>
 *   <li>BFS (unweighted): O(V+E) — ignores weights, may suggest harder paths.</li>
 * </ul>
 */
public class RoutingBenchmark {

    public static void main(String[] args) {
        System.out.println("=== Starting Dijkstra Routing Benchmark ===");

        // ── Build 500-subject graph with 1000 prerequisite edges ─────────────
        int numSubjects    = 500;
        int numPrereqs     = 1000;
        Random rng         = new Random(42);

        List<Subject> subjects = new ArrayList<>();
        for (int i = 1; i <= numSubjects; i++) {
            subjects.add(Subject.builder()
                    .id((long) i)
                    .name("Subject-" + i)
                    .difficultyLevel(1 + (i % 10))
                    .build());
        }

        // Create a spanning tree first (guarantees no cycles)
        List<Prerequisite> prerequisites = new ArrayList<>();
        long pid = 1;
        for (int i = 2; i <= numSubjects; i++) {
            int fromIdx = rng.nextInt(i - 1) + 1;  // random earlier node
            Subject from = subjects.get(fromIdx - 1);
            Subject to   = subjects.get(i - 1);
            prerequisites.add(Prerequisite.builder()
                    .id(pid++)
                    .requiresSubject(from)
                    .subject(to)
                    .weight(1.0)
                    .build());
        }
        // Add remaining edges on top (keeping DAG property by only going forward)
        int extra = numPrereqs - (numSubjects - 1);
        for (int i = 0; i < extra; i++) {
            int fromIdx = rng.nextInt(numSubjects - 1) + 1;
            int toIdx   = fromIdx + 1 + rng.nextInt(numSubjects - fromIdx);
            if (toIdx > numSubjects) continue;
            Subject from = subjects.get(fromIdx - 1);
            Subject to   = subjects.get(toIdx - 1);
            prerequisites.add(Prerequisite.builder()
                    .id(pid++)
                    .requiresSubject(from)
                    .subject(to)
                    .weight(1.0)
                    .build());
        }

        System.out.printf("Graph built: %d subjects, %d prerequisite edges%n",
                numSubjects, prerequisites.size());

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(subjects, prerequisites);

        // Collect subjects by difficulty tier
        Set<Long> easierTargets = new HashSet<>();
        for (Subject s : subjects) {
            if (s.getDifficultyLevel() <= 3) easierTargets.add(s.getId());
        }

        // ── JVM warm-up ───────────────────────────────────────────────────────
        for (int i = 0; i < 1000; i++) {
            long srcId = (rng.nextInt(numSubjects) + 1);
            graph.findLightestPath(srcId, easierTargets);
        }
        System.gc();

        // ── Dijkstra benchmark: 10,000 routing decisions ─────────────────────
        long dijkstraStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            long srcId = (rng.nextInt(numSubjects) + 1);
            PathDTO path = graph.findLightestPath(srcId, easierTargets);
            // Consume result to prevent dead-code elimination
            if (path == null) throw new RuntimeException("null path");
        }
        long dijkstraEnd = System.nanoTime();

        // ── BFS comparison: unweighted shortest path ──────────────────────────
        Map<Long, List<Long>> adj = buildAdjacency(prerequisites);
        long bfsStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            long srcId = (rng.nextInt(numSubjects) + 1);
            bfsShortestPath(srcId, easierTargets, adj);
        }
        long bfsEnd = System.nanoTime();

        // ── Results ───────────────────────────────────────────────────────────
        double dijkstraTotalMs = (dijkstraEnd - dijkstraStart) / 1_000_000.0;
        double dijkstraAvgMs   = dijkstraTotalMs / 10_000.0;
        double bfsTotalMs      = (bfsEnd - bfsStart) / 1_000_000.0;
        double bfsAvgMs        = bfsTotalMs / 10_000.0;

        System.out.printf("Dijkstra routing:  10k decisions in %.1fms (avg %.2fms per decision)%n",
                dijkstraTotalMs, dijkstraAvgMs);
        System.out.printf("BFS routing:       10k decisions in %.1fms (avg %.2fms per decision)%n",
                bfsTotalMs, bfsAvgMs);
        System.out.printf("Dijkstra/BFS ratio: %.1fx%n", dijkstraTotalMs / Math.max(1, bfsTotalMs));
        System.out.println("=== Benchmark Completed ===");
    }

    private static Map<Long, List<Long>> buildAdjacency(List<Prerequisite> prerequisites) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Prerequisite p : prerequisites) {
            Long from = p.getRequiresSubject().getId();
            Long to   = p.getSubject().getId();
            adj.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            adj.computeIfAbsent(to,   k -> new ArrayList<>()).add(from);
        }
        return adj;
    }

    private static List<Long> bfsShortestPath(Long src, Set<Long> targets, Map<Long, List<Long>> adj) {
        if (targets.contains(src)) return List.of(src);
        Queue<Long> queue     = new LinkedList<>();
        Map<Long, Long> pred  = new HashMap<>();
        queue.offer(src);
        pred.put(src, -1L);

        while (!queue.isEmpty()) {
            Long u = queue.poll();
            if (targets.contains(u)) {
                LinkedList<Long> path = new LinkedList<>();
                Long curr = u;
                while (curr != null && curr != -1L) {
                    path.addFirst(curr);
                    curr = pred.get(curr);
                    if (curr != null && curr == -1L) break;
                }
                return path;
            }
            for (Long v : adj.getOrDefault(u, Collections.emptyList())) {
                if (!pred.containsKey(v)) {
                    pred.put(v, u);
                    queue.offer(v);
                }
            }
        }
        return Collections.emptyList();
    }
}
