package com.neuropace.cognitive.util;

import com.neuropace.cognitive.dto.OverloadDetectionDTO;
import com.neuropace.cognitive.entity.LearningEvent;
import com.neuropace.cognitive.model.SlidingWindow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for AnomalyDetector.
 */
class AnomalyDetectorTest {

    private SlidingWindow createPopulatedWindow(double... responseTimes) {
        SlidingWindow window = new SlidingWindow();
        for (double rt : responseTimes) {
            window.addEvent(LearningEvent.builder().responseTimeMs((long) rt).build());
        }
        return window;
    }

    @Test
    void testNormalResponse() {
        // Arrange: Window [1000, 1100, 1050, 1200, 950] ms
        // mean=1060, sample stdDev (÷N-1)=√(37000/4)≈96.18
        SlidingWindow window = createPopulatedWindow(1000, 1100, 1050, 1200, 950);
        double newEventResponseTime = 1150;

        // Act
        OverloadDetectionDTO result = AnomalyDetector.detectOverload(window, newEventResponseTime);

        // Assert: z = (1150 - 1060) / 96.18 ≈ 0.936
        assertThat(result.isOverloaded()).isFalse();
        assertThat(result.zScore()).isCloseTo(0.936, within(0.01));
        assertThat(result.reason()).contains("above student baseline");
    }

    @Test
    void testAnomalyDetected() {
        // Arrange: Window [1000, 1100, 1050, 1200, 950] ms — same baseline
        // z = (3000 - 1060) / 96.18 ≈ 20.17
        SlidingWindow window = createPopulatedWindow(1000, 1100, 1050, 1200, 950);
        double newEventResponseTime = 3000;

        // Act
        OverloadDetectionDTO result = AnomalyDetector.detectOverload(window, newEventResponseTime);

        // Assert: extreme overload (z >> 2.0)
        assertThat(result.isOverloaded()).isTrue();
        assertThat(result.zScore()).isCloseTo(20.17, within(0.05));
    }

    @Test
    void testInsufficientData() {
        // Arrange: Window [1000] (only 1 event)
        SlidingWindow window = createPopulatedWindow(1000);
        double newEventResponseTime = 5000;

        // Act
        OverloadDetectionDTO result = AnomalyDetector.detectOverload(window, newEventResponseTime);

        // Assert
        assertThat(result.isOverloaded()).isFalse();
        assertThat(result.reason().toLowerCase()).contains("insufficient history");
    }

    @Test
    void testAllEventsIdentical() {
        // Arrange: Window [2000, 2000, 2000, 2000]
        SlidingWindow window = createPopulatedWindow(2000, 2000, 2000, 2000);
        double newEventResponseTime = 2000;

        // Act
        OverloadDetectionDTO result = AnomalyDetector.detectOverload(window, newEventResponseTime);

        // Assert
        assertThat(result.zScore()).isEqualTo(0.0);
        assertThat(result.isOverloaded()).isFalse();
    }

    @Test
    void testEdgeOfThreshold() {
        // Arrange: [900, 1000, 1100] (mean = 1000, stdDev = 100)
        // newEvent = 1200 -> z = 2.0 exactly
        SlidingWindow window = createPopulatedWindow(900, 1000, 1100);
        double newEventResponseTime = 1200;

        // Act
        OverloadDetectionDTO result = AnomalyDetector.detectOverload(window, newEventResponseTime);

        // Assert
        assertThat(result.zScore()).isEqualTo(2.0);
        assertThat(result.isOverloaded()).isTrue();
    }
}
