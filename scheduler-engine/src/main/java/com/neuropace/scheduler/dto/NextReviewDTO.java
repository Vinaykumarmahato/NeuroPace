package com.neuropace.scheduler.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object representing the calculated spaced repetition review metrics.
 */
public record NextReviewDTO(double newInterval, double newEaseFactor, LocalDate dueDate) {
}
