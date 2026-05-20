package com.neuropace.curriculum.routing;

import com.neuropace.curriculum.dto.PathDTO;
import com.neuropace.curriculum.dto.RoutingDecision;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.graph.CurriculumGraphWithWeights;
import com.neuropace.curriculum.service.CurriculumGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adaptive routing engine that reroutes overloaded students to easier subjects
 * using Dijkstra's algorithm over the weighted curriculum graph.
 *
 * <p>Routing decision flow:
 * <ol>
 *   <li>Fetch cognitive load for student (0.0–1.0 scale).</li>
 *   <li>If load &gt; threshold: find all subjects with lower difficulty.</li>
 *   <li>Call Dijkstra to find the lightest-cost path to an easier subject.</li>
 *   <li>Return {@link RoutingDecision} with full reasoning.</li>
 * </ol>
 */
@Component
public class LoadBalancingRouter {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancingRouter.class);

    public static final double DEFAULT_THRESHOLD = 0.7;

    private final CurriculumGraphService curriculumGraphService;

    public LoadBalancingRouter(CurriculumGraphService curriculumGraphService) {
        this.curriculumGraphService = curriculumGraphService;
    }

    /**
     * Suggests an adaptive rerouting for a student if their cognitive load exceeds the threshold.
     *
     * @param studentId         the student to evaluate.
     * @param currentSubjectId  the subject the student is currently studying.
     * @param cognitiveLoad     the student's current cognitive load (0.0–1.0).
     * @param overloadThreshold above this value, rerouting is triggered.
     * @return a {@link RoutingDecision}, or {@code null} if no rerouting is needed.
     */
    public RoutingDecision suggestRouting(Long studentId, Long currentSubjectId,
                                         double cognitiveLoad, double overloadThreshold) {
        if (cognitiveLoad <= overloadThreshold) {
            log.debug("Student {}: load={} ≤ threshold={}, no rerouting needed.",
                    studentId, cognitiveLoad, overloadThreshold);
            return null;
        }

        CurriculumGraphWithWeights graph = curriculumGraphService.getWeightedGraph();
        List<Subject> allSubjects = graph.getTopologicalOrder();

        // Find current subject
        Subject current = allSubjects.stream()
                .filter(s -> s.getId().equals(currentSubjectId))
                .findFirst()
                .orElse(null);

        if (current == null) {
            log.warn("Student {}: currentSubjectId={} not found in curriculum.", studentId, currentSubjectId);
            return null;
        }

        int currentDifficulty = current.getDifficultyLevel() != null ? current.getDifficultyLevel() : 5;

        // Collect all easier subjects as routing targets
        Set<Long> easierSubjects = allSubjects.stream()
                .filter(s -> s.getDifficultyLevel() != null && s.getDifficultyLevel() < currentDifficulty)
                .map(Subject::getId)
                .collect(Collectors.toSet());

        if (easierSubjects.isEmpty()) {
            log.info("Student {}: already on easiest subject '{}' (diff={}). No rerouting available.",
                    studentId, current.getName(), currentDifficulty);
            return null;
        }

        // Dijkstra: find lightest path to any easier subject
        PathDTO path = graph.findLightestPath(currentSubjectId, easierSubjects);

        if (path.path().isEmpty() || path.totalCost() == Double.MAX_VALUE) {
            log.warn("Student {}: no reachable easier path from '{}'.", studentId, current.getName());
            return null;
        }

        Subject recommended = path.path().get(path.path().size() - 1);
        int recommendedDifficulty = recommended.getDifficultyLevel() != null ? recommended.getDifficultyLevel() : 0;
        int difficultyChange = recommendedDifficulty - currentDifficulty;

        String reason = String.format(
                "Overloaded on %s (diff=%d). Routed to %s (diff=%d). Path: %s",
                current.getName(), currentDifficulty,
                recommended.getName(), recommendedDifficulty,
                path.path().stream().map(Subject::getName).collect(Collectors.joining(" → ")));

        log.info("Student {} rerouted: {} → {} (load={} > threshold={})",
                studentId, current.getName(), recommended.getName(), cognitiveLoad, overloadThreshold);

        return new RoutingDecision(currentSubjectId, recommended.getId(), reason, difficultyChange);
    }
}
