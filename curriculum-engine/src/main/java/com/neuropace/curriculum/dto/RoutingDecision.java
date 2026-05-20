package com.neuropace.curriculum.dto;

/**
 * Immutable DTO representing an adaptive routing recommendation for a student.
 *
 * @param currentSubjectId      the subject the student is currently studying.
 * @param recommendedSubjectId  the suggested easier subject to move to.
 * @param reason                human-readable explanation of why the reroute was triggered.
 * @param difficultyChange      negative means easier (e.g. -6 means 6 levels easier).
 */
public record RoutingDecision(
        Long currentSubjectId,
        Long recommendedSubjectId,
        String reason,
        int difficultyChange
) {
}
