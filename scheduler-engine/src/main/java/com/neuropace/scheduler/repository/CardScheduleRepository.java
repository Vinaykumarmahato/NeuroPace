package com.neuropace.scheduler.repository;

import com.neuropace.scheduler.entity.DueCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DueCard entities.
 */
@Repository
public interface CardScheduleRepository extends JpaRepository<DueCard, Long> {

    /**
     * Finds due cards for a specific student before or on a given date.
     *
     * @param studentId  the student ID.
     * @param beforeDate the upper bound date.
     * @return List of due cards.
     */
    @Query("SELECT d FROM DueCard d WHERE d.studentId = :studentId AND d.dueDate <= :beforeDate ORDER BY d.dueDate ASC")
    List<DueCard> findDueCardsForStudent(@Param("studentId") Long studentId, @Param("beforeDate") LocalDate beforeDate);

    /**
     * Finds all due cards scheduled before or on a given date.
     *
     * @param date the date boundary.
     * @return List of due cards.
     */
    @Query("SELECT d FROM DueCard d WHERE d.dueDate <= :date ORDER BY d.dueDate ASC")
    List<DueCard> findDueCardsBefore(@Param("date") LocalDate date);

    /**
     * Finds a single due card by student and subject.
     *
     * @param studentId the student ID.
     * @param subjectId the subject ID.
     * @return Optional containing the due card if found.
     */
    Optional<DueCard> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    /**
     * Finds all due cards for a student regardless of due date.
     */
    List<DueCard> findByStudentId(Long studentId);
}
