package com.java.oops.cache.eviction;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LFU (Least Frequently Used) Cache Implementation
 *
 * <pre>
 * This cache removes the least frequently used item when it reaches capacity.
 * It ensures O(1) operations for get() and put() using:
 * - HashMap for storing key-value pairs.
 * - HashMap for tracking access frequency.
 * - LinkedHashSet to maintain order within the same frequency.
 * </pre>
 *
 * <h2>LRU vs. LFU: Key Differences</h2>
 *
 * <table border="1">
 * <tr><th>Feature</th><th>LRU (Least Recently Used)</th><th>LFU (Least Frequently Used)</th></tr>
 * <tr><td><b>Eviction Rule</b></td><td>Removes the least recently accessed item</td><td>Removes the least frequently accessed item</td></tr>
 * <tr><td><b>Tracking Mechanism</b></td><td>Last access time</td><td>Access count</td></tr>
 * <tr><td><b>Data Structures</b></td><td>LinkedHashMap or Doubly Linked List + HashMap</td><td>Min-Heap + HashMap or TreeMap</td></tr>
 * <tr><td><b>Best Use Case</b></td><td>Useful when recent access is important (e.g., web browser cache)</td><td>Useful when frequently accessed items should be retained (e.g., database query cache)</td></tr>
 * <tr><td><b>Time Complexity</b></td><td>O(1) for get() and put() using LinkedHashMap</td><td>O(log n) for put() due to heap reordering</td></tr>
 * </table>
 *
 * @param <K> Type of key used in the cache
 */
@Slf4j
public class LFUEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, Integer> keyToFreq;
    private final Map<Integer, LinkedHashSet<K>> freqToKeys;
    private int minFreq;

    /**
     * Constructs an LFU Eviction Policy instance.
     */
    public LFUEvictionPolicy() {
        this.keyToFreq = new HashMap<>();
        this.freqToKeys = new HashMap<>();
        this.minFreq = 0;
    }

    /**
     * Records access of a key and updates its frequency.
     *
     * @param key Key accessed.
     */
    @Override
    public void recordAccess(K key) {
        int oldFreq = keyToFreq.getOrDefault(key, 0);
        int newFreq = oldFreq + 1;
        keyToFreq.put(key, newFreq);

        log.trace("Access recorded for key: {} | oldFreq: {} | newFreq: {}", key, oldFreq, newFreq);

        // Remove from old frequency set if present
        if (oldFreq > 0) {
            LinkedHashSet<K> oldSet = freqToKeys.get(oldFreq);
            oldSet.remove(key);
            if (oldSet.isEmpty()) {
                freqToKeys.remove(oldFreq);
                if (oldFreq == minFreq) {
                    minFreq++;
                }
            }
        } else {
            // New key accessed first time
            minFreq = 1;
        }

        // Add to new frequency set
        freqToKeys.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(key);

        log.trace("Updated freq-to-key mapping: {}", freqToKeys);
    }

    /**
     * Evicts the least frequently used key. If multiple keys have same frequency,
     * evicts the oldest one among them.
     *
     * @return Evicted key or null if no keys are available.
     */
    @Override
    public K evict() {
        if (keyToFreq.isEmpty()) {
            log.warn("Eviction requested but cache is empty");
            return null;
        }

        LinkedHashSet<K> keysWithMinFreq = freqToKeys.get(minFreq);
        K evictKey = keysWithMinFreq.iterator().next();

        // Remove evicted key from data structures
        keysWithMinFreq.remove(evictKey);
        if (keysWithMinFreq.isEmpty()) {
            freqToKeys.remove(minFreq);
            // After eviction, next insertion/access will reset minFreq appropriately
        }
        keyToFreq.remove(evictKey);

        log.trace("Evicted least frequently used key: {} with frequency {}", evictKey, minFreq);
        return evictKey;
    }
}
