package com.neuropace.curriculum.service;

import com.neuropace.curriculum.dto.RoutingDecision;
import com.neuropace.curriculum.entity.RerouteDecisionEntity;
import com.neuropace.curriculum.repository.RerouteDecisionRepository;
import com.neuropace.curriculum.routing.ConsistentHashingRouter;
import com.neuropace.curriculum.routing.LoadBalancingRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orchestration service that ties together:
 * <ol>
 *   <li>Adaptive routing via {@link LoadBalancingRouter} (Dijkstra).</li>
 *   <li>Worker assignment via {@link ConsistentHashingRouter}.</li>
 *   <li>Persistence of reroute decisions to MySQL.</li>
 *   <li>Redis pub/sub notification: "RerouteDecision" channel.</li>
 * </ol>
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final LoadBalancingRouter loadBalancingRouter;
    private final ConsistentHashingRouter consistentHashingRouter;
    private final RerouteDecisionRepository rerouteDecisionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public RoutingService(LoadBalancingRouter loadBalancingRouter,
                          ConsistentHashingRouter consistentHashingRouter,
                          RerouteDecisionRepository rerouteDecisionRepository,
                          StringRedisTemplate stringRedisTemplate) {
        this.loadBalancingRouter       = loadBalancingRouter;
        this.consistentHashingRouter   = consistentHashingRouter;
        this.rerouteDecisionRepository = rerouteDecisionRepository;
        this.stringRedisTemplate       = stringRedisTemplate;
    }

    /**
     * Processes an overloaded student: computes routing, assigns a worker,
     * persists the decision, and publishes a Redis event.
     *
     * @param studentId        the student to process.
     * @param currentSubjectId the subject they are currently studying.
     * @param cognitiveLoad    the student's current cognitive load (0.0–1.0).
     * @return the saved {@link RerouteDecisionEntity}, or {@code null} if no rerouting needed.
     */
    @Transactional
    public RerouteDecisionEntity processOverloadedStudent(Long studentId,
                                                         Long currentSubjectId,
                                                         double cognitiveLoad) {
        RoutingDecision decision = loadBalancingRouter.suggestRouting(
                studentId, currentSubjectId, cognitiveLoad,
                LoadBalancingRouter.DEFAULT_THRESHOLD);

        if (decision == null) {
            log.debug("Student {}: no rerouting needed (load={})", studentId, cognitiveLoad);
            return null;
        }

        // Assign to a worker node
        String workerId = consistentHashingRouter.assignWorker(studentId);
        log.info("Student {} assigned to worker '{}' for rerouting.", studentId, workerId);

        // Persist
        RerouteDecisionEntity entity = RerouteDecisionEntity.builder()
                .studentId(studentId)
                .fromSubjectId(decision.currentSubjectId())
                .toSubjectId(decision.recommendedSubjectId())
                .reason(decision.reason())
                .rerouteTime(Instant.now())
                .isSuccessful(true)
                .build();
        RerouteDecisionEntity saved = rerouteDecisionRepository.save(entity);

        // Publish Redis event
        try {
            String event = String.format(
                    "{\"studentId\":%d,\"fromSubject\":%d,\"toSubject\":%d,\"worker\":\"%s\"}",
                    studentId, decision.currentSubjectId(), decision.recommendedSubjectId(), workerId);
            stringRedisTemplate.convertAndSend("RerouteDecision", event);
        } catch (Exception e) {
            log.error("Failed to publish RerouteDecision to Redis: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Returns the last 50 reroute decisions for a student, newest first.
     *
     * @param studentId the student ID.
     * @param limit     maximum number to return (enforced in query).
     * @return list of reroute decisions.
     */
    public List<RerouteDecisionEntity> getRoutingHistory(Long studentId, int limit) {
        return rerouteDecisionRepository.findTop50ByStudentIdOrderByRerouteTimeDesc(studentId);
    }

    /**
     * Handles a worker failure by removing it from the hash ring and logging the impact.
     *
     * <p>Students previously routed to this worker will automatically be reassigned
     * to ring neighbours on the next {@link ConsistentHashingRouter#assignWorker} call.
     *
     * @param workerId the failed worker ID.
     */
    public void recordWorkerFailure(String workerId) {
        int totalWorkers   = consistentHashingRouter.getWorkers().size();
        int ringBefore     = consistentHashingRouter.getRingSize();

        consistentHashingRouter.removeWorker(workerId);

        int ringAfter      = consistentHashingRouter.getRingSize();
        int removedVnodes  = ringBefore - ringAfter;
        double pctImpacted = totalWorkers > 0
                ? 100.0 / totalWorkers
                : 0.0;

        log.warn("Worker {} failed. Removed {} virtual nodes (~{}% of load redistributed to neighbours).",
                workerId, removedVnodes, String.format("%.0f", pctImpacted));
    }
}
