package com.neuropace.curriculum.repository;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumRepository extends JpaRepository<Subject, Long> {

    /**
     * Finds all subjects in the system.
     * @return List of all subjects.
     */
    @Query("SELECT s FROM Subject s")
    List<Subject> findAllSubjects();

    /**
     * Finds all prerequisites in the system.
     * @return List of all prerequisites with fetched relationships.
     */
    @Query("SELECT p FROM Prerequisite p JOIN FETCH p.subject JOIN FETCH p.requiresSubject")
    List<Prerequisite> findAllPrerequisites();

    /**
     * Finds all prerequisites required by a specific subject.
     * @param subjectId the ID of the subject.
     * @return List of prerequisites required by the given subject.
     */
    @Query("SELECT p FROM Prerequisite p JOIN FETCH p.subject JOIN FETCH p.requiresSubject WHERE p.subject.id = :subjectId")
    List<Prerequisite> findPrerequisitesFor(@Param("subjectId") Long subjectId);
}
