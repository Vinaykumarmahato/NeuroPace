package com.neuropace.cognitive.repository;

import com.neuropace.cognitive.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
}
