package com.neuropace.cognitive.service;

import com.neuropace.cognitive.dto.OverloadDetectionDTO;
import com.neuropace.cognitive.entity.LearningEvent;
import com.neuropace.cognitive.entity.OverloadDetection;
import com.neuropace.cognitive.entity.Student;
import com.neuropace.cognitive.entity.Subject;
import com.neuropace.cognitive.model.SlidingWindow;
import com.neuropace.cognitive.repository.LearningEventRepository;
import com.neuropace.cognitive.repository.OverloadDetectionRepository;
import com.neuropace.cognitive.repository.RerouteDecisionRepository;
import com.neuropace.cognitive.repository.StudentRepository;
import com.neuropace.cognitive.repository.SubjectRepository;
import com.neuropace.cognitive.util.AnomalyDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing student cognitive load analysis and overload anomaly detection.
 */
@Service
public class CognitiveLoadService {

    private static final Logger log = LoggerFactory.getLogger(CognitiveLoadService.class);

    private final LearningEventRepository learningEventRepository;
    private final RerouteDecisionRepository rerouteDecisionRepository;
    private final OverloadDetectionRepository overloadDetectionRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private final Map<Long, CachedWindow> cache = new ConcurrentHashMap<>();

    public CognitiveLoadService(LearningEventRepository learningEventRepository,
                                RerouteDecisionRepository rerouteDecisionRepository,
                                OverloadDetectionRepository overloadDetectionRepository,
                                StudentRepository studentRepository,
                                SubjectRepository subjectRepository,
                                StringRedisTemplate stringRedisTemplate) {
        this.learningEventRepository = learningEventRepository;
        this.rerouteDecisionRepository = rerouteDecisionRepository;
        this.overloadDetectionRepository = overloadDetectionRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Records a new learning event, runs overload detection, and saves results.
     *
     * Time Complexity: O(1) calculation, database writes.
     *
     * @param studentId        the student ID.
     * @param subjectId        the subject ID.
     * @param responseTimeMs   the response time of the new learning event.
     * @param performanceScore the student performance score.
     * @return The saved OverloadDetection record.
     */
    @Transactional
    public OverloadDetection recordLearningEvent(Long studentId, Long subjectId, double responseTimeMs, int performanceScore) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        SlidingWindow window = getOrInitializeWindow(studentId);
        CachedWindow cachedWindow = cache.get(studentId);

        double baselineMean = window.getMean();
        double baselineStddev = window.getStdDev();

        OverloadDetectionDTO detectionResult = AnomalyDetector.detectOverload(window, responseTimeMs);

        if (cachedWindow != null) {
            cachedWindow.latestZScore = detectionResult.zScore();
        }

        LearningEvent event = LearningEvent.builder()
                .student(student)
                .subject(subject)
                .responseTimeMs((long) responseTimeMs)
                .performanceScore(performanceScore)
                .timestamp(Instant.now())
                .build();
        learningEventRepository.save(event);

        window.addEvent(event);

        OverloadDetection detection = OverloadDetection.builder()
                .studentId(studentId)
                .eventTime(Instant.now())
                .zScore(detectionResult.zScore())
                .responseTimeMs(responseTimeMs)
                .baselineMeanMs(baselineMean)
                .baselineStddev(baselineStddev)
                .isOverloaded(detectionResult.isOverloaded())
                .build();
        OverloadDetection savedDetection = overloadDetectionRepository.save(detection);

        if (detectionResult.isOverloaded()) {
            log.info("Student {} overloaded: Z={}, response_time={}ms > baseline {}ms",
                    studentId, String.format("%.1f", detectionResult.zScore()),
                    String.format("%.0f", responseTimeMs), String.format("%.0f", baselineMean));

            try {
                String eventMessage = String.format("{\"studentId\":%d,\"zScore\":%.2f,\"responseTime\":%.1f,\"baselineMean\":%.1f}",
                        studentId, detectionResult.zScore(), responseTimeMs, baselineMean);
                stringRedisTemplate.convertAndSend("StudentOverloaded", eventMessage);
            } catch (Exception e) {
                log.error("Failed to publish StudentOverloaded event to Redis: {}", e.getMessage());
            }
        } else {
            log.info("Recorded learning event for student {}: Z={}, response_time={}ms",
                    studentId, String.format("%.2f", detectionResult.zScore()), String.format("%.0f", responseTimeMs));
        }

        return savedDetection;
    }

    /**
     * Calculates the cognitive load index (from 0.0 to 1.0) of a student.
     *
     * @param studentId the student ID.
     * @return Cognitive load index (0.0 to 1.0).
     */
    public double getStudentCognitiveLoad(Long studentId) {
        CachedWindow cached = cache.get(studentId);
        double zScore = 0.0;
        if (cached != null) {
            zScore = cached.latestZScore;
        } else {
            Optional<OverloadDetection> latest = overloadDetectionRepository.findFirstByStudentIdOrderByEventTimeDesc(studentId);
            if (latest.isPresent()) {
                zScore = latest.get().getZScore();
            }
        }
        return Math.min(1.0, Math.max(0.0, (zScore - 1.5) / 3.0));
    }

    /**
     * Evicts cached student windows that have been inactive for more than 24 hours.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    public void evictInactiveStudents() {
        long threshold = System.currentTimeMillis() - 86400000;
        int sizeBefore = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().lastActivityTime < threshold);
        int sizeAfter = cache.size();
        if (sizeBefore != sizeAfter) {
            log.info("Evicted {} inactive students from cognitive load cache.", (sizeBefore - sizeAfter));
        }
    }

    private SlidingWindow getOrInitializeWindow(Long studentId) {
        CachedWindow cached = cache.get(studentId);
        if (cached != null) {
            cached.updateActivity();
            return cached.window;
        }

        SlidingWindow window = new SlidingWindow();
        List<LearningEvent> dbEvents = learningEventRepository.findTop20ByStudentIdOrderByTimestampDesc(studentId);
        for (int i = dbEvents.size() - 1; i >= 0; i--) {
            window.addEvent(dbEvents.get(i));
        }

        cache.put(studentId, new CachedWindow(window));
        return window;
    }

    Map<Long, CachedWindow> getCache() {
        return cache;
    }

    static class CachedWindow {
        final SlidingWindow window;
        long lastActivityTime;
        double latestZScore = 0.0;

        CachedWindow(SlidingWindow window) {
            this.window = window;
            this.lastActivityTime = System.currentTimeMillis();
        }

        void updateActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
    }
}
