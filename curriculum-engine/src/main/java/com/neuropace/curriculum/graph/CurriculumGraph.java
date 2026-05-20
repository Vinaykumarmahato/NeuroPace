package com.neuropace.curriculum.graph;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.exception.CircularCurriculumException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable, thread-safe representation of the Curriculum Dependency Graph.
 * Validates acyclicity using Kahn's algorithm and extracts cycle info using DFS.
 */
public class CurriculumGraph {

    public static final int MAX_SUBJECTS = 10000;
    public static final long CYCLE_CHECK_TIMEOUT_MS = 5000;

    protected final Map<Long, Subject> subjectsMap;
    protected final Map<Long, List<Long>> adjacencyList;
    protected final Map<Long, List<Long>> inverseAdjacencyList;
    protected final List<Subject> topologicalOrder;
    protected final boolean validDAG;

    /**
     * Constructs the curriculum graph and checks for cycles.
     *
     * @param subjects      the list of subjects.
     * @param prerequisites the list of prerequisites.
     * @throws CircularCurriculumException if a cycle is detected.
     */
    public CurriculumGraph(List<Subject> subjects, List<Prerequisite> prerequisites) {
        if (subjects == null) {
            subjects = Collections.emptyList();
        }
        if (prerequisites == null) {
            prerequisites = Collections.emptyList();
        }

        Map<Long, Subject> subMap = new HashMap<>();
        for (Subject s : subjects) {
            subMap.put(s.getId(), s);
        }
        this.subjectsMap = Collections.unmodifiableMap(subMap);

        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, List<Long>> invAdj = new HashMap<>();
        for (Subject s : subjects) {
            adj.put(s.getId(), new ArrayList<>());
            invAdj.put(s.getId(), new ArrayList<>());
        }

        for (Prerequisite p : prerequisites) {
            Long subId = p.getSubject().getId();
            Long reqId = p.getRequiresSubject().getId();
            if (adj.containsKey(reqId)) {
                adj.get(reqId).add(subId);
            }
            if (invAdj.containsKey(subId)) {
                invAdj.get(subId).add(reqId);
            }
        }

        // Kahn's algorithm for topological sort
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Subject s : subjects) {
            inDegree.put(s.getId(), 0);
        }
        for (Prerequisite p : prerequisites) {
            Long subId = p.getSubject().getId();
            inDegree.put(subId, inDegree.getOrDefault(subId, 0) + 1);
        }

        Queue<Long> queue = new LinkedList<>();
        for (Subject s : subjects) {
            if (inDegree.get(s.getId()) == 0) {
                queue.add(s.getId());
            }
        }

        List<Subject> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long u = queue.poll();
            Subject s = subjectsMap.get(u);
            if (s != null) {
                order.add(s);
            }
            for (Long v : adj.getOrDefault(u, Collections.emptyList())) {
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.add(v);
                }
            }
        }

        if (order.size() < subjects.size()) {
            this.validDAG = false;
            this.topologicalOrder = Collections.emptyList();
            this.adjacencyList = Collections.emptyMap();
            this.inverseAdjacencyList = Collections.emptyMap();
            List<Subject> cycle = findCycle(subjects, prerequisites);
            throw new CircularCurriculumException(cycle);
        } else {
            this.validDAG = true;
            this.topologicalOrder = Collections.unmodifiableList(order);

            Map<Long, List<Long>> immutableAdj = new HashMap<>();
            adj.forEach((k, v) -> immutableAdj.put(k, Collections.unmodifiableList(v)));
            this.adjacencyList = Collections.unmodifiableMap(immutableAdj);

            Map<Long, List<Long>> immutableInvAdj = new HashMap<>();
            invAdj.forEach((k, v) -> immutableInvAdj.put(k, Collections.unmodifiableList(v)));
            this.inverseAdjacencyList = Collections.unmodifiableMap(immutableInvAdj);
        }
    }

    /**
     * Gets the subjects in topological order.
     * @return List of subjects in dependency order. Time: O(V+E).
     */
    public List<Subject> getTopologicalOrder() {
        return topologicalOrder;
    }

    /**
     * Gets direct prerequisites for a given subject.
     * @param subjectId the ID of the subject.
     * @return List of direct prerequisite subjects. Time: O(1).
     */
    public List<Subject> getPrerequisites(Long subjectId) {
        List<Long> reqIds = inverseAdjacencyList.getOrDefault(subjectId, Collections.emptyList());
        return reqIds.stream()
                .map(subjectsMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Gets direct dependents that require this subject.
     * @param subjectId the ID of the subject.
     * @return List of dependent subjects. Time: O(1).
     */
    public List<Subject> getDependents(Long subjectId) {
        List<Long> depIds = adjacencyList.getOrDefault(subjectId, Collections.emptyList());
        return depIds.stream()
                .map(subjectsMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the graph is a valid DAG.
     * @return true if acyclic, false otherwise. Time: O(1).
     */
    public boolean isValidDAG() {
        return validDAG;
    }

    private List<Subject> findCycle(List<Subject> subjects, List<Prerequisite> prerequisites) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Subject s : subjects) {
            adj.put(s.getId(), new ArrayList<>());
        }
        for (Prerequisite p : prerequisites) {
            Long subId = p.getSubject().getId();
            Long reqId = p.getRequiresSubject().getId();
            if (adj.containsKey(reqId)) {
                adj.get(reqId).add(subId);
            }
        }

        Map<Long, Integer> state = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        for (Subject s : subjects) {
            state.put(s.getId(), 0);
        }

        for (Subject s : subjects) {
            if (state.get(s.getId()) == 0) {
                List<Long> cycleNodes = dfsCycle(s.getId(), adj, state, parent);
                if (cycleNodes != null) {
                    List<Subject> cycleSubjects = new ArrayList<>();
                    for (Long id : cycleNodes) {
                        cycleSubjects.add(subjectsMap.get(id));
                    }
                    return cycleSubjects;
                }
            }
        }
        return Collections.emptyList();
    }

    private List<Long> dfsCycle(Long u, Map<Long, List<Long>> adj, Map<Long, Integer> state, Map<Long, Long> parent) {
        state.put(u, 1);
        for (Long v : adj.getOrDefault(u, Collections.emptyList())) {
            if (state.get(v) == 1) {
                List<Long> cycle = new ArrayList<>();
                Long curr = u;
                cycle.add(v);
                while (curr != null && !curr.equals(v)) {
                    cycle.add(0, curr);
                    curr = parent.get(curr);
                }
                cycle.add(0, v);
                return cycle;
            } else if (state.get(v) == 0) {
                parent.put(v, u);
                List<Long> cycle = dfsCycle(v, adj, state, parent);
                if (cycle != null) {
                    return cycle;
                }
            }
        }
        state.put(u, 2);
        return null;
    }
}
