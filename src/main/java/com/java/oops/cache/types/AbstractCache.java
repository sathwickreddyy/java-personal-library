package com.java.oops.cache.types;

import java.util.Optional;

/**
 * AbstractCache interface
 * @param <K> Key
 * @param <V> Value
 * @author sathwick
 */
public interface AbstractCache<K, V> {
    /**
     * Updates the cache with key and value
     * @param key Of type K
     * @param value Of type V
     */
    void put(K key, V value);

    /**
     * Returns the value for the given key
     * @param key Of type K
     * @return Optional of type V
     */
    Optional<V> get(K key);

    /**
     * Evicts the key from the cache based on eviction policy
     * @param key Of type K
     */
    void evict(K key);
}
