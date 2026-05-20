package com.neuropace.cognitive.benchmark;

import com.neuropace.cognitive.dto.OverloadDetectionDTO;
import com.neuropace.cognitive.entity.LearningEvent;
import com.neuropace.cognitive.model.SlidingWindow;
import com.neuropace.cognitive.util.AnomalyDetector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Benchmark for Cognitive Load Anomaly Detection (Z-Score).
 *
 * Simulates 1,000 students with 20 pre-populated baseline events each,
 * then processes 50,000 Z-score detection calls in isolation.
 *
 * Memory estimate: 1000 students × 20 events × ~8 bytes (Long) = ~160 KB.
 */
public class CognitiveLoadBenchmark {

    public static void main(String[] args) {
        System.out.println("=== Starting Cognitive Load Anomaly Detection Benchmark ===");

        // ── Setup: 1000 students × 20 baseline events ───────────────────────
        Map<Long, SlidingWindow> windows = new ConcurrentHashMap<>();
        for (long studentId = 1; studentId <= 1000; studentId++) {
            SlidingWindow window = new SlidingWindow();
            for (int i = 0; i < 20; i++) {
                window.addEvent(LearningEvent.builder().responseTimeMs(900L + i * 10).build());
            }
            windows.put(studentId, window);
        }

        // ── JVM warm-up (not measured) ───────────────────────────────────────
        for (int i = 0; i < 20_000; i++) {
            SlidingWindow w = windows.get((long) (i % 1000) + 1);
            AnomalyDetector.detectOverload(w, 1200.0);
        }

        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        // ── Timed phase: 50,000 pure detection calls ─────────────────────────
        long startNs = System.nanoTime();
        for (int i = 0; i < 50_000; i++) {
            long studentId = (i % 1000) + 1;
            SlidingWindow w = windows.get(studentId);
            // Vary response time to exercise both normal and anomaly paths
            double responseTime = 1000.0 + (i % 4000);
            AnomalyDetector.detectOverload(w, responseTime);
        }
        long endNs = System.nanoTime();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();

        double totalMs   = (endNs - startNs) / 1_000_000.0;
        double avgUs     = (endNs - startNs) / 50_000.0 / 1000.0;
        double memDeltaMb = Math.max(0.0, (memAfter - memBefore) / (1024.0 * 1024.0));

        System.out.printf("Anomaly detection: 50k events in %.1fms (avg %.2f\u03bcs per check)%n",
                totalMs, avgUs);
        System.out.printf("Memory usage delta: %.2f MB%n", memDeltaMb);
        System.out.println("=== Benchmark Completed ===");
    }
}
