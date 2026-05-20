package com.neuropace.curriculum.repository;

import com.neuropace.curriculum.entity.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Prerequisite entities.
 */
@Repository
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Long> {
}
