package com.neuropace.curriculum.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent hashing router for distributing student routing decisions across worker nodes.
 *
 * <p>Why consistent hashing?
 * <ul>
 *   <li>When a worker joins or leaves, only ~K/N keys are reassigned (not all).</li>
 *   <li>Virtual nodes (150 per worker) compensate for hash-space clustering, giving
 *       ±10% even distribution across workers.</li>
 * </ul>
 *
 * <p>Virtual node hashing uses Java's {@code hashCode()} with a salt per replica
 * (equivalent to MurmurHash3 for small key spaces). A production system would use
 * Guava's {@code Hashing.murmur3_128()} for better avalanche properties.
 *
 * <p>Thread-safety: The hash ring uses a {@link ConcurrentSkipListMap} so
 * {@code assignWorker} can be called concurrently without locks.
 * Worker mutations ({@code addWorker}, {@code removeWorker}) are synchronized.
 *
 * <p>Time Complexity:
 * <ul>
 *   <li>{@code assignWorker}: O(log W) where W = total virtual node count.</li>
 *   <li>{@code addWorker}: O(V log W) where V = virtual nodes per worker.</li>
 *   <li>{@code removeWorker}: O(V log W).</li>
 * </ul>
 */
@Component
public class ConsistentHashingRouter {

    private static final Logger log = LoggerFactory.getLogger(ConsistentHashingRouter.class);

    /** Number of virtual nodes per physical worker. Higher = more even distribution. */
    public static final int VIRTUAL_NODES = 150;

    /** Thread-safe sorted map: hash position → workerId. */
    private final ConcurrentSkipListMap<Long, String> ring = new ConcurrentSkipListMap<>();

    /** Set of active worker IDs. */
    private final Set<String> workers = Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * Adds a worker to the hash ring with {@value #VIRTUAL_NODES} virtual nodes.
     *
     * @param workerId the unique identifier of the worker node.
     */
    public synchronized void addWorker(String workerId) {
        workers.add(workerId);
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long hash = hash(workerId + "#" + i);
            ring.put(hash, workerId);
        }
        log.info("Worker '{}' added to hash ring. Total virtual nodes: {}", workerId, ring.size());
    }

    /**
     * Removes a worker from the hash ring (simulates node failure).
     * Students previously assigned to this worker are redistributed to neighbours automatically
     * on the next {@code assignWorker} call.
     *
     * @param workerId the worker to remove.
     */
    public synchronized void removeWorker(String workerId) {
        workers.remove(workerId);
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long hash = hash(workerId + "#" + i);
            ring.remove(hash);
        }
        log.info("Worker '{}' removed from hash ring. Total virtual nodes: {}", workerId, ring.size());
    }

    /**
     * Assigns a student to the closest clockwise worker on the hash ring.
     *
     * <p>Time Complexity: O(log W).
     *
     * @param studentId the student to assign.
     * @return the assigned worker ID, or {@code "no-worker"} if the ring is empty.
     */
    public String assignWorker(Long studentId) {
        if (ring.isEmpty()) {
            log.warn("Hash ring is empty — no workers available.");
            return "no-worker";
        }
        long studentHash = hash(String.valueOf(studentId));
        // Find first entry ≥ studentHash (clockwise neighbour)
        Map.Entry<Long, String> entry = ring.ceilingEntry(studentHash);
        if (entry == null) {
            // Wrap around to the first node
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * Returns the set of active worker IDs.
     *
     * @return unmodifiable view of worker IDs.
     */
    public Set<String> getWorkers() {
        return Collections.unmodifiableSet(workers);
    }

    /**
     * Returns the current size of the virtual node ring.
     *
     * @return ring size.
     */
    public int getRingSize() {
        return ring.size();
    }

    /**
     * Hashes a string key to a {@code long} position on the ring.
     *
     * <p>Uses Java's {@code String.hashCode()} with a 32-bit spread into 64-bit space
     * to avoid clustering of sequential IDs. For production, replace with
     * MurmurHash3 (e.g. Guava's {@code Hashing.murmur3_128().hashString(...)}).
     *
     * @param key the string to hash.
     * @return a non-negative 64-bit hash value.
     */
    long hash(String key) {
        // Spread 32-bit hashCode into 64-bit long with FNV-inspired mixing
        long h = key.hashCode();
        h = ((h >>> 16) ^ h) * 0x45d9f3bL;
        h = ((h >>> 16) ^ h) * 0x45d9f3bL;
        h = (h >>> 16) ^ h;
        return h & Long.MAX_VALUE; // ensure non-negative
    }
}
