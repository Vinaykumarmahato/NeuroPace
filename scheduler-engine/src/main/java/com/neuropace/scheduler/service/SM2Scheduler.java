package com.neuropace.scheduler.service;

import com.neuropace.scheduler.dto.NextReviewDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Spaced Repetition Scheduler implementing the SuperMemo-2 (SM-2) algorithm.
 *
 * Reference: Wozniak, P. A. (1987). "Optimization of learning."
 */
@Component
public class SM2Scheduler {

    public static final double MIN_EASE_FACTOR = 1.3;

    /**
     * Calculates the next review date and intervals based on the SM-2 algorithm.
     *
     * Time Complexity: O(1)
     *
     * Example interval calculation:
     * - First review: interval = 1.0 day
     * - Second review: interval = 2.0 days
     * - Subsequent reviews: interval = previousInterval * newEaseFactor
     *
     * @param currentEaseFactor the current ease factor of the card (minimum 1.3).
     * @param previousInterval  the previous interval in days (0 if new card).
     * @param performanceScore  the performance score from 0 to 5.
     * @return NextReviewDTO containing the new interval, new ease factor, and due date.
     */
    public NextReviewDTO calculateNextReview(double currentEaseFactor, double previousInterval, int performanceScore) {
        double newEaseFactor = Math.max(MIN_EASE_FACTOR, currentEaseFactor - (5.0 - performanceScore));

        double newInterval;
        if (performanceScore < 3) {
            newInterval = 1.0;
        } else {
            if (previousInterval <= 0) {
                newInterval = 1.0;
            } else if (previousInterval == 1.0) {
                newInterval = 2.0;
            } else {
                newInterval = previousInterval * newEaseFactor;
            }
        }

        LocalDate dueDate = LocalDate.now().plusDays(Math.round(newInterval));
        return new NextReviewDTO(newInterval, newEaseFactor, dueDate);
    }
}
