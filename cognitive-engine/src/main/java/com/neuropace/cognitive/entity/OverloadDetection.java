package com.neuropace.cognitive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a logged cognitive overload detection analysis for a student.
 */
@Entity
@Table(name = "overload_detections", indexes = {
    @Index(name = "idx_student_event_time", columnList = "student_id, event_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverloadDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "z_score", nullable = false)
    private Double zScore;

    @Column(name = "response_time_ms", nullable = false)
    private Double responseTimeMs;

    @Column(name = "baseline_mean_ms", nullable = false)
    private Double baselineMeanMs;

    @Column(name = "baseline_stddev", nullable = false)
    private Double baselineStddev;

    @Column(name = "is_overloaded", nullable = false)
    private Boolean isOverloaded;
}
