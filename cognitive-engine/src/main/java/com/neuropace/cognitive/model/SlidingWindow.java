package com.neuropace.cognitive.model;

import com.neuropace.cognitive.entity.LearningEvent;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe sliding window queue of the last N events (N=20).
 *
 * <p>Designed to be thread-safe by using a {@link ConcurrentLinkedDeque}.
 * Statistical metrics (mean, standard deviation) are computed from a local snapshot
 * of the deque to avoid concurrent modification issues without external synchronization.
 *
 * <p>Uses sample standard deviation (divides by N-1) for unbiased baseline estimation,
 * which is standard practice in anomaly detection systems.
 *
 * <p>Thread-safety guarantee: addEvent and all metric methods are safe to call
 * concurrently from multiple threads. No additional locking is required.
 */
public class SlidingWindow {

    public static final int MAX_SIZE = 20;

    private final ConcurrentLinkedDeque<Long> deque = new ConcurrentLinkedDeque<>();

    /**
     * Adds an event to the sliding window, removing the oldest if size exceeds 20.
     *
     * <p>Time Complexity: O(1) amortized.
     *
     * @param event the LearningEvent to add (must have a non-null responseTimeMs).
     */
    public void addEvent(LearningEvent event) {
        if (event == null || event.getResponseTimeMs() == null) return;
        deque.addLast(event.getResponseTimeMs());
        // Trim oldest entries — at most 1 trim per add
        while (deque.size() > MAX_SIZE) {
            deque.pollFirst();
        }
    }

    /**
     * Returns the average response time in milliseconds.
     * Returns 0.0 if there are fewer than 3 events (insufficient data).
     *
     * <p>Time Complexity: O(N) where N ≤ 20.
     *
     * @return mean response time in ms, or 0.0 if insufficient data.
     */
    public double getMean() {
        Long[] snapshot = deque.toArray(new Long[0]);
        if (snapshot.length < 3) return 0.0;
        double sum = 0.0;
        for (long v : snapshot) sum += v;
        return sum / snapshot.length;
    }

    /**
     * Returns the sample standard deviation of response times.
     * Returns 0.0 if there are fewer than 3 events or all values are identical.
     *
     * <p>Uses sample std dev (divide by N-1) for unbiased estimation.
     * Time Complexity: O(N) where N ≤ 20.
     *
     * @return sample standard deviation in ms, or 0.0 if insufficient data.
     */
    public double getStdDev() {
        Long[] snapshot = deque.toArray(new Long[0]);
        int n = snapshot.length;
        if (n < 3) return 0.0;

        double sum = 0.0;
        for (long v : snapshot) sum += v;
        double mean = sum / n;

        double varianceSum = 0.0;
        for (long v : snapshot) varianceSum += (v - mean) * (v - mean);
        return Math.sqrt(varianceSum / (n - 1));
    }

    /**
     * Returns the current number of events in the window (0–20).
     *
     * @return event count.
     */
    public int getEventCount() {
        return deque.size();
    }

    /**
     * Clears all events from the window.
     */
    public void reset() {
        deque.clear();
    }
}
