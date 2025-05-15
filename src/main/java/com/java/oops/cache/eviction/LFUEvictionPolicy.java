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
 *     <caption>LRU vs. LFU: Key Differences</caption>
 * <tr><th>Feature</th><th>LRU (Least Recently Used)</th><th>LFU (Least Frequently Used)</th></tr>
 * <tr><td><b>Eviction Rule</b></td><td>Removes the least recently accessed item</td><td>Removes the least frequently accessed item</td></tr>
 * <tr><td><b>Tracking Mechanism</b></td><td>Last access time</td><td>Access count</td></tr>
 * <tr><td><b>Data Structures</b></td><td>LinkedHashMap or Doubly Linked List + HashMap</td><td>Min-Heap + HashMap or TreeMap</td></tr>
 * <tr><td><b>Best Use Case</b></td><td>Useful when recent access is important (e.g., web browser cache)</td><td>Useful when frequently accessed items should be retained (e.g., database query cache)</td></tr>
 * <tr><td><b>Time Complexity</b></td><td>O(1) for get() and put() using LinkedHashMap</td><td>O(log n) for put() due to heap reordering</td></tr>
 * </table>
 *
 * @param <K> Type of key used in the cache
 * @author sathwick
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
     * If the key is new, its frequency is set to 1.
     * If the key exists, its frequency is incremented.
     *
     * @param key Key accessed.
     */
    @Override
    public void recordAccess(K key) {
        int oldFreq = keyToFreq.getOrDefault(key, 0);
        int newFreq = oldFreq + 1;
        keyToFreq.put(key, newFreq);

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
            // New key, set minFreq to 1
            minFreq = 1;
        }

        freqToKeys.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(key);

        log.debug("Recorded access for key '{}': oldFreq={}, newFreq={}, minFreq={}", key, oldFreq, newFreq, minFreq);
    }

    /**
     * Evicts the least frequently used key (and oldest among ties).
     *
     * @return The evicted key, or null if cache is empty.
     */
    @Override
    public K evict() {
        if (keyToFreq.isEmpty()) {
            log.info("Eviction requested but cache is empty.");
            return null;
        }

        LinkedHashSet<K> keysWithMinFreq = freqToKeys.get(minFreq);
        if (keysWithMinFreq == null || keysWithMinFreq.isEmpty()) {
            log.info("No keys found with minFreq {} during eviction.", minFreq);
            return null;
        }

        K evictKey = keysWithMinFreq.iterator().next();
        keysWithMinFreq.remove(evictKey);
        keyToFreq.remove(evictKey);

        if (keysWithMinFreq.isEmpty()) {
            freqToKeys.remove(minFreq);
            // Update minFreq to next available frequency, or 0 if none
            minFreq = freqToKeys.keySet().stream().min(Integer::compareTo).orElse(0);
        }

        log.info("Evicted key '{}' with frequency {}.", evictKey, minFreq);
        return evictKey;
    }

    /**
     * Evicts a specific key from the cache, updating frequency structures.
     *
     * @param key The key to evict.
     */
    @Override
    public void evict(K key) {
        Integer freq = keyToFreq.remove(key);
        if (freq == null) {
            log.debug("Eviction requested for non-existent key '{}'.", key);
            return;
        }
        LinkedHashSet<K> set = freqToKeys.get(freq);
        if (set != null) {
            set.remove(key);
            if (set.isEmpty()) {
                freqToKeys.remove(freq);
                // Update minFreq if needed
                if (freq == minFreq) {
                    minFreq = freqToKeys.keySet().stream().min(Integer::compareTo).orElse(0);
                }
            }
        }
        log.info("Evicted specific key '{}' with frequency {}.", key, freq);
    }
}