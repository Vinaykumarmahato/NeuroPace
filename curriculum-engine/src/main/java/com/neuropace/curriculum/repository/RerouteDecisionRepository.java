package com.neuropace.curriculum.repository;

import com.neuropace.curriculum.entity.RerouteDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for RerouteDecisionEntity.
 */
@Repository
public interface RerouteDecisionRepository extends JpaRepository<RerouteDecisionEntity, Long> {

    /**
     * Finds the N most recent reroute decisions for a student, ordered newest first.
     *
     * @param studentId the student ID.
     * @return List of reroute decisions.
     */
    List<RerouteDecisionEntity> findTop50ByStudentIdOrderByRerouteTimeDesc(Long studentId);
}
