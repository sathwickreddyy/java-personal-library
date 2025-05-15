package com.java.oops.cache.eviction;


/**
 * Eviction Policy to be implemented
 * @param <K> Key of type K
 */
public interface EvictionPolicy<K> {
    /**
     * Records the access of the key
     * @param key of type K
     */
    void recordAccess(K key);

    /**
     * Evicts the key
     * @return Key of type K
     */
    K evict();

    /**
     * Evict the specific key
     *
     * @param key of type K
     */
    void evict(K key);
}
