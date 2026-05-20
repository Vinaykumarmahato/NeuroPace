package com.neuropace.scheduler.service;

import com.neuropace.scheduler.dto.NextReviewDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for core SM2Scheduler algorithm.
 */
class SM2SchedulerTest {

    private final SM2Scheduler scheduler = new SM2Scheduler();

    @Test
    void testFirstReview() {
        // Arrange
        double easeFactor = 2.5;
        double previousInterval = 0.0;
        int score = 4;

        // Act
        NextReviewDTO result = scheduler.calculateNextReview(easeFactor, previousInterval, score);

        // Assert
        assertThat(result.newInterval()).isEqualTo(1.0);
        assertThat(result.newEaseFactor()).isEqualTo(1.5);
        assertThat(result.dueDate()).isAfter(LocalDate.now());
    }

    @Test
    void testPerformanceDrop() {
        // Arrange
        double easeFactor = 2.0;
        double previousInterval = 10.0;
        int score = 2;

        // Act
        NextReviewDTO result = scheduler.calculateNextReview(easeFactor, previousInterval, score);

        // Assert
        assertThat(result.newInterval()).isEqualTo(1.0);
        assertThat(result.newEaseFactor()).isEqualTo(1.3);
        assertThat(result.dueDate()).isAfter(LocalDate.now());
    }

    @Test
    void testPerfectStreak() {
        // Arrange
        double initialEF = 2.5;
        int score = 5;

        // Act & Assert review sequence
        // Step 1: from new card (0) to first review (1)
        NextReviewDTO step1 = scheduler.calculateNextReview(initialEF, 0.0, score);
        assertThat(step1.newInterval()).isEqualTo(1.0);
        assertThat(step1.newEaseFactor()).isEqualTo(2.5);

        // Step 2: review with interval 1 -> 2
        NextReviewDTO step2 = scheduler.calculateNextReview(step1.newEaseFactor(), step1.newInterval(), score);
        assertThat(step2.newInterval()).isEqualTo(2.0);
        assertThat(step2.newEaseFactor()).isEqualTo(2.5);

        // Step 3: review with interval 2 -> 5
        NextReviewDTO step3 = scheduler.calculateNextReview(step2.newEaseFactor(), step2.newInterval(), score);
        assertThat(step3.newInterval()).isEqualTo(5.0);
        assertThat(step3.newEaseFactor()).isEqualTo(2.5);

        // Step 4: review with interval 5 -> 12.5
        NextReviewDTO step4 = scheduler.calculateNextReview(step3.newEaseFactor(), step3.newInterval(), score);
        assertThat(step4.newInterval()).isEqualTo(12.5);
        assertThat(step4.newEaseFactor()).isEqualTo(2.5);
    }

    @Test
    void testBoundaryValues() {
        // score = 0 -> EF floor (capped at 1.3)
        NextReviewDTO floorResult = scheduler.calculateNextReview(2.5, 5.0, 0);
        assertThat(floorResult.newEaseFactor()).isEqualTo(1.3);

        // score = 5 -> EF growth (capped at 2.5)
        NextReviewDTO growthResult = scheduler.calculateNextReview(2.5, 5.0, 5);
        assertThat(growthResult.newEaseFactor()).isEqualTo(2.5);
    }

    @Test
    void testVeryLongInterval() {
        // Arrange
        double easeFactor = 2.5;
        double previousInterval = 1000.0;
        int score = 5;

        // Act
        NextReviewDTO result = scheduler.calculateNextReview(easeFactor, previousInterval, score);

        // Assert
        assertThat(result.newInterval()).isEqualTo(2500.0);
        assertThat(result.newEaseFactor()).isEqualTo(2.5);
        assertThat(result.dueDate()).isEqualTo(LocalDate.now().plusDays(2500));
    }
}
