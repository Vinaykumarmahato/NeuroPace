package com.neuropace.cognitive.model;

import com.neuropace.cognitive.entity.LearningEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for SlidingWindow data structure.
 */
class SlidingWindowTest {

    @Test
    void testMaintainSize20() {
        // Arrange
        SlidingWindow window = new SlidingWindow();

        // Act: Add 25 events
        for (int i = 1; i <= 25; i++) {
            window.addEvent(LearningEvent.builder().responseTimeMs((long) i).build());
        }

        // Assert: Size is maintained at 20, oldest 5 are removed
        assertThat(window.getEventCount()).isEqualTo(20);
        // The mean of elements [6..25] is (6 + 25) / 2 = 15.5
        assertThat(window.getMean()).isEqualTo(15.5);
    }

    @Test
    void testMeanCalculation() {
        // Arrange
        SlidingWindow window = new SlidingWindow();
        window.addEvent(LearningEvent.builder().responseTimeMs(10L).build());
        window.addEvent(LearningEvent.builder().responseTimeMs(20L).build());
        window.addEvent(LearningEvent.builder().responseTimeMs(30L).build());

        // Act & Assert
        assertThat(window.getMean()).isEqualTo(20.0);
    }

    @Test
    void testStdDevCalculation() {
        // Arrange
        SlidingWindow window = new SlidingWindow();
        window.addEvent(LearningEvent.builder().responseTimeMs(10L).build());
        window.addEvent(LearningEvent.builder().responseTimeMs(20L).build());
        window.addEvent(LearningEvent.builder().responseTimeMs(30L).build());

        // Act & Assert
        // Sample standard deviation (divided by n-1 = 2) for [10, 20, 30] is 10.0
        assertThat(window.getStdDev()).isCloseTo(10.0, within(0.01));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // Arrange
        SlidingWindow window = new SlidingWindow();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Act: 10 concurrent threads submit 10 events each
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    window.addEvent(LearningEvent.builder().responseTimeMs((long) (threadId * 100 + j)).build());
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assert
        assertThat(finished).isTrue();
        assertThat(window.getEventCount()).isEqualTo(20);
        // Calculate statistics to ensure no ConcurrentModificationException is thrown
        assertThat(window.getMean()).isGreaterThan(0.0);
        assertThat(window.getStdDev()).isGreaterThanOrEqualTo(0.0);
    }
}
