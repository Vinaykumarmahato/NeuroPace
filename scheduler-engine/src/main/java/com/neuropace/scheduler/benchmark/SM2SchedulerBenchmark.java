package com.neuropace.scheduler.benchmark;

import com.neuropace.scheduler.dto.NextReviewDTO;
import com.neuropace.scheduler.entity.DueCard;
import com.neuropace.scheduler.service.SM2Scheduler;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Benchmark simulating spaced repetition algorithm performance.
 *
 * Runs a simulation with 50,000 due cards and measures the execution latency of
 * 5,000 consecutive updates.
 */
public class SM2SchedulerBenchmark {

    /**
     * Executes the spaced repetition benchmark.
     *
     * Time Complexity: O(U) where U is the number of updates.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        System.out.println("=== Starting SM-2 Scheduler Benchmark ===");

        SM2Scheduler scheduler = new SM2Scheduler();
        Map<String, DueCard> inMemoryDb = new ConcurrentHashMap<>();

        // 1. Setup Phase: Simulate 1000 students x 50 subjects = 50,000 cards
        long setupStart = System.currentTimeMillis();
        for (long studentId = 1; studentId <= 1000; studentId++) {
            for (long subjectId = 1; subjectId <= 50; subjectId++) {
                String key = studentId + "_" + subjectId;
                DueCard card = DueCard.builder()
                        .studentId(studentId)
                        .subjectId(subjectId)
                        .easeFactor(2.5)
                        .intervalDays(0.0)
                        .repetitionCount(0)
                        .dueDate(LocalDate.now())
                        .build();
                inMemoryDb.put(key, card);
            }
        }
        long setupEnd = System.currentTimeMillis();
        System.out.printf("Setup complete: Created %d cards in %d ms%n", inMemoryDb.size(), (setupEnd - setupStart));

        // Warm-up JVM JIT compiler
        for (int i = 0; i < 5000; i++) {
            scheduler.calculateNextReview(2.5, 5.0, 4);
        }

        // Garbage collection hint before measurement to reduce noise
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 2. Execution Phase: Process 5000 performance updates
        long runStart = System.nanoTime();
        for (int i = 1; i <= 5000; i++) {
            long studentId = (i % 1000) + 1;
            long subjectId = (i % 50) + 1;
            int score = (i % 6); // score between 0 and 5

            String key = studentId + "_" + subjectId;
            DueCard card = inMemoryDb.get(key);

            double currentEF = card.getEaseFactor();
            double currentInterval = card.getIntervalDays();
            int currentRep = card.getRepetitionCount();

            NextReviewDTO result = scheduler.calculateNextReview(currentEF, currentInterval, score);

            card.setEaseFactor(result.newEaseFactor());
            card.setIntervalDays(result.newInterval());
            card.setDueDate(result.dueDate());
            card.setRepetitionCount(score < 3 ? 0 : currentRep + 1);
        }
        long runEnd = System.nanoTime();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        double totalMs = (runEnd - runStart) / 1_000_000.0;
        double avgUs = ((runEnd - runStart) / 5000.0) / 1000.0;
        double memoryDeltaMb = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);

        System.out.printf("SM-2 performance: 5000 updates in %.1fms (avg %.1fµs per update)%n", totalMs, avgUs);
        System.out.printf("Memory usage delta: %.2f MB%n", Math.max(0.0, memoryDeltaMb));
        System.out.println("=== Benchmark Completed ===");
    }
}
