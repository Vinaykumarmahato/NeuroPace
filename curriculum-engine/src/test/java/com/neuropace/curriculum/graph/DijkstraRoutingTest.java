package com.neuropace.curriculum.graph;

import com.neuropace.curriculum.dto.PathDTO;
import com.neuropace.curriculum.entity.Prerequisite;
import com.neuropace.curriculum.entity.Subject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Dijkstra routing on the weighted curriculum graph.
 *
 * Each test follows: graph setup → routing request → expected path.
 */
class DijkstraRoutingTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Subject subject(long id, String name, int difficulty) {
        return Subject.builder().id(id).name(name).difficultyLevel(difficulty).build();
    }

    private Prerequisite prereq(long id, Subject from, Subject to) {
        // Edge: "to" requires "from". Weight = to.difficultyLevel.
        return Prerequisite.builder().id(id).subject(to).requiresSubject(from).weight(1.0).build();
    }

    // ── Test 1: Simple linear path A → B → C ─────────────────────────────

    @Test
    void testSimpleLinearPath() {
        // Graph: A(diff=1) → B(diff=2) → C(diff=3)
        // Routing DOWN: from C(diff=3), target = A(diff=1)
        Subject a = subject(1L, "A", 1);
        Subject b = subject(2L, "B", 2);
        Subject c = subject(3L, "C", 3);

        // B requires A; C requires B
        List<Prerequisite> prereqs = Arrays.asList(
                prereq(1L, a, b),   // B requires A (A→B edge, weight = B.diff = 2)
                prereq(2L, b, c)    // C requires B (B→C edge, weight = C.diff = 3)
        );

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(
                Arrays.asList(a, b, c), prereqs);

        // Route from C(3) down to A(1)
        PathDTO path = graph.findLightestPath(3L, Set.of(1L));

        assertThat(path.path()).isNotEmpty();
        assertThat(path.path().get(0).getId()).isEqualTo(3L);          // starts at C
        assertThat(path.path().get(path.path().size() - 1).getId()).isEqualTo(1L); // ends at A
        assertThat(path.totalCost()).isGreaterThan(0.0);
        assertThat(path.reasoning()).contains("A");
    }

    // ── Test 2: Multiple paths, picks lowest cost ─────────────────────────

    @Test
    void testMultiplePathsPicksLowestCost() {
        // Graph:
        //   A(1) → B(2) → D(5)   cost A→B→D = 2+5 = 7
        //   A(1) → C(4) → D(5)   cost A→C→D = 4+5 = 9  (higher)
        // Route from D(5) to A(1) — Dijkstra picks the lower-cost path through B
        Subject a = subject(1L, "A", 1);
        Subject b = subject(2L, "B", 2);
        Subject c = subject(3L, "C", 4);
        Subject d = subject(4L, "D", 5);

        List<Prerequisite> prereqs = Arrays.asList(
                prereq(1L, a, b),  // B requires A
                prereq(2L, a, c),  // C requires A
                prereq(3L, b, d),  // D requires B
                prereq(4L, c, d)   // D requires C
        );

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(
                Arrays.asList(a, b, c, d), prereqs);

        PathDTO path = graph.findLightestPath(4L, Set.of(1L));

        assertThat(path.path()).isNotEmpty();
        // Path should go through B (cheaper intermediate node)
        boolean containsB = path.path().stream().anyMatch(s -> s.getId() == 2L);
        assertThat(containsB).isTrue();
        assertThat(path.totalCost()).isLessThanOrEqualTo(8.0); // B-path: reverse edges diff 2+1=3
    }

    // ── Test 3: Already at destination ───────────────────────────────────

    @Test
    void testAlreadyAtDestination() {
        Subject a = subject(1L, "Algebra", 2);
        Subject b = subject(2L, "Calculus", 9);
        List<Prerequisite> prereqs = Collections.singletonList(prereq(1L, a, b));

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(
                Arrays.asList(a, b), prereqs);

        // Source and target are the same
        PathDTO path = graph.findLightestPath(1L, Set.of(1L));

        assertThat(path.totalCost()).isEqualTo(0.0);
        assertThat(path.reasoning()).containsIgnoringCase("optimal");
    }

    // ── Test 4: No path exists ────────────────────────────────────────────

    @Test
    void testNoPathExists() {
        // D is isolated — no edges connect it to A, B, C
        Subject a = subject(1L, "A", 1);
        Subject b = subject(2L, "B", 2);
        Subject d = subject(4L, "D", 9);  // isolated

        List<Prerequisite> prereqs = Collections.singletonList(prereq(1L, a, b));

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(
                Arrays.asList(a, b, d), prereqs);

        // Route from D to A — D is isolated, no path
        PathDTO path = graph.findLightestPath(4L, Set.of(1L));

        assertThat(path.path()).isEmpty();
        assertThat(path.totalCost()).isEqualTo(Double.MAX_VALUE);
        assertThat(path.reasoning()).containsIgnoringCase("no path");
    }

    // ── Test 5: Diamond graph (no cycle, Dijkstra picks min cost) ────────

    @Test
    void testDiamondGraphNoCycle() {
        // Diamond: A→B, A→C, B→D, C→D
        // Difficulty: A=1, B=3, C=2, D=6
        Subject a = subject(1L, "A", 1);
        Subject b = subject(2L, "B", 3);
        Subject c = subject(3L, "C", 2);
        Subject d = subject(4L, "D", 6);

        List<Prerequisite> prereqs = Arrays.asList(
                prereq(1L, a, b),  // B requires A
                prereq(2L, a, c),  // C requires A
                prereq(3L, b, d),  // D requires B
                prereq(4L, c, d)   // D requires C
        );

        CurriculumGraphWithWeights graph = new CurriculumGraphWithWeights(
                Arrays.asList(a, b, c, d), prereqs);

        // Route from D(6) to A(1)
        PathDTO path = graph.findLightestPath(4L, Set.of(1L));

        // Must terminate (no infinite loop) and find a path
        assertThat(path.path()).isNotEmpty();
        assertThat(path.path().get(path.path().size() - 1).getId()).isEqualTo(1L);
        // Via C(2) is cheaper than via B(3)
        boolean viaCheaperPath = path.path().stream().anyMatch(s -> s.getId() == 3L);
        assertThat(viaCheaperPath).isTrue();
    }
}
