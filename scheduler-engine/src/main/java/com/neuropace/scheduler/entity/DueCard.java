package com.neuropace.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing a spaced repetition scheduling record for a student and subject.
 */
@Entity
@Table(name = "due_cards", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueCard implements Comparable<DueCard> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "interval_days", nullable = false)
    private Double intervalDays;

    @Column(name = "ease_factor", nullable = false)
    private Double easeFactor;

    @Column(name = "repetition_count", nullable = false)
    private Integer repetitionCount;

    @Override
    public int compareTo(DueCard o) {
        if (this.dueDate == null && o.dueDate == null) return 0;
        if (this.dueDate == null) return 1;
        if (o.dueDate == null) return -1;
        return this.dueDate.compareTo(o.dueDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DueCard dueCard = (DueCard) o;
        return Objects.equals(id, dueCard.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String intervalStr = (intervalDays % 1 == 0) ? String.format("%.0f", intervalDays) : String.valueOf(intervalDays);
        return String.format("DueCard{student=%d, subject=%d, due=%s, interval=%sd, EF=%.1f}",
                studentId, subjectId, dueDate, intervalStr, easeFactor);
    }
}
