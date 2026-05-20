package com.neuropace.curriculum.service;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.repository.CurriculumRepository;
import com.neuropace.curriculum.repository.PrerequisiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CurriculumGraphServiceTest {

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private PrerequisiteRepository prerequisiteRepository;

    @Autowired
    private CurriculumGraphService curriculumGraphService;

    private Subject introMath;
    private Subject calculus;
    private final List<Subject> savedSubjects = new ArrayList<>();
    private final List<Prerequisite> savedPrerequisites = new ArrayList<>();

    @BeforeEach
    void setUp() {
        introMath = Subject.builder()
                .name("Integration Test Intro to Math")
                .difficultyLevel(3)
                .topicArea("Math")
                .build();
        calculus = Subject.builder()
                .name("Integration Test Calculus")
                .difficultyLevel(6)
                .topicArea("Math")
                .build();
        
        savedSubjects.addAll(curriculumRepository.saveAll(List.of(introMath, calculus)));

        Prerequisite p1 = Prerequisite.builder()
                .subject(calculus)
                .requiresSubject(introMath)
                .weight(1.0)
                .build();
        savedPrerequisites.add(prerequisiteRepository.save(p1));

        // Reload the service's graph with the newly added test fixtures
        curriculumGraphService.init();
    }

    @AfterEach
    void tearDown() {
        // Delete only the test-specific entries to avoid FK violations on other tables
        prerequisiteRepository.deleteAll(savedPrerequisites);
        curriculumRepository.deleteAll(savedSubjects);
    }

    @Test
    void testServiceTopologicalOrder() {
        assertThat(curriculumGraphService.isValidDAG()).isTrue();
        List<Subject> order = curriculumGraphService.getTopologicalOrder();
        assertThat(order).isNotEmpty();
        
        // Assert that the relative ordering of our test subjects is correct
        int introIndex = -1;
        int calcIndex = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).getId().equals(introMath.getId())) {
                introIndex = i;
            } else if (order.get(i).getId().equals(calculus.getId())) {
                calcIndex = i;
            }
        }
        
        assertThat(introIndex).isNotEqualTo(-1);
        assertThat(calcIndex).isNotEqualTo(-1);
        assertThat(introIndex).isLessThan(calcIndex);
    }

    @Test
    void testServicePrerequisitesAndDependents() {
        List<Subject> prereqs = curriculumGraphService.getPrerequisites(calculus.getId());
        assertThat(prereqs)
                .extracting(Subject::getId)
                .contains(introMath.getId());

        List<Subject> dependents = curriculumGraphService.getDependents(introMath.getId());
        assertThat(dependents)
                .extracting(Subject::getId)
                .contains(calculus.getId());
    }
}
