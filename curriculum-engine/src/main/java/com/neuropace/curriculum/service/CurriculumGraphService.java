package com.neuropace.curriculum.service;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.graph.CurriculumGraph;
import com.neuropace.curriculum.graph.CurriculumGraphWithWeights;
import com.neuropace.curriculum.repository.CurriculumRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring service managing the application-wide CurriculumGraph.
 * Loads the dependency graph at startup and validates it is acyclic.
 */
@Service
@RequiredArgsConstructor
public class CurriculumGraphService {

    private static final Logger log = LoggerFactory.getLogger(CurriculumGraphService.class);

    private final CurriculumRepository curriculumRepository;
    private CurriculumGraph curriculumGraph;
    private CurriculumGraphWithWeights weightedGraph;

    /**
     * Initializes both the topological and weighted curriculum graphs from the database.
     * Throws CircularCurriculumException if a cycle is detected.
     */
    @PostConstruct
    public void init() {
        log.info("Loading curriculum graph from database...");
        List<Subject> subjects = curriculumRepository.findAllSubjects();
        List<Prerequisite> prerequisites = curriculumRepository.findAllPrerequisites();

        this.curriculumGraph = new CurriculumGraph(subjects, prerequisites);
        this.weightedGraph   = new CurriculumGraphWithWeights(subjects, prerequisites);

        log.info("Curriculum graph loaded: V={} vertices, E={} edges, order={}",
                subjects.size(),
                prerequisites.size(),
                curriculumGraph.getTopologicalOrder().size());
    }

    /**
     * Returns subjects in topological order (dependency order).
     * @return List of subjects. Time: O(V+E).
     */
    public List<Subject> getTopologicalOrder() {
        return curriculumGraph.getTopologicalOrder();
    }

    /**
     * Returns direct prerequisites for a subject.
     * @param subjectId the subject ID.
     * @return List of prerequisite subjects. Time: O(1).
     */
    public List<Subject> getPrerequisites(Long subjectId) {
        return curriculumGraph.getPrerequisites(subjectId);
    }

    /**
     * Returns subjects that directly depend on the given subject.
     * @param subjectId the subject ID.
     * @return List of dependent subjects. Time: O(1).
     */
    public List<Subject> getDependents(Long subjectId) {
        return curriculumGraph.getDependents(subjectId);
    }

    /**
     * Validates if the graph is a valid DAG.
     * @return true if valid, false otherwise.
     */
    public boolean isValidDAG() {
        return curriculumGraph.isValidDAG();
    }

    /**
     * Returns the weighted curriculum graph used for Dijkstra routing.
     * @return CurriculumGraphWithWeights instance.
     */
    public CurriculumGraphWithWeights getWeightedGraph() {
        return weightedGraph;
    }
}
