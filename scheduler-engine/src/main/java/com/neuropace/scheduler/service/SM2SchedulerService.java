package com.neuropace.scheduler.service;

import com.neuropace.scheduler.dto.NextReviewDTO;
import com.neuropace.scheduler.entity.DueCard;
import com.neuropace.scheduler.repository.CardScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing spaced repetition scheduling for students.
 */
@Service
public class SM2SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SM2SchedulerService.class);

    public static final double DEFAULT_EASE_FACTOR = 2.5;
    public static final double DEFAULT_INTERVAL_DAYS = 0.0;
    public static final int DEFAULT_REPETITION_COUNT = 0;

    private final CardScheduleRepository cardScheduleRepository;
    private final SM2Scheduler sm2Scheduler;
    private final StringRedisTemplate stringRedisTemplate;

    public SM2SchedulerService(CardScheduleRepository cardScheduleRepository,
                               SM2Scheduler sm2Scheduler,
                               StringRedisTemplate stringRedisTemplate) {
        this.cardScheduleRepository = cardScheduleRepository;
        this.sm2Scheduler = sm2Scheduler;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Records a student's review performance and updates the card's next review date.
     *
     * Time Complexity: O(1) database read and write.
     *
     * @param studentId        the student ID.
     * @param subjectId        the subject ID.
     * @param performanceScore the review performance score (0 to 5).
     * @return The updated DueCard.
     */
    @Transactional
    public DueCard recordStudentPerformance(Long studentId, Long subjectId, int performanceScore) {
        Optional<DueCard> existingCardOpt = cardScheduleRepository.findByStudentIdAndSubjectId(studentId, subjectId);

        DueCard card;
        double currentEF;
        double currentInterval;
        int currentRepetitionCount;

        if (existingCardOpt.isPresent()) {
            card = existingCardOpt.get();
            currentEF = card.getEaseFactor();
            currentInterval = card.getIntervalDays();
            currentRepetitionCount = card.getRepetitionCount();
        } else {
            // First time card creation defaults
            currentEF = DEFAULT_EASE_FACTOR;
            currentInterval = DEFAULT_INTERVAL_DAYS;
            currentRepetitionCount = DEFAULT_REPETITION_COUNT;

            card = DueCard.builder()
                    .studentId(studentId)
                    .subjectId(subjectId)
                    .build();
        }

        // Run core SM-2 algorithm
        NextReviewDTO result = sm2Scheduler.calculateNextReview(currentEF, currentInterval, performanceScore);

        // Update card parameters
        card.setEaseFactor(result.newEaseFactor());
        card.setIntervalDays(result.newInterval());
        card.setDueDate(result.dueDate());
        card.setRepetitionCount(performanceScore < 3 ? 0 : currentRepetitionCount + 1);

        DueCard savedCard = cardScheduleRepository.save(card);

        log.info("Performance update: studentId={}, subjectId={}: before=[interval={}, EF={}, repCount={}], after=[interval={}, EF={}, due={}]",
                studentId, subjectId, currentInterval, currentEF, currentRepetitionCount,
                savedCard.getIntervalDays(), savedCard.getEaseFactor(), savedCard.getDueDate());

        // Publish event to Redis pub/sub with resiliency
        try {
            String eventMessage = String.format("{\"studentId\":%d,\"subjectId\":%d,\"newInterval\":%.2f,\"dueDate\":\"%s\"}",
                    studentId, subjectId, result.newInterval(), result.dueDate());
            stringRedisTemplate.convertAndSend("CardScheduled", eventMessage);
        } catch (Exception e) {
            log.error("Failed to publish CardScheduled event to Redis: {}", e.getMessage());
        }

        return savedCard;
    }

    /**
     * Retrieves up to 10 due cards for a specific student, sorted by due date.
     *
     * Time Complexity: O(N log N) where N is the number of student cards.
     *
     * @param studentId the student ID.
     * @return Sorted list of due cards limited to 10.
     */
    public List<DueCard> getDueCardsForStudent(Long studentId) {
        List<DueCard> cards = cardScheduleRepository.findByStudentId(studentId);
        return cards.stream()
                .sorted()
                .limit(10)
                .toList();
    }
}
