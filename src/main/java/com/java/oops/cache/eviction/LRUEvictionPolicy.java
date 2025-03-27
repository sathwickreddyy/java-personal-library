package com.java.oops.cache.eviction;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;


/**
 * Implementation of Least Recently Used (LRU) eviction policy.
 *
 * <p>
 * This class uses a {@link LinkedHashMap} to maintain the orderAccessMap in access order, 
 * ensuring that the least recently used (LRU) key is always at the front of the map.
 * The oldest entry is automatically removed when the capacity is exceeded.
 * </p>
 *
 * Why LinkedHashMap?
 * <ul>
 *   <li>Maintains key-value mappings, unlike LinkedHashSet which only stores keys.</li>
 *   <li>Supports <b>access-ordering</b> (enabled via constructor), ensuring quick LRU eviction.</li>
 *   <li>Allows O(1) access, insertion, and removal operations.</li>
 *   <li>Overrides {@code removeEldestEntry()} to implement automatic eviction.</li>
 * </ul>
 *
 * @param <K> Key type
 */
@Slf4j
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

    /**
     * Internal orderAccessMap using LinkedHashMap to store keys in LRU order.
     */
    private final LinkedHashMap<K, Boolean> orderAccessMap;


    /**
     * Initializes an LRU eviction policy with the given capacity.
     *
     * @param capacity Maximum number of keys before eviction occurs.
     */
    public LRUEvictionPolicy(int capacity) {
        this.orderAccessMap = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    /**
     * Records access to the specified key, marking it as recently used.
     *
     * @param key The accessed key.
     */
    @Override
    public void recordAccess(K key) {
        log.trace("Access recorded for key: {}", key);
        orderAccessMap.put(key, Boolean.TRUE);
        log.trace("orderAccessMap state after access: {}", orderAccessMap.keySet());
    }

    /**
     * Evicts the least recently used key from the orderAccessMap.
     *
     * @return The evicted key, or null if the orderAccessMap is empty.
     */
    @Override
    public K evict() {
        if (orderAccessMap.isEmpty()) {
            log.warn("Eviction requested but orderAccessMap is empty");
            return null;
        }
        K evictedKey = orderAccessMap.keySet().iterator().next();
        orderAccessMap.remove(evictedKey);
        log.trace("Manually evicted LRU key: {}", evictedKey);
        return evictedKey;
    }
}
