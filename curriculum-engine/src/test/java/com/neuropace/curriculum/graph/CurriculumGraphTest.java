package com.neuropace.curriculum.graph;

import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import com.neuropace.curriculum.exception.CircularCurriculumException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurriculumGraphTest {

    private Subject createSubject(Long id, String name) {
        return Subject.builder()
                .id(id)
                .name(name)
                .difficultyLevel(5)
                .topicArea("Science")
                .build();
    }

    private Prerequisite createPrerequisite(Long id, Subject subject, Subject requires) {
        return Prerequisite.builder()
                .id(id)
                .subject(subject)
                .requiresSubject(requires)
                .weight(1.0)
                .build();
    }

    @Test
    void testValidDAG() {
        Subject s1 = createSubject(1L, "Math");
        Subject s2 = createSubject(2L, "Algebra");
        Subject s3 = createSubject(3L, "Geometry");
        Subject s4 = createSubject(4L, "Calculus 1");
        Subject s5 = createSubject(5L, "Calculus 2");

        List<Subject> subjects = List.of(s1, s2, s3, s4, s5);

        List<Prerequisite> prerequisites = List.of(
                createPrerequisite(1L, s2, s1), // Math -> Algebra
                createPrerequisite(2L, s3, s1), // Math -> Geometry
                createPrerequisite(3L, s4, s2), // Algebra -> Calculus 1
                createPrerequisite(4L, s5, s4)  // Calculus 1 -> Calculus 2
        );

        CurriculumGraph graph = new CurriculumGraph(subjects, prerequisites);

        assertThat(graph.isValidDAG()).isTrue();
        List<Subject> order = graph.getTopologicalOrder();
        assertThat(order).hasSize(5);

        assertThat(order.indexOf(s1)).isLessThan(order.indexOf(s2));
        assertThat(order.indexOf(s1)).isLessThan(order.indexOf(s3));
        assertThat(order.indexOf(s2)).isLessThan(order.indexOf(s4));
        assertThat(order.indexOf(s4)).isLessThan(order.indexOf(s5));

        // Test getPrerequisites and getDependents
        assertThat(graph.getPrerequisites(2L)).containsExactly(s1);
        assertThat(graph.getDependents(2L)).containsExactly(s4);
    }

    @Test
    void testCycleDetection() {
        Subject sA = createSubject(1L, "Math");
        Subject sB = createSubject(2L, "Algebra");
        Subject sC = createSubject(3L, "Physics");

        List<Subject> subjects = List.of(sA, sB, sC);

        List<Prerequisite> prerequisites = List.of(
                createPrerequisite(1L, sB, sA), // A -> B
                createPrerequisite(2L, sC, sB), // B -> C
                createPrerequisite(3L, sA, sC)  // C -> A
        );

        assertThatThrownBy(() -> new CurriculumGraph(subjects, prerequisites))
                .isInstanceOf(CircularCurriculumException.class)
                .hasMessageContaining("Circular curriculum detected")
                .hasMessageContaining("Math")
                .hasMessageContaining("Algebra")
                .hasMessageContaining("Physics");
    }

    @Test
    void testEmptyGraph() {
        CurriculumGraph graph = new CurriculumGraph(new ArrayList<>(), new ArrayList<>());
        assertThat(graph.isValidDAG()).isTrue();
        assertThat(graph.getTopologicalOrder()).isEmpty();
    }

    @Test
    void testSingleSubject() {
        Subject s1 = createSubject(1L, "Math");
        CurriculumGraph graph = new CurriculumGraph(List.of(s1), new ArrayList<>());
        assertThat(graph.isValidDAG()).isTrue();
        assertThat(graph.getTopologicalOrder()).containsExactly(s1);
    }

    @Test
    void testLinearChain() {
        Subject sA = createSubject(1L, "A");
        Subject sB = createSubject(2L, "B");
        Subject sC = createSubject(3L, "C");
        Subject sD = createSubject(4L, "D");
        Subject sE = createSubject(5L, "E");

        List<Subject> subjects = List.of(sA, sB, sC, sD, sE);
        List<Prerequisite> prerequisites = List.of(
                createPrerequisite(1L, sB, sA),
                createPrerequisite(2L, sC, sB),
                createPrerequisite(3L, sD, sC),
                createPrerequisite(4L, sE, sD)
        );

        CurriculumGraph graph = new CurriculumGraph(subjects, prerequisites);
        assertThat(graph.isValidDAG()).isTrue();
        assertThat(graph.getTopologicalOrder()).containsExactly(sA, sB, sC, sD, sE);
    }

    @Test
    void testDiamond() {
        Subject sA = createSubject(1L, "A");
        Subject sB = createSubject(2L, "B");
        Subject sC = createSubject(3L, "C");
        Subject sD = createSubject(4L, "D");

        List<Subject> subjects = List.of(sA, sB, sC, sD);
        List<Prerequisite> prerequisites = List.of(
                createPrerequisite(1L, sB, sA),
                createPrerequisite(2L, sC, sA),
                createPrerequisite(3L, sD, sB),
                createPrerequisite(4L, sD, sC)
        );

        CurriculumGraph graph = new CurriculumGraph(subjects, prerequisites);
        assertThat(graph.isValidDAG()).isTrue();
        List<Subject> order = graph.getTopologicalOrder();
        assertThat(order.indexOf(sA)).isLessThan(order.indexOf(sB));
        assertThat(order.indexOf(sA)).isLessThan(order.indexOf(sC));
        assertThat(order.indexOf(sB)).isLessThan(order.indexOf(sD));
        assertThat(order.indexOf(sC)).isLessThan(order.indexOf(sD));
    }
}
