package com.neuropace.curriculum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity recording an adaptive rerouting decision made for a student.
 */
@Entity
@Table(name = "reroute_decisions_v2", indexes = {
    @Index(name = "idx_reroute_student_time", columnList = "student_id, reroute_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RerouteDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_subject_id", nullable = false)
    private Long fromSubjectId;

    @Column(name = "to_subject_id", nullable = false)
    private Long toSubjectId;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "reroute_time", nullable = false)
    private Instant rerouteTime;

    @Column(name = "is_successful")
    private Boolean isSuccessful;
}
