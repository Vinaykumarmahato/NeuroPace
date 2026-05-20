package com.neuropace.cognitive.util;

import com.neuropace.cognitive.dto.OverloadDetectionDTO;
import com.neuropace.cognitive.model.SlidingWindow;

/**
 * Utility class for calculating Z-scores and detecting cognitive overload anomalies.
 *
 * A threshold of 2.0 covers approximately 97.7% of a normal distribution, meaning any
 * response time beyond this is statistically an outlier (anomaly).
 */
public final class AnomalyDetector {

    public static final double OVERLOAD_THRESHOLD = 2.0;

    private AnomalyDetector() {
        // Prevent instantiation
    }

    /**
     * Evaluates a new response time against the historical sliding window using Z-score.
     *
     * Time Complexity: O(1).
     *
     * @param slidingWindow         the baseline historical window.
     * @param newEventResponseTime the response time of the new learning event.
     * @return OverloadDetectionDTO indicating z-score and overload status.
     */
    public static OverloadDetectionDTO detectOverload(SlidingWindow slidingWindow, double newEventResponseTime) {
        int count = slidingWindow.getEventCount();
        if (count < 3) {
            return new OverloadDetectionDTO(0.0, false, "Insufficient history for overload detection.");
        }

        double mean = slidingWindow.getMean();
        double stdDev = slidingWindow.getStdDev();

        if (stdDev == 0.0) {
            return new OverloadDetectionDTO(0.0, false, "Response time matches baseline (stdDev=0).");
        }

        double zScore = (newEventResponseTime - mean) / stdDev;
        boolean isOverloaded = zScore >= OVERLOAD_THRESHOLD;

        String reason = String.format("Response time %.0fms is %.1fσ above student baseline %.0fms",
                newEventResponseTime, zScore, mean);

        return new OverloadDetectionDTO(zScore, isOverloaded, reason);
    }
}
