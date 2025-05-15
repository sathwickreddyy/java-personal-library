package com.java.oops.cache.eviction;

import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;

/**
 * Implementation of the Least Recently Used (LRU) eviction policy.
 *
 * <p>
 * This class uses a {@link LinkedHashMap} configured for access-order,
 * ensuring that the least recently used key is always at the front of the map.
 * Eviction is performed explicitly via the {@link #evict()} or {@link #evict(Object)} methods.
 * </p>
 *
 * <b>Why LinkedHashMap?</b>
 * <ul>
 *   <li>Maintains key-value mappings and access order for O(1) LRU operations.</li>
 *   <li>Allows quick identification and removal of the least recently used entry.</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this policy
 * @author sathwick
 */
@Slf4j
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

    /**
     * Internal map to store keys in access order.
     * The value is a dummy placeholder; only the keys and their order matter.
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
     * Records access to the specified key, marking it as most recently used.
     * Adds the key if not already present.
     *
     * @param key The accessed key.
     */
    @Override
    public void recordAccess(K key) {
        orderAccessMap.put(key, Boolean.TRUE);
        log.debug("Recorded access for key '{}'. Current LRU order: {}", key, orderAccessMap.keySet());
    }

    /**
     * Evicts the least recently used key from the map.
     *
     * @return The evicted key, or null if the map is empty.
     */
    @Override
    public K evict() {
        if (orderAccessMap.isEmpty()) {
            log.info("Eviction requested but LRU map is empty.");
            return null;
        }
        K evictedKey = orderAccessMap.keySet().iterator().next();
        orderAccessMap.remove(evictedKey);
        log.info("Evicted least recently used key '{}'.", evictedKey);
        return evictedKey;
    }

    /**
     * Evicts a specific key from the map, if present.
     *
     * @param key The key to evict.
     */
    @Override
    public void evict(K key) {
        if (orderAccessMap.remove(key) != null) {
            log.info("Evicted specific key '{}' from LRU map.", key);
        } else {
            log.debug("Eviction requested for non-existent key '{}'.", key);
        }
    }
}
