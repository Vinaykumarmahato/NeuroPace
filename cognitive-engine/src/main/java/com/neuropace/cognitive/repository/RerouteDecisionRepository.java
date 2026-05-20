package com.neuropace.cognitive.repository;

import com.neuropace.cognitive.entity.RerouteDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RerouteDecisionRepository extends JpaRepository<RerouteDecision, Long> {
}
