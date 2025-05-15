package com.java.oops.cache.eviction;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * FIFO (First-In-First-Out) Eviction Policy implementation.
 *
 * <p>This policy evicts keys in the exact order they were added,
 * regardless of their access frequency or recency.</p>
 *
 * @param <K> Key type.
 * @author sathwick
 */
@Slf4j
public class FIFOEvictionPolicy<K> implements EvictionPolicy<K> {

    /**
     * Queue to maintain insertion order of keys.
     */
    private final Queue<K> queue;

    /**
     * Constructs a FIFO Eviction Policy instance.
     */
    public FIFOEvictionPolicy() {
        this.queue = new LinkedList<>();
    }

    /**
     * Records the access of the key.
     *
     * <p>In FIFO policy, repeated access does not affect eviction priority.
     * The key is only recorded if it's new to the cache.</p>
     *
     * @param key Key accessed.
     */
    @Override
    public void recordAccess(K key) {
        if (!queue.contains(key)) {
            queue.offer(key);
            log.trace("Key recorded: {}. Current queue state: {}", key, queue);
        } else {
            log.trace("Key {} already present. No action taken.", key);
        }
    }

    /**
     * Evicts the oldest inserted key from the cache.
     *
     * @return Evicted key or null if no keys are available.
     */
    @Override
    public K evict() {
        K evictedKey = queue.poll();
        if (evictedKey == null) {
            log.warn("Eviction requested but cache is empty");
            return null;
        }
        log.trace("Evicted oldest inserted key: {}", evictedKey);
        return evictedKey;
    }

    /**
     * Evict the specific key
     *
     * @param key of type K
     */
    @Override
    public void evict(K key) {
        if(key == null) {
            return;
        }
        queue.remove(key);
    }
}
