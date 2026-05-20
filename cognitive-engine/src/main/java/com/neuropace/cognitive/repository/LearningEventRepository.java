package com.neuropace.cognitive.repository;

import com.neuropace.cognitive.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for LearningEvent entities.
 */
@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {

    /**
     * Finds the 20 most recent learning events for a specific student, sorted by timestamp descending.
     *
     * @param studentId the student ID.
     * @return List of learning events.
     */
    List<LearningEvent> findTop20ByStudentIdOrderByTimestampDesc(Long studentId);
}
