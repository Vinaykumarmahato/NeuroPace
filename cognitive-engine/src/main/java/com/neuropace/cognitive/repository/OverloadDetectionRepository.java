package com.neuropace.cognitive.repository;

import com.neuropace.cognitive.entity.OverloadDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for OverloadDetection entities.
 */
@Repository
public interface OverloadDetectionRepository extends JpaRepository<OverloadDetection, Long> {

    /**
     * Finds the most recent overload detection record for a specific student.
     *
     * @param studentId the student ID.
     * @return Optional containing the latest OverloadDetection if found.
     */
    Optional<OverloadDetection> findFirstByStudentIdOrderByEventTimeDesc(Long studentId);
}
