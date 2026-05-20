package com.neuropace.curriculum.benchmark;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.graph.CurriculumGraph;

import java.util.*;

/**
 * Benchmark utility to test the topological sorting speed and complexity of CurriculumGraph.
 */
public class CurriculumGraphBenchmark {

    public static void main(String[] args) {
        int V = 500;
        int E = 1000;

        List<Subject> subjects = new ArrayList<>();
        for (long i = 1; i <= V; i++) {
            subjects.add(Subject.builder()
                    .id(i)
                    .name("Subject " + i)
                    .difficultyLevel(5)
                    .topicArea("General")
                    .build());
        }

        Set<String> edgeSet = new HashSet<>();
        List<Prerequisite> prerequisites = new ArrayList<>();
        Random random = new Random(42);

        long idCounter = 1;
        while (prerequisites.size() < E) {
            int uIndex = random.nextInt(V - 1) + 1; // 1 to V-1
            int vIndex = random.nextInt(V - uIndex) + uIndex + 1; // uIndex+1 to V
            
            String edgeKey = uIndex + "->" + vIndex;
            if (edgeSet.add(edgeKey)) {
                Subject u = subjects.get(uIndex - 1);
                Subject v = subjects.get(vIndex - 1);
                prerequisites.add(Prerequisite.builder()
                        .id(idCounter++)
                        .subject(v)
                        .requiresSubject(u)
                        .weight(1.0)
                        .build());
            }
        }

        // Warm up JVM
        for (int i = 0; i < 200; i++) {
            new CurriculumGraph(subjects, prerequisites);
        }

        // Measure execution time
        long start = System.nanoTime();
        CurriculumGraph graph = new CurriculumGraph(subjects, prerequisites);
        long end = System.nanoTime();

        double durationMs = (end - start) / 1_000_000.0;
        boolean withinLimit = durationMs < 10.0;

        System.out.printf("Topological sort: %d subjects, %d edges in %.2fms (O(V+E)=%b)%n",
                V, E, durationMs, withinLimit);
    }
}
