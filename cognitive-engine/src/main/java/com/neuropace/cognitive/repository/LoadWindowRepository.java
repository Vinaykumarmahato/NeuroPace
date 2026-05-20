package com.neuropace.cognitive.repository;

import com.neuropace.cognitive.entity.LoadWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadWindowRepository extends JpaRepository<LoadWindow, Long> {
}
